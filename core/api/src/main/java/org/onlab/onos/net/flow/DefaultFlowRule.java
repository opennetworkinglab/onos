package org.onlab.onos.net.flow;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.onos.net.DeviceId;

public class DefaultFlowRule implements FlowRule {

    private final DeviceId deviceId;
    private final int priority;
    private final TrafficSelector selector;
    private final TrafficTreatment treatment;
    private final FlowId id;
    private final long created;
    private final long life;
    private final long packets;
    private final long bytes;
    private final FlowRuleState state;


    public DefaultFlowRule(DeviceId deviceId,
            TrafficSelector selector, TrafficTreatment treatment,
            int priority, FlowRuleState state) {
        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.state = state;
        this.life = 0;
        this.packets = 0;
        this.bytes = 0;
        this.id = FlowId.valueOf(this.hashCode());
        this.created = System.currentTimeMillis();
    }

    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, int priority, FlowRuleState state,
            long life, long packets, long bytes, Integer flowId) {
        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.state = state;

        this.id = FlowId.valueOf(flowId);

        this.life = life;
        this.packets = packets;
        this.bytes = bytes;
        this.created = System.currentTimeMillis();
    }

    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatement, int priority) {
        this(deviceId, selector, treatement, priority, FlowRuleState.CREATED);
    }

    public DefaultFlowRule(FlowRule rule, FlowRuleState state) {
        this(rule.deviceId(), rule.selector(), rule.treatment(),
                rule.priority(), state);
    }


    @Override
    public FlowId id() {
        return id;
    }

    @Override
    public int priority() {
        return priority;
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
    public long lifeMillis() {
        return life;
    }

    @Override
    public long packets() {
        return packets;
    }

    @Override
    public long bytes() {
        return bytes;
    }

    @Override
    public FlowRuleState state() {
        return this.state;
    }


    @Override
    /*
     * The priority and statistics can change on a given treatment and selector
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public int hashCode() {
        return Objects.hash(deviceId, selector, treatment);
    }

    @Override
    /*
     * The priority and statistics can change on a given treatment and selector
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FlowRule) {
            FlowRule that = (FlowRule) obj;
            return Objects.equals(deviceId, that.deviceId()) &&
                    Objects.equals(id, that.id());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("deviceId", deviceId)
                .add("priority", priority)
                .add("selector", selector)
                .add("treatment", treatment)
                .add("created", created)
                .toString();
    }

}
