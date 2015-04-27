package org.onosproject.ovsdb.lib.impl;

import java.util.List;

import org.onosproject.ovsdb.lib.MonitorCallBack;
import org.onosproject.ovsdb.lib.OvsdbClient;
import org.onosproject.ovsdb.lib.message.MonitorRequest;
import org.onosproject.ovsdb.lib.message.TableUpdates;
import org.onosproject.ovsdb.lib.notation.Row;
import org.onosproject.ovsdb.lib.operations.Operation;
import org.onosproject.ovsdb.lib.operations.OperationResult;
import org.onosproject.ovsdb.lib.operations.TransactionBuilder;
import org.onosproject.ovsdb.lib.schema.DatabaseSchema;
import org.onosproject.ovsdb.lib.schema.GenericTableSchema;
import org.onosproject.ovsdb.lib.schema.TableSchema;
import org.onosproject.ovsdb.lib.schema.typed.TypedBaseTable;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * OvsdbClientImpl class.
 *
 *
 */
public class OvsdbClientImpl implements OvsdbClient {

    @Override
    public ListenableFuture<List<OperationResult>> transact(DatabaseSchema dbSchema,
                                                            List<Operation> operations) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<List<String>> getDatabases() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<DatabaseSchema> getSchema(String database) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TransactionBuilder transactBuilder(DatabaseSchema dbSchema) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <E extends TableSchema<E>> TableUpdates monitor(DatabaseSchema schema,
                                                           List<MonitorRequest<E>> monitorRequests,
                                                           MonitorCallBack callback) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<Boolean> steal(String lockId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<Boolean> unLock(String lockId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void stopEchoService() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void disconnect() {
        // TODO Auto-generated method stub

    }

    @Override
    public DatabaseSchema getDatabaseSchema(String dbName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(Class<T> klazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(DatabaseSchema dbSchema,
                                                                 Class<T> klazz) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends TypedBaseTable<?>> T getTypedRowWrapper(Class<T> klazz,
                                                              Row<GenericTableSchema> row) {
        // TODO Auto-generated method stub
        return null;
    }

}
