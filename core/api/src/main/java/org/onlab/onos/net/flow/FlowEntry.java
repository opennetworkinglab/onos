package org.onlab.onos.net.flow;

/**
 * Represents a flow rule and its associated accumulated metrics.
 */
public interface FlowEntry extends FlowRule {

    /**
     * Returns the ID of this flow.
     *
     * @return the flow ID
     */
    FlowId id();

    /**
     * Returns the number of milliseconds this flow rule has been applied.
     *
     * @return number of millis
     */
    long lifeMillis();

    /**
     * Returns the number of milliseconds this flow rule has been idle.
     *
     * @return number of millis
     */
    long idleMillis();

    /**
     * Returns the number of packets this flow rule has matched.
     *
     * @return number of packets
     */
    long packets();

    /**
     * Returns the number of bytes this flow rule has matched.
     *
     * @return number of bytes
     */
    long bytes();

}
