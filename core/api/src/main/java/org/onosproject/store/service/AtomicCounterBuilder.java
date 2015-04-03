package org.onosproject.store.service;

/**
 * Builder for AtomicCounter.
 */
public interface AtomicCounterBuilder {

    /**
     * Sets the name for the atomic counter.
     * <p>
     * Each atomic counter is identified by a unique name.
     * </p>
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param name name of the atomic counter
     * @return this AtomicCounterBuilder
     */
    public AtomicCounterBuilder withName(String name);

    /**
     * Creates this counter on the partition that spans the entire cluster.
     * <p>
     * When partitioning is disabled, the counter state will be
     * ephemeral and does not survive a full cluster restart.
     * </p>
     * <p>
     * Note: By default partitions are enabled.
     * </p>
     * @return this AtomicCounterBuilder
     */
    public AtomicCounterBuilder withPartitionsDisabled();

    /**
     * Builds a AtomicCounter based on the configuration options
     * supplied to this builder.
     *
     * @return new AtomicCounter
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    public AtomicCounter build();

    /**
     * Builds a AsyncAtomicCounter based on the configuration options
     * supplied to this builder.
     *
     * @return new AsyncAtomicCounter
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    public AsyncAtomicCounter buildAsyncCounter();
}
