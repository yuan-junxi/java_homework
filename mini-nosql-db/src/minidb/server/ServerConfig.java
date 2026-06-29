package minidb.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ServerConfig {
    public final String role;
    public final int port;
    public final Path dataDir;
    public final int cacheSize;
    public final List<Node> slaves;
    public final boolean allowShutdown;

    public ServerConfig(String role, int port, Path dataDir, int cacheSize, List<Node> slaves, boolean allowShutdown) {
        this.role = role;
        this.port = port;
        this.dataDir = dataDir;
        this.cacheSize = cacheSize;
        this.slaves = slaves;
        this.allowShutdown = allowShutdown;
    }

    public static ServerConfig defaultSingle() {
        return new ServerConfig("single", 7070, Paths.get("data", "single"), 64, Collections.emptyList(), true);
    }

    public static ServerConfig load(String file) throws IOException {
        Properties props = new Properties();
        try (InputStream in = java.nio.file.Files.newInputStream(Paths.get(file))) {
            props.load(in);
        }
        String role = props.getProperty("role", "single").trim().toLowerCase();
        int port = Integer.parseInt(props.getProperty("port", "7070").trim());
        Path dataDir = Paths.get(props.getProperty("dataDir", "data/" + role).trim());
        int cacheSize = Integer.parseInt(props.getProperty("cacheSize", "64").trim());
        boolean allowShutdown = Boolean.parseBoolean(props.getProperty("allowShutdown", "true").trim());
        return new ServerConfig(role, port, dataDir, cacheSize, parseNodes(props.getProperty("slaves", "")), allowShutdown);
    }

    public boolean isMaster() {
        return "master".equals(role);
    }

    public boolean isSlave() {
        return "slave".equals(role);
    }

    private static List<Node> parseNodes(String text) {
        List<Node> nodes = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return nodes;
        }
        for (String item : text.split(",")) {
            String[] parts = item.trim().split(":");
            if (parts.length == 2) {
                nodes.add(new Node(parts[0], Integer.parseInt(parts[1])));
            }
        }
        return nodes;
    }

    public static class Node {
        public final String host;
        public final int port;

        public Node(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }
}

