package org.onlab.onos.store.service.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.kuujo.copycat.cluster.Member;
import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.event.EventHandler;
import net.kuujo.copycat.event.LeaderElectEvent;
import net.kuujo.copycat.protocol.SubmitRequest;
import net.kuujo.copycat.protocol.SubmitResponse;
import net.kuujo.copycat.spi.protocol.ProtocolClient;

import org.onlab.onos.store.service.BatchReadRequest;
import org.onlab.onos.store.service.BatchWriteRequest;
import org.onlab.onos.store.service.DatabaseException;
import org.onlab.onos.store.service.ReadResult;
import org.onlab.onos.store.service.VersionedValue;
import org.onlab.onos.store.service.WriteResult;
import org.slf4j.Logger;

/**
 * Client for interacting with the Copycat Raft cluster.
 */
public class DatabaseClient implements EventHandler<LeaderElectEvent> {

    private static final int RETRIES = 5;

    private static final int TIMEOUT_MS = 2000;

    private final Logger log = getLogger(getClass());

    private final DatabaseProtocolService protocol;
    private volatile ProtocolClient copycat = null;
    private volatile Member currentLeader = null;

    public DatabaseClient(DatabaseProtocolService protocol) {
        this.protocol = protocol;
    }

    @Override
    public void handle(LeaderElectEvent event) {
        Member newLeader = event.leader();
        if (newLeader != null && !newLeader.equals(currentLeader)) {
            currentLeader = newLeader;
            if (copycat != null) {
                copycat.close();
            }
            copycat = protocol.createClient((TcpMember) currentLeader);
            copycat.connect();
        }
    }

    private String nextRequestId() {
        return UUID.randomUUID().toString();
    }

    public void waitForLeader() {
        if (currentLeader != null) {
            return;
        }

        log.info("No leader in cluster, waiting for election.");

        try {
            while (currentLeader == null) {
                Thread.sleep(200);
            }
            log.info("Leader appeared: {}", currentLeader);
            return;
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for Leader", e);
            Thread.currentThread().interrupt();
        }
    }

    private <T> T submit(String operationName, Object... args) {
        waitForLeader();
        if (currentLeader == null) {
            throw new DatabaseException("Raft cluster does not have a leader.");
        }

        SubmitRequest request =
                new SubmitRequest(nextRequestId(), operationName, Arrays.asList(args));

        CompletableFuture<SubmitResponse> submitResponse = copycat.submit(request);

        log.debug("Sent {} to {}", request, currentLeader);

        try {
            return (T) submitResponse.get(TIMEOUT_MS, TimeUnit.MILLISECONDS).result();
        } catch (ExecutionException | InterruptedException e) {
            throw new DatabaseException(e);
        } catch (TimeoutException e) {
            throw new DatabaseException.Timeout(e);
        }
    }

    public boolean createTable(String tableName) {
        return submit("createTable", tableName);
    }

    public boolean createTable(String tableName, int ttlMillis) {
        return submit("createTable", tableName, ttlMillis);
    }

    public void dropTable(String tableName) {
        submit("dropTable", tableName);
    }

    public void dropAllTables() {
        submit("dropAllTables");
    }

    public Set<String> listTables() {
        return submit("listTables");
    }

    public List<ReadResult> batchRead(BatchReadRequest batchRequest) {
        return submit("read", batchRequest);
    }

    public List<WriteResult> batchWrite(BatchWriteRequest batchRequest) {
        return submit("write", batchRequest);
    }

    public Map<String, VersionedValue> getAll(String tableName) {
        return submit("getAll", tableName);
    }
}
