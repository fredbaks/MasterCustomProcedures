# Master Custom Procedures

This repository contains the code for Fredrik Baksaas' master thesis.  
It is a collection of Neo4j custom procedures mostly dealing with source to target path enumeration problems.

## Compiling and running test

Because of the use of GDS in the custom made procedures another testing approach was needed that the Neo4j Procedure Template originally used.

The new procedure tests use testcontainers, so docker is required as well as the latest Neo4j image.

The container is injected with both GDS and this project's .jar files which means that the tests can not run if the executables are not present.

Therefor, to compile the project use

```
.\mvnw clean compile -DskipTests
```

Without this the tests will fail every time because of a lack of executable.

To run tests use

```
.\mvnw test
```

For a specific test only use

```
.\mvnw test -Dtest=$testname$
```

## Running Neo4j with GDS on a server

A `docker-compose.yml` and `run.sh` helper script are included for deploying the full stack
(Neo4j 2025.10.1 + GDS + custom procedures) on any Linux server accessible only via terminal.

### Prerequisites

- Docker Engine installed on the server
- The project JAR built locally (`./mvnw clean package -DskipTests`) and copied to the server

### First-time setup

1. **Set a password** – edit `.env` and change `changeme` to a strong password:

   ```
   NEO4J_AUTH=neo4j/<your-password>
   ```

2. **Make the script executable** (only needed once):
   ```bash
   chmod +x run.sh
   ```

### Starting the container

```bash
# Foreground
./run.sh

# Background
./run.sh -d
```

On first start, Neo4j automatically downloads the GDS plugin (~100 MB).
The downloaded JAR is cached in `./plugins/` and reused on subsequent starts.

### Other commands

```bash
# Tail container logs (detached mode)
./run.sh --logs
# Stop and remove the container
./run.sh --stop
```

### Endpoints

| Endpoint      | Address                   |
| ------------- | ------------------------- |
| Neo4j Browser | `http://<server-ip>:7474` |
| Bolt          | `bolt://<server-ip>:7687` |

### Bind-mounted directories

| Host path    | Container path           | Purpose                          |
| ------------ | ------------------------ | -------------------------------- |
| `./logs/`    | `/logs`                  | Server log files                 |
| `./output/`  | `/var/lib/neo4j/output`  | Procedure output files           |
| `./plugins/` | `/var/lib/neo4j/plugins` | GDS + custom procedures JARs     |
| `./conf/`    | `/var/lib/neo4j/conf`    | Logging config (`user-logs.xml`) |
| `./CSV/`     | `/var/lib/neo4j/import`  | CSV datasets for `CsvLoader`     |

---

## Loading datasets — `CsvLoader`

`CsvLoader` is a standalone Java utility that loads the CSV datasets in `CSV/` into the
running Neo4j container. It always **clears the entire database first** before inserting,
since only one database is available and the datasets must not overlap.

### Graph schema

Every CSV file follows the `START_ID,END_ID,TYPE` edge-list format and is loaded as:

- **Nodes** — label `:Node`, property `id` (string)
- **Relationships** — type `:EDGE`, directed from `START_ID` → `END_ID`

### Running

```bash
java -cp target/master-procedures-0.0.1.jar master.CsvLoader <filename>
```

No argument prints usage and exits without touching the database.

### Connection configuration

| Environment variable | Default                 | Description                                                   |
| -------------------- | ----------------------- | ------------------------------------------------------------- |
| `NEO4J_BOLT_URL`     | `bolt://localhost:7687` | Bolt URL of the Neo4j instance                                |
| `NEO4J_AUTH`         | `neo4j/neo4j`           | Credentials in `user/password` format; use `none` for no auth |

---

**This repository is based on the Neo4j Procedure Template. Its readme is found below:**

# Neo4j Procedure Template

**Branch:** `2025.x`  
**Root:** [Neo4j Procedure Template GitHub](https://github.com/neo4j-examples/neo4j-procedure-template/blob/2025.x/src)

This project is an example you can use to build user defined procedures, functions, and aggregation functions in Neo4j.  
It contains two procedures, for reading and updating a full-text index.

To try this out, simply clone this repository and have a look at the source and test code (including Test-Server-Setup).

> **Note:**  
> This project requires a Neo4j `2025.x` dependency.

---

## User Defined Procedure

The user defined procedure allows you to get the incoming and outgoing relationships for a given node.

See [`GetRelationshipTypes.java`](https://github.com/neo4j-examples/neo4j-procedure-template/blob/2025.x/src/main/java/example/GetRelationshipTypes.java) and [`GetRelationshipTypesTests.java`](https://github.com/neo4j-examples/neo4j-procedure-template/blob/2025.x/src/test/java/example/GetRelationshipTypesTests.java).

```cypher
MATCH (n:Person)
CALL example.getRelationshipTypes(n)
YIELD outgoing, incoming
RETURN outgoing, incoming;
```

---

## User Defined Function

The user defined function is a simple join function that joins a list of strings using a delimiter.

See [`Join.java`](https://github.com/neo4j-examples/neo4j-procedure-template/blob/2025.x/src/main/java/example/Join.java) and [`JoinTest.java`](https://github.com/neo4j-examples/neo4j-procedure-template/blob/2025.x/src/test/java/example/JoinTest.java).

```cypher
RETURN master.join(['A','quick','brown','fox'],' ') as sentence
```

---

## User Defined Aggregation Function

The aggregation function `example.last` returns the last row of an aggregation.

```cypher
MATCH (n:Person)
WITH n ORDER BY n.born
WITH example.last(n) as last
RETURN last
```

See [`Last.java`](https://github.com/neo4j-examples/neo4j-procedure-template/blob/2025.x/src/main/java/example/Last.java) and [`LastTest.java`](https://github.com/neo4j-examples/neo4j-procedure-template/blob/2025.x/src/test/java/example/LastTest.java).

---

## Building

This project uses Maven. To build a jar-file with the procedure in this project, simply package the project with Maven:

```sh
./mvnw clean package
```

This will produce a jar-file, `target/procedure-template-1.0.0-SNAPSHOT.jar`, that can be deployed in the `plugin` directory of your Neo4j instance.

---

## License

Apache License V2, see [LICENSE](LICENSE)
