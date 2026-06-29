# Java Homework / Mini NoSQL DB

This repository contains my Java Advanced Programming course project: **Mini NoSQL DB**, a small key-value database implemented with plain Java 11.

The project is deliberately lightweight, but it is organized like a normal open-source repository: source code, sample configs, runnable commands, tests, and verification notes are all kept in the repo.

## Highlights

- Java 11 only, no external dependencies.
- Multi-threaded C/S architecture using `ServerSocket`.
- REST-like text protocol over TCP.
- Basic NoSQL key-value operations.
- Typed values: string, number, set, and map.
- WAL persistence and restart recovery.
- In-memory index plus LRU cache.
- Static master/slave replication demo.
- Simple command line client and test harness.

## Repository Structure

```text
.
├── README.md
└── mini-nosql-db/
    ├── config/
    ├── data/
    ├── docs/
    ├── src/
    └── test/
```

## Quick Start

```powershell
cd mini-nosql-db
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src,test -Filter *.java).FullName
java -cp out minidb.test.MiniDbTests
java -cp out minidb.server.MiniDbServer config/single.properties
```

Client example:

```powershell
java -cp out minidb.client.MiniDbClient localhost 7070 PUT /kv/name string:Alice
java -cp out minidb.client.MiniDbClient localhost 7070 GET /kv/name
```

See [mini-nosql-db/README.md](mini-nosql-db/README.md) for the full command list and cluster demo.

## Course Scope

This is a minimum-delivery individual course design. The code focuses on the required parts: Java, C/S, multithreading, CRUD, persistence, cache/index, WAL recovery, API examples, tests, and static cluster behavior.

Extension features such as collection management, batch operations, file rotation, compression, LSM-tree storage, dynamic election, and GUI are intentionally not included.

