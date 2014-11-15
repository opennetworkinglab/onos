package org.onlab.onos.store.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.kuujo.copycat.Copycat;
import net.kuujo.copycat.event.EventHandler;
import net.kuujo.copycat.event.LeaderElectEvent;

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
public class DatabaseClient {

    private static final int RETRIES = 5;

    private static final int TIMEOUT_MS = 2000;

    private final Logger log = getLogger(getClass());

    private final Copycat copycat;

    public DatabaseClient(Copycat copycat) {
        this.copycat = checkNotNull(copycat);
    }

    public void waitForLeader() {
        if (copycat.leader() != null) {
            return;
        }

        log.info("No leader in cluster, waiting for election.");
        final CountDownLatch latch = new CountDownLatch(1);
        final EventHandler<LeaderElectEvent> leaderLsnr = new EventHandler<LeaderElectEvent>() {

            @Override
            public void handle(LeaderElectEvent event) {
                log.info("Leader chosen: {}", event);
                latch.countDown();
            }
        };

        copycat.event(LeaderElectEvent.class).registerHandler(leaderLsnr);
        try {
            while (copycat.leader() == null) {
                latch.await(200, TimeUnit.MILLISECONDS);
            }
            log.info("Leader appeared: {}", copycat.leader());
            return;
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for Leader", e);
            Thread.currentThread().interrupt();
        } finally {
            copycat.event(LeaderElectEvent.class).unregisterHandler(leaderLsnr);
        }
    }

    public boolean createTable(String tableName) {
        waitForLeader();
        CompletableFuture<Boolean> future = copycat.submit("createTable", tableName);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public boolean createTable(String tableName, int ttlMillis) {
        waitForLeader();
        CompletableFuture<Boolean> future = copycat.submit("createTableWithExpiration", tableName);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public void dropTable(String tableName) {
        waitForLeader();
        CompletableFuture<Void> future = copycat.submit("dropTable", tableName);
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public void dropAllTables() {
        waitForLeader();
        CompletableFuture<Void> future = copycat.submit("dropAllTables");
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public Set<String> listTables() {
        waitForLeader();
        try {
            for (int i = 0; i < RETRIES; ++i) {
                CompletableFuture<Set<String>> future = copycat.submit("listTables");
                try {
                    return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    log.debug("Timed out retrying {}", i);
                    future.cancel(true);
                    waitForLeader();
                }
            }
            // TODO: proper timeout handling
            log.error("Timed out");
            return Collections.emptySet();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public List<ReadResult> batchRead(BatchReadRequest batchRequest) {
        waitForLeader();
        CompletableFuture<List<ReadResult>> future = copycat.submit("read", batchRequest);
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        } catch (TimeoutException e) {
            throw new DatabaseException(e);
        }
    }

    public List<WriteResult> batchWrite(BatchWriteRequest batchRequest) {
        waitForLeader();
        CompletableFuture<List<WriteResult>> future = copycat.submit("write", batchRequest);
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        } catch (TimeoutException e) {
            throw new DatabaseException(e);
        }
    }

    public Map<String, VersionedValue> getAll(String tableName) {
        waitForLeader();
        CompletableFuture<Map<String, VersionedValue>> future = copycat.submit("getAll", tableName);
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        } catch (TimeoutException e) {
            throw new DatabaseException(e);
        }
    }
}
