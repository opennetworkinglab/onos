package org.onlab.onos.net.flow;


/**
 * Represents a generalized match &amp; action pair to be applied to
 * an infrastucture device.
 */
public interface FlowEntry extends FlowRule {


    public enum FlowEntryState {

        /**
         * Indicates that this rule has been submitted for addition.
         * Not necessarily in  the flow table.
         */
        PENDING_ADD,

        /**
         * Rule has been added which means it is in the flow table.
         */
        ADDED,

        /**
         * Flow has been marked for removal, might still be in flow table.
         */
        PENDING_REMOVE,

        /**
         * Flow has been removed from flow table and can be purged.
         */
        REMOVED,

        /**
         * Indicates that the installation of this flow has failed.
         */
        FAILED
    }

    /**
     * Returns the flow entry state.
     *
     * @return flow entry state
     */
    FlowEntryState state();

    /**
     * Returns the number of milliseconds this flow rule has been applied.
     *
     * @return number of millis
     */
    long life();

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

    /**
     * When this flow entry was last deemed active.
     * @return epoch time of last activity
     */
    long lastSeen();

    /**
     * Sets the last active epoch time.
     */
    void setLastSeen();

    /**
     * Sets the new state for this entry.
     * @param newState new flow entry state.
     */
    void setState(FlowEntryState newState);

    /**
     * Sets how long this entry has been entered in the system.
     * @param life epoch time
     */
    void setLife(long life);

    /**
     * Number of packets seen by this entry.
     * @param packets a long value
     */
    void setPackets(long packets);

    /**
     * Number of bytes seen by this rule.
     * @param bytes a long value
     */
    void setBytes(long bytes);

    /**
     * Indicates the error type.
     * @return an integer value of the error
     */
    int errType();

    /**
     * Indicates the error code.
     * @return an integer value of the error
     */
    int errCode();

}
