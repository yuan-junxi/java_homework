package minidb.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MiniDbClient {
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java -cp out minidb.client.MiniDbClient <host> <port> <METHOD> <PATH> [body]");
            System.out.println("Example: java -cp out minidb.client.MiniDbClient localhost 7070 PUT /kv/name string:Alice");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String method = args[2];
        String path = args[3];
        String body = join(args, 4);

        System.out.println(send(host, port, method + " " + path + (body.isEmpty() ? "" : " " + body)));
    }

    public static String send(String host, int port, String requestLine) throws IOException {
        try (Socket socket = new Socket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            writer.write(requestLine);
            writer.newLine();
            writer.flush();
            return reader.readLine();
        }
    }

    private static String join(String[] args, int start) {
        if (start >= args.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}

