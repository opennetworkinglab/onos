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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime * this.deviceId().hashCode();
        result = prime * result + selector.hashCode();
        result = prime * result + treatment.hashCode();
        return result;
    }

    @Override
    /*
     * The priority and statistics can change on a given treatment and selector
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof FlowRule) {
            DefaultFlowRule that = (DefaultFlowRule) obj;
            if (!this.deviceId().equals(that.deviceId())) {
                return false;
            }
            if (!this.treatment().equals(that.treatment())) {
                return false;
            }
            if (!this.selector().equals(that.selector())) {
                return false;
            }
        }
        return true;
    }


}
