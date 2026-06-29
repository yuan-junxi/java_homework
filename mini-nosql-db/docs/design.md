# Design Notes

## Architecture

Mini NoSQL DB uses a simple C/S structure:

```text
CLI Client -> TCP Socket -> MiniDbServer -> Request Dispatcher -> KvStore
                                                        |
                                                        +-> WAL file
                                                        +-> in-memory index
                                                        +-> LRU cache
```

In master mode, write requests are also forwarded to statically configured slave nodes:

```text
Client -> Master -> local WAL/index/cache
                 -> REPLPUT/REPLPATCH/REPLDELETE -> Slave
```

## Main Modules

- `minidb.client`: command line client and socket helper.
- `minidb.server`: server bootstrap, request dispatch, config, and replication.
- `minidb.core`: typed value model, key-value store, and LRU cache.
- `minidb.persist`: append-only WAL log and replay logic.
- `minidb.test`: no-dependency test harness.

## Consistency Rule

For write operations, the store appends the WAL record first and then updates memory. When the server restarts, it replays the WAL from the beginning to rebuild the latest state.

This is not a full database transaction system, but it is enough to show the basic idea of redo-log recovery.

