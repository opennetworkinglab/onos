/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.cli.net;

import java.util.Collection;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.primitives.TransactionId;
import org.onosproject.store.service.StorageAdminService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * CLI to view in-progress database transactions in the system.
 */
@Command(scope = "onos", name = "transactions",
        description = "Utility for listing pending/inprogress transactions")
public class TransactionsCommand extends AbstractShellCommand {

    /**
     * Converts collection of transactions into a JSON object.
     *
     * @param transactionIds transaction identifiers
     */
    private JsonNode json(Collection<TransactionId> transactionIds) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode txns = mapper.createArrayNode();
        transactionIds.forEach(id -> txns.add(id.toString()));
        return txns;
    }

    @Override
    protected void execute() {
        StorageAdminService storageAdminService = get(StorageAdminService.class);
        Collection<TransactionId> transactionIds = storageAdminService.getPendingTransactions();
        if (outputJson()) {
            print("%s", json(transactionIds));
        } else {
            transactionIds.forEach(id -> print("%s", id.toString()));
        }
    }
}
