package org.onosproject.ovsdb.lib;

import java.util.List;

import org.onosproject.ovsdb.lib.operations.Operation;
import org.onosproject.ovsdb.lib.operations.OperationResult;
import org.onosproject.ovsdb.lib.schema.DatabaseSchema;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * OvsdbClient class.
 */
public interface OvsdbClient {
    /**
     * Execute the list of operations in a single Transactions. Similar to the
     * transactBuilder() method
     *
     * @param operations List of operations that needs to be part of a transact
     *            call
     * @return Future object representing the result of the transaction. Calling
     *         cancel on the Future would cause OVSDB cancel operation to be
     *         fired against the device.
     */
    ListenableFuture<List<OperationResult>> transact(DatabaseSchema dbSchema,
                                                     List<Operation> operations);

}
