# Mini NoSQL DB

Mini NoSQL DB is a tiny Java key-value database built for the Java Advanced Programming course project. It is intentionally compact, but it still demonstrates a complete path from socket requests to in-memory indexing, WAL persistence, restart recovery, and a minimal static master/slave cluster.

## What It Covers

- Pure Java 11, no Maven, no third-party dependencies.
- Multi-threaded TCP server based on `ServerSocket`.
- REST-like line protocol: `PUT /kv/<key>`, `GET /kv/<key>`, `PATCH /kv/<key>`, `DELETE /kv/<key>`, `GET /keys`, `GET /info`.
- Value types: `string`, `number`, `set`, `map`.
- Append-only WAL persistence with restart recovery.
- In-memory index with a small LRU read cache.
- Static master/slave replication for a minimal cluster demo.
- Command line client and self-contained test harness.

## What It Does Not Cover

The course brief lists several extension items. For this individual minimum-delivery version, the project does not implement collection management, batch operations, file rotation/compression, LSM-tree storage, dynamic leader election, GUI, or a production security model.

## Project Layout

```text
mini-nosql-db/
  config/        sample server configs
  data/          runtime data, ignored by git
  docs/          verification records
  src/           production Java source
  test/          simple Java test harness
```

## Quick Start

Compile:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src,test -Filter *.java).FullName
```

Run tests:

```powershell
java -cp out minidb.test.MiniDbTests
```

Start a single node:

```powershell
java -cp out minidb.server.MiniDbServer config/single.properties
```

Use the client from another terminal:

```powershell
java -cp out minidb.client.MiniDbClient localhost 7070 PUT /kv/name string:Alice
java -cp out minidb.client.MiniDbClient localhost 7070 GET /kv/name
java -cp out minidb.client.MiniDbClient localhost 7070 PATCH /kv/tags set:java,db
java -cp out minidb.client.MiniDbClient localhost 7070 GET /keys
java -cp out minidb.client.MiniDbClient localhost 7070 GET /info
```

Build a runnable server jar after compiling:

```powershell
jar --create --file mini-nosql-db.jar --main-class minidb.server.MiniDbServer -C out .
java -jar mini-nosql-db.jar config/single.properties
```

Or use the helper script:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build.ps1
```

## Value Format

The request body uses a small readable format:

```text
string:hello
number:123.45
set:java,db,socket
map:name=Alice,score=95
```

`PATCH` replaces strings/numbers, unions sets, and merges maps.

## Cluster Demo

Open two terminals:

```powershell
java -cp out minidb.server.MiniDbServer config/slave.properties
java -cp out minidb.server.MiniDbServer config/master.properties
```

Then write to the master:

```powershell
java -cp out minidb.client.MiniDbClient localhost 7070 PUT /kv/course string:Java
java -cp out minidb.client.MiniDbClient localhost 7071 GET /kv/course
```

The slave is intentionally simple: it accepts read requests and internal replication writes. Normal client writes should go to the master.

## Protocol

Each request is one UTF-8 line:

```text
METHOD PATH [body]
```

Examples:

```text
PUT /kv/name string:Alice
GET /kv/name
PATCH /kv/tags set:java,socket
DELETE /kv/name
GET /keys
GET /info
```

Responses are also one line, for example `200 OK string:Alice` or `404 NOT_FOUND`.
