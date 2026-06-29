package minidb.server;

import minidb.core.DbValue;
import minidb.core.KvStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiniDbServer {
    private final ServerConfig config;
    private final KvStore store;
    private final Replicator replicator;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public MiniDbServer(ServerConfig config) throws IOException {
        this.config = config;
        this.store = new KvStore(config.dataDir, config.cacheSize);
        this.replicator = new Replicator(config.slaves);
    }

    public static void main(String[] args) throws Exception {
        ServerConfig config = args.length == 0 ? ServerConfig.defaultSingle() : ServerConfig.load(args[0]);
        MiniDbServer server = new MiniDbServer(config);
        server.start();
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(config.port);
        System.out.println("Mini NoSQL DB started on port " + config.port + " as " + config.role);
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                pool.submit(() -> handle(socket));
            } catch (IOException e) {
                if (running) {
                    System.err.println("accept failed: " + e.getMessage());
                }
            }
        }
    }

    public Thread startAsync() {
        Thread thread = new Thread(() -> {
            try {
                start();
            } catch (IOException e) {
                if (running) {
                    throw new RuntimeException(e);
                }
            }
        }, "mini-db-server-" + config.port);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // best effort shutdown for tests
        }
        pool.shutdownNow();
    }

    private void handle(Socket socket) {
        try (Socket s = socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8))) {
            String requestLine = reader.readLine();
            String response = dispatch(requestLine);
            writer.write(response);
            writer.newLine();
            writer.flush();
        } catch (Exception e) {
            System.err.println("request failed: " + e.getMessage());
        }
    }

    public String dispatch(String requestLine) {
        try {
            Request request = Request.parse(requestLine);
            if ("POST".equals(request.method) && "/shutdown".equals(request.path) && config.allowShutdown) {
                new Thread(this::stop).start();
                return "200 OK shutdown";
            }
            if ("GET".equals(request.method) && "/keys".equals(request.path)) {
                return "200 OK " + String.join(",", store.keys());
            }
            if ("GET".equals(request.method) && "/info".equals(request.path)) {
                return "200 OK " + store.info(config.role, config.port);
            }
            if (!request.path.startsWith("/kv/")) {
                return "400 BAD_REQUEST unsupported path";
            }
            String key = URLDecoder.decode(request.path.substring(4), StandardCharsets.UTF_8.name());
            return handleKv(request, key);
        } catch (Exception e) {
            return "500 ERROR " + safe(e.getMessage());
        }
    }

    private String handleKv(Request request, String key) throws IOException {
        boolean replication = request.method.startsWith("REPL");
        String method = replication ? request.method.substring(4) : request.method;
        if (!replication && config.isSlave() && isWrite(method)) {
            return "403 FORBIDDEN slave is read only";
        }

        switch (method) {
            case "GET":
                DbValue found = store.get(key);
                return found == null ? "404 NOT_FOUND" : "200 OK " + found.toWire();
            case "PUT": {
                DbValue value = DbValue.parse(request.body);
                DbValue next = store.put(key, value);
                if (!replication && config.isMaster()) {
                    replicator.replicate("REPLPUT", key, value.toWire());
                }
                return "200 OK " + next.toWire();
            }
            case "PATCH": {
                DbValue value = DbValue.parse(request.body);
                DbValue next = store.patch(key, value);
                if (!replication && config.isMaster()) {
                    replicator.replicate("REPLPATCH", key, value.toWire());
                }
                return "200 OK " + next.toWire();
            }
            case "DELETE": {
                boolean existed = store.delete(key);
                if (!replication && config.isMaster()) {
                    replicator.replicate("REPLDELETE", key, "");
                }
                return existed ? "200 OK deleted" : "404 NOT_FOUND";
            }
            default:
                return "400 BAD_REQUEST unsupported method";
        }
    }

    private static boolean isWrite(String method) {
        return "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private static String safe(String text) {
        return text == null ? "" : text.replace('\n', ' ').replace('\r', ' ');
    }

    private static class Request {
        final String method;
        final String path;
        final String body;

        private Request(String method, String path, String body) {
            this.method = method;
            this.path = path;
            this.body = body == null ? "" : body;
        }

        static Request parse(String line) {
            if (line == null || line.trim().isEmpty()) {
                throw new IllegalArgumentException("empty request");
            }
            String[] parts = line.trim().split(" ", 3);
            if (parts.length < 2) {
                throw new IllegalArgumentException("request should be METHOD PATH [body]");
            }
            return new Request(parts[0].toUpperCase(), parts[1], parts.length == 3 ? parts[2] : "");
        }
    }
}

