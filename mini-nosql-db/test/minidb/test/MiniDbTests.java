package minidb.test;

import minidb.client.MiniDbClient;
import minidb.core.DbValue;
import minidb.core.KvStore;
import minidb.server.MiniDbServer;
import minidb.server.ServerConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

public class MiniDbTests {
    public static void main(String[] args) throws Exception {
        testValueTypes();
        testWalRecovery();
        testServerCrud();
        testStaticReplication();
        System.out.println("ALL TESTS PASSED");
    }

    private static void testValueTypes() {
        assertEquals("string:hello", DbValue.parse("string:hello").toWire(), "string value");
        assertEquals("number:42", DbValue.parse("number:42").toWire(), "number value");
        assertEquals("set:a,b", DbValue.parse("set:a,b").toWire(), "set value");
        assertEquals("map:name=Alice,score=95", DbValue.parse("map:name=Alice,score=95").toWire(), "map value");
        assertEquals("set:a,b,c", DbValue.parse("set:a,b").patch(DbValue.parse("set:b,c")).toWire(), "set patch");
    }

    private static void testWalRecovery() throws Exception {
        Path dir = Files.createTempDirectory("mini-db-wal");
        KvStore store = new KvStore(dir, 2);
        store.put("name", DbValue.parse("string:Alice"));
        store.patch("profile", DbValue.parse("map:age=20"));
        store.patch("profile", DbValue.parse("map:city=Chengdu"));
        store.delete("missing");

        KvStore recovered = new KvStore(dir, 2);
        assertEquals("string:Alice", recovered.get("name").toWire(), "wal string recovery");
        assertEquals("map:age=20,city=Chengdu", recovered.get("profile").toWire(), "wal map patch recovery");
    }

    private static void testServerCrud() throws Exception {
        Path dir = Files.createTempDirectory("mini-db-server");
        MiniDbServer server = new MiniDbServer(new ServerConfig("single", 18080, dir, 8, Collections.emptyList(), true));
        server.startAsync();
        sleep();
        try {
            assertStarts("200 OK string:Bob", MiniDbClient.send("localhost", 18080, "PUT /kv/user string:Bob"), "server put");
            assertStarts("200 OK string:Bob", MiniDbClient.send("localhost", 18080, "GET /kv/user"), "server get");
            assertStarts("200 OK deleted", MiniDbClient.send("localhost", 18080, "DELETE /kv/user"), "server delete");
            assertStarts("404 NOT_FOUND", MiniDbClient.send("localhost", 18080, "GET /kv/user"), "server missing");
        } finally {
            MiniDbClient.send("localhost", 18080, "POST /shutdown");
        }
    }

    private static void testStaticReplication() throws Exception {
        Path masterDir = Files.createTempDirectory("mini-db-master");
        Path slaveDir = Files.createTempDirectory("mini-db-slave");
        MiniDbServer slave = new MiniDbServer(new ServerConfig("slave", 18082, slaveDir, 8, Collections.emptyList(), true));
        MiniDbServer master = new MiniDbServer(new ServerConfig(
                "master",
                18081,
                masterDir,
                8,
                Arrays.asList(new ServerConfig.Node("localhost", 18082)),
                true));
        slave.startAsync();
        master.startAsync();
        sleep();
        try {
            assertStarts("200 OK string:Java", MiniDbClient.send("localhost", 18081, "PUT /kv/course string:Java"), "master put");
            Thread.sleep(250);
            assertStarts("200 OK string:Java", MiniDbClient.send("localhost", 18082, "GET /kv/course"), "slave read replicated data");
            assertStarts("403 FORBIDDEN", MiniDbClient.send("localhost", 18082, "PUT /kv/nope string:x"), "slave rejects normal write");
        } finally {
            MiniDbClient.send("localhost", 18081, "POST /shutdown");
            MiniDbClient.send("localhost", 18082, "POST /shutdown");
        }
    }

    private static void sleep() throws InterruptedException {
        Thread.sleep(250);
    }

    private static void assertEquals(String expected, String actual, String name) {
        if (!expected.equals(actual)) {
            throw new AssertionError(name + " expected [" + expected + "] but got [" + actual + "]");
        }
    }

    private static void assertStarts(String expectedPrefix, String actual, String name) {
        if (actual == null || !actual.startsWith(expectedPrefix)) {
            throw new AssertionError(name + " expected prefix [" + expectedPrefix + "] but got [" + actual + "]");
        }
    }
}

