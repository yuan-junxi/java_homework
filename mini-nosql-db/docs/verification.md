# Verification Notes

This file records the commands used during local verification on Windows with JDK 11.

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src,test -Filter *.java).FullName
java -cp out minidb.test.MiniDbTests
```

Latest local result:

```text
Mini NoSQL DB started on port 18080 as single
Mini NoSQL DB started on port 18082 as slave
Mini NoSQL DB started on port 18081 as master
ALL TESTS PASSED
```

Jar build check:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build.ps1
```

```text
Build finished: mini-nosql-db.jar
```

Manual socket client check:

```powershell
java -jar mini-nosql-db.jar config/single.properties
java -cp out minidb.client.MiniDbClient localhost 7070 PUT /kv/manual string:ok
java -cp out minidb.client.MiniDbClient localhost 7070 GET /kv/manual
java -cp out minidb.client.MiniDbClient localhost 7070 PATCH /kv/tags set:java,db
java -cp out minidb.client.MiniDbClient localhost 7070 GET /keys
java -cp out minidb.client.MiniDbClient localhost 7070 GET /info
java -cp out minidb.client.MiniDbClient localhost 7070 DELETE /kv/manual
```

```text
200 OK string:ok
200 OK string:ok
200 OK set:java,db
200 OK manual,tags
200 OK role=single;port=7070;keys=2;cache=2
200 OK deleted
```

The test harness covers:

- value parsing for string, number, set, and map
- WAL persistence and restart recovery
- server CRUD through the socket client
- static master/slave replication
- slave write rejection for normal client writes
