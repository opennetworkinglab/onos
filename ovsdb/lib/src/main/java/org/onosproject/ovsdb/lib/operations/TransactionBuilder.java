/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */

package org.onosproject.ovsdb.lib.operations;

import java.util.ArrayList;
import java.util.List;

import org.onosproject.ovsdb.lib.impl.OvsdbClientImpl;
import org.onosproject.ovsdb.lib.schema.DatabaseSchema;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

public class TransactionBuilder {

    private DatabaseSchema eDatabaseSchema;
    OvsdbClientImpl ovs;
    ArrayList<Operation> operations = Lists.newArrayList();

    public TransactionBuilder(OvsdbClientImpl ovs, DatabaseSchema schema) {
        this.ovs = ovs;
        eDatabaseSchema = schema;
    }

    public ArrayList<Operation> getOperations() {
        return operations;
    }

    public TransactionBuilder add(Operation operation) {
        operations.add(operation);
        return this;
    }

    public List<Operation> build() {
        return operations;
    }

    public ListenableFuture<List<OperationResult>> execute() {
        return ovs.transact(eDatabaseSchema, operations);
    }
}
