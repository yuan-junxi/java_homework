package minidb.persist;

import minidb.core.DbValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

public class WalLog {
    public interface ReplayConsumer {
        void accept(String op, String key, DbValue value);
    }

    private final Path walFile;

    public WalLog(Path walFile) throws IOException {
        this.walFile = walFile;
        if (walFile.getParent() != null) {
            Files.createDirectories(walFile.getParent());
        }
        if (!Files.exists(walFile)) {
            Files.createFile(walFile);
        }
    }

    public synchronized void append(String op, String key, DbValue value) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                walFile,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(op);
            writer.write('\t');
            writer.write(encode(key));
            writer.write('\t');
            writer.write(value == null ? "" : encode(value.toWire()));
            writer.newLine();
            writer.flush();
        }
    }

    public void replay(ReplayConsumer consumer) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(walFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\t", -1);
                if (parts.length < 3) {
                    continue;
                }
                String op = parts[0];
                String key = decode(parts[1]);
                DbValue value = parts[2].isEmpty() ? null : DbValue.parse(decode(parts[2]));
                consumer.accept(op, key, value);
            }
        }
    }

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String text) {
        return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
    }
}

