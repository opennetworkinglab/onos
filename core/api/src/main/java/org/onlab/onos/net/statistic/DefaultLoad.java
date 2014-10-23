package org.onlab.onos.net.statistic;

import org.onlab.onos.net.flow.FlowRuleProvider;

/**
 * Implementation of a load.
 */
public class DefaultLoad implements Load {

    private final boolean isValid;
    private final long current;
    private final long previous;
    private final long time;

    /**
     * Creates an invalid load.
     */
    public DefaultLoad() {
        this.isValid = false;
        this.time = System.currentTimeMillis();
        this.current = -1;
        this.previous = -1;
    }

    /**
     * Creates a load value from the parameters.
     * @param current the current value
     * @param previous the previous value
     */
    public DefaultLoad(long current, long previous) {
        this.current = current;
        this.previous = previous;
        this.time = System.currentTimeMillis();
        this.isValid = true;
    }

    @Override
    public long rate() {
        return (current - previous) / FlowRuleProvider.POLL_INTERVAL;
    }

    @Override
    public long latest() {
        return current;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public long time() {
        return time;
    }
}
