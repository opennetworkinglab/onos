package org.onlab.onos.net.flow;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.intent.BatchOperationTarget;

/**
 * Represents a generalized match &amp; action pair to be applied to
 * an infrastucture device.
 */
public interface FlowRule extends BatchOperationTarget {

    static final int MAX_TIMEOUT = 60;
    static final int MIN_PRIORITY = 0;

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
    short appId();

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
     * Returns the timeout for this flow requested by an application.
     * @return integer value of the timeout
     */
    int timeout();

}
