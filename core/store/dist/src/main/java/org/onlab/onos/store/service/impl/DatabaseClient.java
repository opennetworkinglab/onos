package org.onlab.onos.store.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.kuujo.copycat.Copycat;

import org.onlab.onos.store.service.DatabaseException;
import org.onlab.onos.store.service.ReadRequest;
import org.onlab.onos.store.service.WriteRequest;

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

    public List<InternalReadResult> batchRead(List<ReadRequest> requests) {

        CompletableFuture<List<InternalReadResult>> future = copycat.submit("read", requests);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }

    public List<InternalWriteResult> batchWrite(List<WriteRequest> requests) {

        CompletableFuture<List<InternalWriteResult>> future = copycat.submit("write", requests);
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DatabaseException(e);
        }
    }
}
