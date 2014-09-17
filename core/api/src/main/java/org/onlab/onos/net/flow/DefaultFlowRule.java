package org.onlab.onos.net.flow;

import org.onlab.onos.net.DeviceId;

public class DefaultFlowRule implements FlowRule {

    private final TrafficSelector selector;
    private final TrafficTreatment treatment;
    private final DeviceId deviceId;

    public DefaultFlowRule(DeviceId deviceId,
            TrafficSelector selector, TrafficTreatment treatment) {
        this.treatment = treatment;
        this.selector = selector;
        this.deviceId = deviceId;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public TrafficSelector selector() {
        return selector;
    }

    @Override
    public TrafficTreatment treatment() {
        return treatment;
    }

}
