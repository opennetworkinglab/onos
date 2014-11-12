package org.onlab.onos.store.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.kuujo.copycat.Copycat;

import org.onlab.onos.store.service.BatchReadRequest;
import org.onlab.onos.store.service.BatchWriteRequest;
import org.onlab.onos.store.service.DatabaseException;
import org.onlab.onos.store.service.ReadResult;
import org.onlab.onos.store.service.WriteResult;

/**
 * Client for interacting with the Copycat Raft cluster.
 */
public class DatabaseClient {

    private final Copycat copycat;

    public DatabaseClient(Copycat copycat) {
        this.copycat = checkNotNull(copycat);
    }

    public boolean createTable(String tableName) {

        CompletableFuture<Boolean> future = copycat.submit("createTable", tableName);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public boolean createTable(String tableName, int ttlMillis) {

        CompletableFuture<Boolean> future = copycat.submit("createTable", tableName, ttlMillis);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public void dropTable(String tableName) {

        CompletableFuture<Void> future = copycat.submit("dropTable", tableName);
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public void dropAllTables() {

        CompletableFuture<Void> future = copycat.submit("dropAllTables");
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public List<String> listTables() {

        CompletableFuture<List<String>> future = copycat.submit("listTables");
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public List<ReadResult> batchRead(BatchReadRequest batchRequest) {

        CompletableFuture<List<ReadResult>> future = copycat.submit("read", batchRequest);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public List<WriteResult> batchWrite(BatchWriteRequest batchRequest) {

        CompletableFuture<List<WriteResult>> future = copycat.submit("write", batchRequest);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }
}
