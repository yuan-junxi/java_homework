package minidb.server;

import minidb.client.MiniDbClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Replicator {
    private final List<ServerConfig.Node> slaves;

    public Replicator(List<ServerConfig.Node> slaves) {
        this.slaves = slaves;
    }

    public void replicate(String method, String key, String body) {
        for (ServerConfig.Node slave : slaves) {
            try {
                String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.name()).replace("+", "%20");
                MiniDbClient.send(slave.host, slave.port, method + " /kv/" + encodedKey + (body == null || body.isEmpty() ? "" : " " + body));
            } catch (Exception e) {
                System.err.println("replication to " + slave + " failed: " + e.getMessage());
            }
        }
    }
}

