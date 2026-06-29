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

The test harness covers:

- value parsing for string, number, set, and map
- WAL persistence and restart recovery
- server CRUD through the socket client
- static master/slave replication
- slave write rejection for normal client writes

