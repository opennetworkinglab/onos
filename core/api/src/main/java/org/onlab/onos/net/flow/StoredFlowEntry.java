package org.onlab.onos.net.flow;


public interface StoredFlowEntry extends FlowEntry {

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

}
