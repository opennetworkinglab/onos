package org.onosproject.ovsdb.lib.impl;

import java.util.List;

import org.onosproject.ovsdb.lib.OvsdbClient;
import org.onosproject.ovsdb.lib.operations.Operation;
import org.onosproject.ovsdb.lib.operations.OperationResult;
import org.onosproject.ovsdb.lib.schema.DatabaseSchema;

import com.google.common.util.concurrent.ListenableFuture;

public class OvsdbClientImpl implements OvsdbClient {

    @Override
    public ListenableFuture<List<OperationResult>> transact(DatabaseSchema dbSchema,
                                                            List<Operation> operations) {
        // TODO Auto-generated method stub
        return null;
    }

}
