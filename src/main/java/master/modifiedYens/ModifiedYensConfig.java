package master.modifiedYens;

import org.neo4j.gds.core.CypherMapWrapper;

public interface ModifiedYensConfig extends ModifiedYensBaseConfig {
     static ModifiedYensConfig of(CypherMapWrapper userInput) {
        return new ModifiedYensConfigImpl(userInput);
    }
}
