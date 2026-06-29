# Protocol Notes

The server reads one UTF-8 request line and returns one UTF-8 response line.

## Request Shape

```text
METHOD PATH [body]
```

## Public Operations

```text
PUT /kv/<key> <value>
GET /kv/<key>
PATCH /kv/<key> <value>
DELETE /kv/<key>
GET /keys
GET /info
POST /shutdown
```

`POST /shutdown` is mainly used by tests and local demo scripts.

## Value Body

```text
string:hello
number:12
set:java,db,socket
map:name=Alice,score=95
```

## Internal Replication Operations

The master sends these requests to slaves:

```text
REPLPUT /kv/<key> <value>
REPLPATCH /kv/<key> <value>
REPLDELETE /kv/<key>
```

