package master.modifiedYens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import org.jetbrains.annotations.NotNull;
import org.neo4j.gds.NodeLabel;
import org.neo4j.gds.RelationshipType;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.config.BaseConfig;
import org.neo4j.gds.config.ConcurrencyConfig;
import org.neo4j.gds.config.JobIdParser;
import org.neo4j.gds.config.SourceNodeConfig;
import org.neo4j.gds.config.TargetNodeConfig;
import org.neo4j.gds.core.CypherMapAccess;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.core.concurrency.Concurrency;
import org.neo4j.gds.core.JobId;

@Generated("org.neo4j.gds.proc.ConfigurationProcessor")
public final class ModifiedYensConfigImpl implements ModifiedYensConfig {
    private int l;

    private long targetNode;

    private List<String> relationshipTypes;

    private List<String> nodeLabels;

    private Optional<String> usernameOverride;

    private boolean sudo;

    private boolean logProgress;

    private Concurrency concurrency;

    private JobId jobId;

    private long sourceNode;

    private Optional<String> relationshipWeightProperty;

    public ModifiedYensConfigImpl(@NotNull CypherMapAccess config) {
        ArrayList<IllegalArgumentException> errors = new ArrayList<>();
        try {
            this.l = config.requireInt("l");
            CypherMapAccess.validateIntegerRange("l", l, 1, 2147483647, true, true);
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            this.targetNode = TargetNodeConfig.parseTargetNode(config.requireChecked("targetNode", Object.class));
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            this.relationshipTypes = CypherMapAccess.failOnNull("relationshipTypes",
                    config.getChecked("relationshipTypes", ModifiedYensConfig.super.relationshipTypes(), List.class));
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            this.nodeLabels = CypherMapAccess.failOnNull("nodeLabels",
                    config.getChecked("nodeLabels", ModifiedYensConfig.super.nodeLabels(), List.class));
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            this.usernameOverride = CypherMapAccess.failOnNull("username",
                    config.getOptional("username", String.class).map(BaseConfig::trim));
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            this.sudo = config.getBool("sudo", ModifiedYensConfig.super.sudo());
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            this.logProgress = config.getBool("logProgress", ModifiedYensConfig.super.logProgress());
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            this.concurrency = CypherMapAccess.failOnNull("concurrency", ConcurrencyConfig
                    .parse(config.getChecked("concurrency", ModifiedYensConfig.super.concurrency(), Object.class)));
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            this.jobId = CypherMapAccess.failOnNull("jobId",
                    JobIdParser.parse(config.getChecked("jobId", ModifiedYensConfig.super.jobId(), Object.class)));
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            this.sourceNode = SourceNodeConfig.parseSourceNode(config.requireChecked("sourceNode", Object.class));
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            this.relationshipWeightProperty = CypherMapAccess.failOnNull("relationshipWeightProperty",
                    config.getOptional("relationshipWeightProperty", String.class));
        } catch (IllegalArgumentException e) {
            errors.add(e);
        }
        try {
            validateConcurrency();
        } catch (IllegalArgumentException e) {
            errors.add(e);
        } catch (NullPointerException e) {
        }
        try {
            validateRelationshipWeightProperty();
        } catch (IllegalArgumentException e) {
            errors.add(e);
        } catch (NullPointerException e) {
        }
        if (!errors.isEmpty()) {
            if (errors.size() == 1) {
                throw errors.get(0);
            } else {
                String combinedErrorMsg = errors.stream().map(IllegalArgumentException::getMessage)
                        .collect(Collectors.joining(System.lineSeparator() + "\t\t\t\t",
                                "Multiple errors in configuration arguments:" + System.lineSeparator() + "\t\t\t\t",
                                ""));
                IllegalArgumentException combinedError = new IllegalArgumentException(combinedErrorMsg);
                errors.forEach(error -> combinedError.addSuppressed(error));
                throw combinedError;
            }
        }
    }

    @Override
    public int l() {
        return this.l;
    }

    @Override
    public long targetNode() {
        return this.targetNode;
    }

    @Override
    public List<String> relationshipTypes() {
        return this.relationshipTypes;
    }

    @Override
    public List<String> nodeLabels() {
        return this.nodeLabels;
    }

    @Override
    public void graphStoreValidation(GraphStore graphStore, Collection<NodeLabel> selectedLabels,
            Collection<RelationshipType> selectedRelationshipTypes) {
        ArrayList<IllegalArgumentException> errors_ = new ArrayList<>();
        try {
            validateTargetNode(graphStore, selectedLabels, selectedRelationshipTypes);
        } catch (IllegalArgumentException e) {
            errors_.add(e);
        }
        try {
            validateNodeLabels(graphStore, selectedLabels, selectedRelationshipTypes);
        } catch (IllegalArgumentException e) {
            errors_.add(e);
        }
        try {
            validateRelationshipTypes(graphStore, selectedLabels, selectedRelationshipTypes);
        } catch (IllegalArgumentException e) {
            errors_.add(e);
        }
        try {
            validateSourceNode(graphStore, selectedLabels, selectedRelationshipTypes);
        } catch (IllegalArgumentException e) {
            errors_.add(e);
        }
        try {
            relationshipWeightValidation(graphStore, selectedLabels, selectedRelationshipTypes);
        } catch (IllegalArgumentException e) {
            errors_.add(e);
        }
        if (!errors_.isEmpty()) {
            if (errors_.size() == 1) {
                throw errors_.get(0);
            } else {
                String combinedErrorMsg_ = errors_.stream().map(IllegalArgumentException::getMessage)
                        .collect(Collectors.joining(System.lineSeparator() + "\t\t\t\t",
                                "Multiple errors in configuration arguments:" + System.lineSeparator() + "\t\t\t\t",
                                ""));
                IllegalArgumentException combinedError_ = new IllegalArgumentException(combinedErrorMsg_);
                errors_.forEach(error_ -> combinedError_.addSuppressed(error_));
                throw combinedError_;
            }
        }
    }

