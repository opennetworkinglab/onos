/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal.
 */
package org.onosproject.ovsdb.lib.message;

import java.util.List;

import org.onosproject.ovsdb.lib.jsonrpc.Params;
import org.onosproject.ovsdb.lib.operations.Operation;
import org.onosproject.ovsdb.lib.schema.DatabaseSchema;

import com.google.common.collect.Lists;

public class TransactBuilder implements Params {

    List<Operation> requests = Lists.newArrayList();
    DatabaseSchema dbSchema;

    public TransactBuilder(DatabaseSchema dbSchema) {
        this.dbSchema = dbSchema;
    }

    public List<Operation> getRequests() {
        return requests;
    }

    @Override
    public List<Object> params() {
        List<Object> lists = Lists.newArrayList((Object) dbSchema.getName());
        lists.addAll(requests);
        return lists;
    }

    public void addOperations(List<Operation> o) {
        requests.addAll(o);
    }

    public void addOperation(Operation o) {
        requests.add(o);
    }
}
