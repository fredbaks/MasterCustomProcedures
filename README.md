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

The database is accessible via java code or `localhost:7474/browser/`.

### Prerequisites

- Docker Engine installed on the server

**Script might need to be made executable** (only needed once):

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

### Other commands

```bash
# Tail container logs (detached mode)
./run.sh --logs
# Stop the container
./run.sh --stop
# Remove the container
./run.sh --down
# Refresh custom plugin jar in the docker container, stopping and starting in the process
./run.sh --refresh
```

### Bind-mounted directories

| Host path    | Container path           | Purpose                          |
| ------------ | ------------------------ | -------------------------------- |
| `./logs/`    | `/logs`                  | Server log files                 |
| `./output/`  | `/var/lib/neo4j/output`  | Procedure output files           |
| `./plugins/` | `/var/lib/neo4j/plugins` | GDS + custom procedures JARs     |
| `./conf/`    | `/var/lib/neo4j/conf`    | Logging config (`user-logs.xml`) |
| `./CSV/`     | `/var/lib/neo4j/import`  | CSV datasets for `CsvLoader`     |

---

## Converting datasets - `FileToCSV`

`FileToCSV` is a Java utility that converts files of different formats describing a graph into .csv formatted files. Neo4j can more easily load .csv files through cypher.
File created by FileToCSV are placed in `CSV/`, and are on the format expected by `CsvLoader`.

### Running

```bash
java -cp plugins/master-procedures-0.0.1.jar master.dataHandling.FileToCsv <filePath>
```

## Loading datasets — `CsvLoader`

`CsvLoader` is a standalone Java utility that loads the CSV datasets in `CSV/` into the
running Neo4j container.

### Running

```bash
java -cp plugins/master-procedures-0.0.1.jar master.dataHandling.CsvLoader <filename>
```

No argument prints usage and exits without touching the database.

---

## Creating graph coverage plots

Each run of an algorithm prodeces a results file in the `output/<graphProjectionName>-k_<hop-limit>`. This files have the format `<algorithm>-<hopLimit>-<graphProjectionName>-source_<sourceId>-target_<targetId>-<Timestamp>.csv`.

The file graphCoverageVislualization.py produces plots of graph coverages (node coverage) of each algorithm found in the folder, which shoudl be of same graph projection and hoplimit. It averages the runtime per 0.1 percent over all executions of the same algorithm.

It accepts three parameters:

- `--folder/-f <foldername>` - **Required**(str): Is the folder which the result files to be plotted is located
- `--hoplimit/-k <hoplimit>` - **Required**(int): Is the hoplimit the result was produces with
- `--show/-s` - **Optional**: Shows the plot when finished

### Example

```bash
py visualization/graphCoverageVisualization.py --folder testoutput/testGraph-k_3 --hoplimit 3

py visualization/graphCoverageVisualization.py --folder outputs/bio-grid-yeast-k_6 --hoplimit 6 --show
```

The plotting program assumes the csv files using the same format as the files produces by `PathEnumerationResultWriter`. Using the procedures in this project produces files in folders which follows the expected structure and format.

## Batch running queries - `ExperimentHandler`

The jar contains a Java class created specifically for running many, parallel enumeration queries. This is used to automate larger scale result production for the thesis.

It creates queries by first creating a list of 1000 source-target pairs. These pairs are unique in the list and verified that there exists at least one path from the source to the target with less than `hoplimit` hops.

The source-target pairs are stored in the folder `/source-target-pairs` as .csv files and if such a file exist, then it is used rather than creating a new one.

`ExperimentHandler` executes each source-pair target for each of the four hop-constrained source-target path enumeration algorithms implemented in this project. It executes the queries in parallel using a fixed thread pool. This is by default set to `4`, but this value should be changed to the number of cores on the processor to be used.

## Running

```bash
#Single
java -cp plugins/master-procedures-0.0.1.jar master.dataHandling.ExperimentHandler single <dataset/filename> <hoplimit> <isDatasetLoaded>

#Multiple
java -cp plugins/master-procedures-0.0.1.jar master.dataHandling.ExperimentHandler multiple <dataset1> <dataset2> ...

#Multiple with default
java -cp plugins/master-procedures-0.0.1.jar master.dataHandling.ExperimentHandler multiple
```

Parameters:

- \<dataset/filename> is a filename of a dataset in the folder `/CSV`. The name of the file is used as the name of the graph projection and therefore also the output folder.

- \<hoplimit> is the hoplimit

- \<isDatasetLoaded> is a boolean `true`/`false` which specifies if the dataset is loaded in the neo4j container or not. If `false` then the dataset is loaded using `CsvLoader`, otherwise this is skipped.

---

## **This repository is based on the Neo4j Procedure Template. Its readme is found below:**

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
