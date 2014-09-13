package org.onlab.onos.net.flow;

import org.onlab.onos.net.DeviceId;

/**
 * Represents a generalized match &amp; action pair to be applied to
 * an infrastucture device.
 */
public interface FlowRule {

    //TODO: build cookie value

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

}
