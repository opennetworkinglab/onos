package org.onosproject.ovsdb.lib;

import java.util.List;

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
 * OvsdbClient class.
 */
public interface OvsdbClient {
    /**
     * Gets the list of database names exposed by this ovsdb capable device.
     *
     * @return list of database names
     */
    ListenableFuture<List<String>> getDatabases();

    /**
     * Asynchronously returns the schema object for a specific database.
     *
     * @param database name of the database schema
     * @return DatabaseSchema future
     */
    ListenableFuture<DatabaseSchema> getSchema(String database);

    /**
     * Allows for a mini DSL way of collecting the transactions to be executed.
     * against the ovsdb instance.
     *
     * @return TransactionBuilder
     */
    TransactionBuilder transactBuilder(DatabaseSchema dbSchema);

    /**
     * Execute the list of operations in a single Transactions. Similar to the
     * transactBuilder() method.
     *
     * @param operations List of operations that needs to be part of a transact
     *            call
     * @return Future object representing the result of the transaction. Calling
     *         cancel on the Future would cause OVSDB cancel operation to be
     *         fired against the device.
     */
    ListenableFuture<List<OperationResult>> transact(DatabaseSchema dbSchema,
                                                     List<Operation> operations);

    /**
     * ovsdb <a href=
     * "http://tools.ietf.org/html/draft-pfaff-ovsdb-proto-04#section-4.1.5"
     * >monitor</a> operation.
     *
     * @param monitorRequests represents what needs to be monitored including a
     *            client specified monitor handle. This handle is used to later
     *            cancel ({@link #cancelMonitor(MonitorHandle)}) the monitor.
     * @param callback receives the monitor response
     */
    public <E extends TableSchema<E>> TableUpdates monitor(DatabaseSchema schema,
                                                           List<MonitorRequest<E>> monitorRequests,
                                                           MonitorCallBack callback);

    /**
     * ovsdb steal operation, see
     * {@link #lock(String, LockAquisitionCallback, LockStolenCallback)}.
     *
     * @param lockId
     * @return
     */
    public ListenableFuture<Boolean> steal(String lockId);

    /**
     * ovsdb unlock operaiton, see {@link #unLock(String)}.
     *
     * @param lockId
     * @return
     */
    public ListenableFuture<Boolean> unLock(String lockId);

    /**
     * Stops the echo service, i.e echo requests from the remote would not be
     * acknowledged after this call.
     */
    public void stopEchoService();

    public boolean isActive();

    public void disconnect();

    public DatabaseSchema getDatabaseSchema(String dbName);

    /**
     * User friendly convenient methods that make use of
     * TyperUtils.getTypedRowWrapper to create a Typed Row Proxy given the Typed
     * Table Class.
     *
     * @param klazz Typed Interface
     * @return Proxy wrapper for the actual raw Row class.
     */
    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(Class<T> klazz);

    /**
     * User friendly convenient methods that make use of getTypedRowWrapper to
     * create a Typed Row Proxy given DatabaseSchema and Typed Table Class.
     *
     * @param dbSchema Database Schema of interest
     * @param klazz Typed Interface
     * @return Proxy wrapper for the actual raw Row class.
     */
    public <T extends TypedBaseTable<?>> T createTypedRowWrapper(DatabaseSchema dbSchema,
                                                                 Class<T> klazz);

    /**
     * User friendly convenient method to get a Typed Row Proxy given a Typed
     * Table Class and the Row to be wrapped.
     *
     * @param klazz Typed Interface
     * @param row The actual Row that the wrapper is operating on. It can be
     *            null if the caller is just interested in getting ColumnSchema.
     * @return Proxy wrapper for the actual raw Row class.
     */
    public <T extends TypedBaseTable<?>> T getTypedRowWrapper(final Class<T> klazz,
                                                              final Row<GenericTableSchema> row);

}
