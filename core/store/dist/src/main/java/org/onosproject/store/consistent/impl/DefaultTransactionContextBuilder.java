package org.onosproject.store.consistent.impl;

import org.onosproject.store.service.TransactionContext;
import org.onosproject.store.service.TransactionContextBuilder;

/**
 * The default implementation of a transaction context builder. This builder
 * generates a {@link DefaultTransactionContext}.
 */
public class DefaultTransactionContextBuilder implements TransactionContextBuilder {

    private boolean partitionsEnabled = true;
    private final Database partitionedDatabase;
    private final Database inMemoryDatabase;
    private final long transactionId;

    public DefaultTransactionContextBuilder(
            Database inMemoryDatabase, Database partitionedDatabase, long transactionId) {
        this.partitionedDatabase = partitionedDatabase;
        this.inMemoryDatabase = inMemoryDatabase;
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
                partitionsEnabled ? partitionedDatabase : inMemoryDatabase,
                transactionId);
    }

}
