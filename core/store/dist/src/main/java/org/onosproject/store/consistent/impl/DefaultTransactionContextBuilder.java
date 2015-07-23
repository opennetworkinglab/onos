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
package org.onosproject.store.consistent.impl;

import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionContextBuilder;

/**
 * The default implementation of a transaction context builder. This builder
 * generates a {@link DefaultTransactionContext}.
 */
public class DefaultTransactionContextBuilder implements TransactionContextBuilder {

    private boolean partitionsEnabled = true;
    private final DatabaseManager manager;
    private final long transactionId;

    public DefaultTransactionContextBuilder(DatabaseManager manager, long transactionId) {
        this.manager = manager;
        this.transactionId = transactionId;
    }

    @Override
    public TransactionContextBuilder withPartitionsDisabled() {
        partitionsEnabled = false;
        return this;
    }

    @Override
    public TransactionContext build() {
        return new DefaultTransactionContext(
                transactionId,
                partitionsEnabled ? manager.partitionedDatabase : manager.inMemoryDatabase,
                () -> partitionsEnabled ? manager.consistentMapBuilder()
                                        : manager.consistentMapBuilder().withPartitionsDisabled());
    }
}
