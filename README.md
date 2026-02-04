# Master Custom Procedures

This repository contains the code for Fredrik Baksaas' master thesis.  
It is a collection of Neo4j custom procedures mostly dealing with source to target path enumeration problems.


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
RETURN example.join(['A','quick','brown','fox'],' ') as sentence
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
