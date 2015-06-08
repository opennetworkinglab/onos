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
