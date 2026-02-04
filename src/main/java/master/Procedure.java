package master;

import org.neo4j.gds.core.Username;
import org.neo4j.gds.core.utils.progress.TaskRegistryFactory;
import org.neo4j.gds.procedures.GraphDataScienceProcedures;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;

public class Procedure {

    @Context
    public GraphDataScienceProcedures facade;

    @Context
    public GraphDatabaseService dbService;

    @Context
    public KernelTransaction transaction;

    @Context
    public Username username = Username.EMPTY_USERNAME;

    @Context
    public Log log;

    @Context
    public TaskRegistryFactory taskRegistryFactory;

}
