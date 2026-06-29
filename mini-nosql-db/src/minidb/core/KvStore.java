package minidb.core;

import minidb.persist.WalLog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class KvStore {
    private final ConcurrentHashMap<String, DbValue> index = new ConcurrentHashMap<>();
    private final LruCache<String, DbValue> cache;
    private final WalLog walLog;

    public KvStore(Path dataDir, int cacheSize) throws IOException {
        this.cache = new LruCache<>(cacheSize);
        this.walLog = new WalLog(dataDir.resolve("db.wal"));
        this.walLog.replay(this::applyRecovered);
    }

    public synchronized DbValue put(String key, DbValue value) throws IOException {
        checkKey(key);
        walLog.append("PUT", key, value);
        index.put(key, value);
        cache.put(key, value);
        return value;
    }

    public synchronized DbValue patch(String key, DbValue value) throws IOException {
        checkKey(key);
        DbValue old = index.get(key);
        DbValue next = old == null ? value : old.patch(value);
        walLog.append("PATCH", key, value);
        index.put(key, next);
        cache.put(key, next);
        return next;
    }

    public synchronized boolean delete(String key) throws IOException {
        checkKey(key);
        walLog.append("DELETE", key, null);
        cache.remove(key);
        return index.remove(key) != null;
    }

    public DbValue get(String key) {
        DbValue cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        DbValue value = index.get(key);
        if (value != null) {
            cache.put(key, value);
        }
        return value;
    }

    public List<String> keys() {
        List<String> keys = new ArrayList<>(index.keySet());
        Collections.sort(keys);
        return keys;
    }

    public int size() {
        return index.size();
    }

    public int cacheSize() {
        return cache.size();
    }

    public String info(String role, int port) {
        return "role=" + role + ";port=" + port + ";keys=" + size() + ";cache=" + cacheSize();
    }

    private void applyRecovered(String op, String key, DbValue value) {
        if ("DELETE".equals(op)) {
            index.remove(key);
            cache.remove(key);
        } else if ("PATCH".equals(op)) {
            DbValue old = index.get(key);
            DbValue next = old == null ? value : old.patch(value);
            index.put(key, next);
            cache.put(key, next);
        } else if ("PUT".equals(op)) {
            index.put(key, value);
            cache.put(key, value);
        }
    }

    private static void checkKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key is empty");
        }
    }
}

