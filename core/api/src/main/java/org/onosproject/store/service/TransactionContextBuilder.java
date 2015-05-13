package org.onosproject.store.service;

/**
 * Interface definition for a transaction context builder.
 */
public interface TransactionContextBuilder {

    /**
     * Disables distribution of map entries across multiple database partitions.
     * <p>
     * When partitioning is disabled, the returned map will have a single
     * partition that spans the entire cluster. Furthermore, the changes made to
     * the map are ephemeral and do not survive a full cluster restart.
     * </p>
     * <p>
     * Note: By default, partitions are enabled. This feature is intended to
     * simplify debugging.
     * </p>
     *
     * @return this TransactionalContextBuilder
     */
    TransactionContextBuilder withPartitionsDisabled();

    /**
     * Builds a TransactionContext based on configuration options supplied to this
     * builder.
     *
     * @return a new TransactionalContext
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    TransactionContext build();
}
