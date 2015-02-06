package org.onosproject.store.consistent.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.kuujo.copycat.cluster.ClusterConfig;
import net.kuujo.copycat.log.FileLog;
import net.kuujo.copycat.netty.NettyTcpProtocol;
import net.kuujo.copycat.protocol.Consistency;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.store.serializers.StoreSerializer;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Database manager.
 */
@Component(immediate = true, enabled = true)
@Service
public class DatabaseManager implements DatabaseService {

    private final Logger log = getLogger(getClass());
    private PartitionedDatabase partitionedDatabase;
    public static final int COPYCAT_TCP_PORT = 7238; //  7238 = RAFT
    private static final String CONFIG_DIR = "../config";
    private static final String PARTITION_DEFINITION_FILE = "tablets.json";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    protected String nodeToUri(ControllerNode node) {
        return "tcp://" + node.ip() + ":" + COPYCAT_TCP_PORT;
    }

    @Activate
    public void activate() {

        final String logDir = System.getProperty("karaf.data", "./data");

        // load database configuration
        File file = new File(CONFIG_DIR, PARTITION_DEFINITION_FILE);
        log.info("Loading database definition: {}", file.getAbsolutePath());

        DatabaseDefinitionStore databaseDef = new DatabaseDefinitionStore(file);
        Map<String, Set<DefaultControllerNode>> partitionMap;
        try {
            partitionMap = databaseDef.read();
        } catch (IOException e) {
            log.error("Failed to load database config {}", file);
            throw new IllegalStateException("Failed to load database config", e);
        }

        String[] activeNodeUris = partitionMap.values()
                    .stream()
                    .reduce((s1, s2) -> Sets.union(s1, s2))
                    .get()
                    .stream()
                    .map(this::nodeToUri)
                    .toArray(String[]::new);

        String localNodeUri = nodeToUri(clusterService.getLocalNode());

        ClusterConfig clusterConfig = new ClusterConfig()
            .withProtocol(new NettyTcpProtocol())
            .withMembers(activeNodeUris)
            .withLocalMember(localNodeUri);

        PartitionedDatabaseConfig databaseConfig = new PartitionedDatabaseConfig();

        partitionMap.forEach((name, nodes) -> {
            Set<String> replicas = nodes.stream().map(this::nodeToUri).collect(Collectors.toSet());
            DatabaseConfig partitionConfig = new DatabaseConfig()
                            .withConsistency(Consistency.STRONG)
                            .withLog(new FileLog(logDir))
                            .withReplicas(replicas);
            databaseConfig.addPartition(name, partitionConfig);
        });

        partitionedDatabase = PartitionedDatabaseManager.create("onos-store", clusterConfig, databaseConfig);

        partitionedDatabase.open().whenComplete((db, error) -> {
            if (error != null) {
                log.warn("Failed to open database.", error);
            } else {
                log.info("Successfully opened database.");
            }
        });
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        partitionedDatabase.close().whenComplete((result, error) -> {
            if (error != null) {
                log.warn("Failed to cleanly close database.", error);
            } else {
                log.info("Successfully closed database.");
            }
        });
        log.info("Stopped");
    }

    @Override
    public <K, V> ConsistentMap<K , V> createConsistentMap(String name, StoreSerializer serializer) {
        return new ConsistentMapImpl<K, V>(name, partitionedDatabase, serializer);
    }
}