    @Override
    public Optional<String> usernameOverride() {
        return this.usernameOverride;
    }

    @Override
    public boolean sudo() {
        return this.sudo;
    }

    @Override
    public boolean logProgress() {
        return this.logProgress;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("l", l());
        map.put("targetNode", targetNode());
        map.put("relationshipTypes", relationshipTypes());
        map.put("nodeLabels", nodeLabels());
        usernameOverride().ifPresent(username -> map.put("username", username));
        map.put("sudo", sudo());
        map.put("logProgress", logProgress());
        map.put("concurrency", org.neo4j.gds.config.ConcurrencyConfig.render(concurrency()));
        map.put("jobId", jobId().asString());
        map.put("sourceNode", sourceNode());
        relationshipWeightProperty().ifPresent(
                relationshipWeightProperty -> map.put("relationshipWeightProperty", relationshipWeightProperty));
        return map;
    }

    @Override
    public Collection<String> configKeys() {
        return Arrays.asList("l", "targetNode", "relationshipTypes", "nodeLabels", "username", "sudo", "logProgress",
                "concurrency", "jobId", "sourceNode", "relationshipWeightProperty");
    }

    @Override
    public Concurrency concurrency() {
        return this.concurrency;
    }

    @Override
    public JobId jobId() {
        return this.jobId;
    }

    @Override
    public long sourceNode() {
        return this.sourceNode;
    }

    @Override
    public Optional<String> relationshipWeightProperty() {
        return this.relationshipWeightProperty;
    }

    public static ModifiedYensConfigImpl.Builder builder() {
        return new ModifiedYensConfigImpl.Builder();
    }

    public static final class Builder {
        private final Map<String, Object> config;

        public Builder() {
            this.config = new HashMap<>();
        }

        public static ModifiedYensConfigImpl.Builder from(
                ModifiedYensConfig baseConfig) {
            var builder = new ModifiedYensConfigImpl.Builder();
            builder.l(baseConfig.l());
            builder.targetNode(baseConfig.targetNode());
            builder.relationshipTypes(baseConfig.relationshipTypes());
            builder.nodeLabels(baseConfig.nodeLabels());
            builder.usernameOverride(baseConfig.usernameOverride());
            builder.sudo(baseConfig.sudo());
            builder.logProgress(baseConfig.logProgress());
            builder.concurrency(baseConfig.concurrency());
            builder.jobId(baseConfig.jobId());
            builder.sourceNode(baseConfig.sourceNode());
            builder.relationshipWeightProperty(baseConfig.relationshipWeightProperty());
            return builder;
        }

        public ModifiedYensConfigImpl.Builder l(int l) {
            this.config.put("l", l);
            return this;
        }

        public ModifiedYensConfigImpl.Builder targetNode(Object targetNode) {
            this.config.put("targetNode", targetNode);
            return this;
        }

        public ModifiedYensConfigImpl.Builder relationshipTypes(
                List<String> relationshipTypes) {
            this.config.put("relationshipTypes", relationshipTypes);
            return this;
        }

        public ModifiedYensConfigImpl.Builder nodeLabels(List<String> nodeLabels) {
            this.config.put("nodeLabels", nodeLabels);
            return this;
        }

        public ModifiedYensConfigImpl.Builder usernameOverride(String usernameOverride) {
            this.config.put("username", usernameOverride);
            return this;
        }

        public ModifiedYensConfigImpl.Builder usernameOverride(
                Optional<String> usernameOverride) {
            usernameOverride.ifPresent(actualusernameOverride -> this.config.put("username", actualusernameOverride));
            return this;
        }

        public ModifiedYensConfigImpl.Builder sudo(boolean sudo) {
            this.config.put("sudo", sudo);
            return this;
        }

        public ModifiedYensConfigImpl.Builder logProgress(boolean logProgress) {
            this.config.put("logProgress", logProgress);
            return this;
        }

        public ModifiedYensConfigImpl.Builder concurrency(Object concurrency) {
            this.config.put("concurrency", concurrency);
            return this;
        }

        public ModifiedYensConfigImpl.Builder jobId(Object jobId) {
            this.config.put("jobId", jobId);
            return this;
        }

        public ModifiedYensConfigImpl.Builder sourceNode(Object sourceNode) {
            this.config.put("sourceNode", sourceNode);
            return this;
        }

        public ModifiedYensConfigImpl.Builder relationshipWeightProperty(
                String relationshipWeightProperty) {
            this.config.put("relationshipWeightProperty", relationshipWeightProperty);
            return this;
        }

        public ModifiedYensConfigImpl.Builder relationshipWeightProperty(
                Optional<String> relationshipWeightProperty) {
            relationshipWeightProperty.ifPresent(actualrelationshipWeightProperty -> this.config
                    .put("relationshipWeightProperty", actualrelationshipWeightProperty));
            return this;
        }

        public ModifiedYensConfig build() {
            CypherMapWrapper config = CypherMapWrapper.create(this.config);
            return new ModifiedYensConfigImpl(config);
        }
    }
}
