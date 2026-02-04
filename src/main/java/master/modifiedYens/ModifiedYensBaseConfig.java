// Source code is decompiled from a .class file using FernFlower decompiler (from Intellij IDEA).
package master.modifiedYens;

import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.RelationshipWeightConfig;
import org.neo4j.gds.config.SourceNodeConfig;
import org.neo4j.gds.config.TargetNodeConfig;

public interface ModifiedYensBaseConfig extends TargetNodeConfig, AlgoBaseConfig, SourceNodeConfig, RelationshipWeightConfig {

   // Number of shortest paths to compute
    @Configuration.IntegerRange(min = 1)
    int l();

    @Configuration.Ignore
    default ModifiedYensParameters toParameters() {
        return new ModifiedYensParameters(
            sourceNode(),
            targetNode(),
            l(),
            concurrency()
        );
    }
}
