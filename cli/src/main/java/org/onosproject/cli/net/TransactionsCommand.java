/*
 * Copyright 2015 Open Networking Laboratory
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
import org.apache.karaf.shell.commands.Option;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.service.StorageAdminService;
import org.onosproject.store.service.Transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * CLI to work with database transactions in the system.
 */
@Command(scope = "onos", name = "transactions",
        description = "Utility for viewing and redriving database transactions")
public class TransactionsCommand extends AbstractShellCommand {

    @Option(name = "-r", aliases = "--redrive",
            description = "Redrive stuck transactions while removing those that are done",
            required = false, multiValued = false)
    private boolean redrive = false;

    private static final String FMT = "%-20s %-15s %-10s";

    /**
     * Displays transactions as text.
     *
     * @param transactions transactions
     */
    private void displayTransactions(Collection<Transaction> transactions) {
        print("---------------------------------------------");
        print(FMT, "Id", "State", "Updated");
        print("---------------------------------------------");
        transactions.forEach(txn -> print(FMT, txn.id(), txn.state(),  Tools.timeAgo(txn.lastUpdated())));
        if (transactions.size() > 0) {
            print("---------------------------------------------");
        }
    }

    /**
     * Converts collection of transactions into a JSON object.
     *
     * @param transactions transactions
     */
    private JsonNode json(Collection<Transaction> transactions) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode txns = mapper.createArrayNode();

        // Create a JSON node for each transaction
        transactions.stream().forEach(txn -> {
                    ObjectNode txnNode = mapper.createObjectNode();
                    txnNode.put("id", txn.id())
                    .put("state", txn.state().toString())
                    .put("lastUpdated", txn.lastUpdated());
                    txns.add(txnNode);
                });

        return txns;
    }

    @Override
    protected void execute() {
        StorageAdminService storageAdminService = get(StorageAdminService.class);

        if (redrive) {
            storageAdminService.redriveTransactions();
            return;
        }

        Collection<Transaction> transactions = storageAdminService.getTransactions();
        if (outputJson()) {
            print("%s", json(transactions));
        } else {
            displayTransactions(transactions);
        }
    }
}
