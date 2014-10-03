package org.onlab.onos.net.flow;

import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.DeviceId;

/**
 * Represents a generalized match &amp; action pair to be applied to
 * an infrastucture device.
 */
public interface FlowRule {

    static final int MAX_TIMEOUT = 60;
    static final int MIN_PRIORITY = 0;

    public enum FlowRuleState {
        /**
         * Indicates that this rule has been created.
         */
        CREATED,

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
        REMOVED
    }

    /**
     * Returns the flow rule state.
     *
     * @return flow rule state
     */
    FlowRuleState state();

    //TODO: build cookie value
    /**
     * Returns the ID of this flow.
     *
     * @return the flow ID
     */
    FlowId id();

    /**
     * Returns the application id of this flow.
     *
     * @return an applicationId
     */
    ApplicationId appId();

    /**
     * Returns the flow rule priority given in natural order; higher numbers
     * mean higher priorities.
     *
     * @return flow rule priority
     */
    int priority();

    /**
     * Returns the identity of the device where this rule applies.
     *
     * @return device identifier
     */
    DeviceId deviceId();

    /**
     * Returns the traffic selector that identifies what traffic this
     * rule should apply to.
     *
     * @return traffic selector
     */
    TrafficSelector selector();

    /**
     * Returns the traffic treatment that applies to selected traffic.
     *
     * @return traffic treatment
     */
    TrafficTreatment treatment();

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
     * Returns the timeout for this flow requested by an application.
     * @return integer value of the timeout
     */
    int timeout();

}
