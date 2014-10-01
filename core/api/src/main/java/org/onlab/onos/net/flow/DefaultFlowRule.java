package org.onlab.onos.net.flow;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;

import org.onlab.onos.ApplicationId;
import org.onlab.onos.net.DeviceId;
import org.slf4j.Logger;

public class DefaultFlowRule implements FlowRule {

    private final Logger log = getLogger(getClass());

    private final DeviceId deviceId;
    private final int priority;
    private final TrafficSelector selector;
    private final TrafficTreatment treatment;
    private final long created;
    private final long life;
    private final long packets;
    private final long bytes;
    private final FlowRuleState state;

    private final FlowId id;

    private final ApplicationId appId;

    private boolean expired;

    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, int priority, FlowRuleState state,
            long life, long packets, long bytes, long flowId, boolean expired) {
        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.state = state;
        this.appId = ApplicationId.valueOf((int) (flowId >> 32));
        this.id = FlowId.valueOf(flowId);
        this.expired = expired;
        this.life = life;
        this.packets = packets;
        this.bytes = bytes;
        this.created = System.currentTimeMillis();
    }

    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatement, int priority, ApplicationId appId) {
        this(deviceId, selector, treatement, priority, FlowRuleState.CREATED, appId);
    }

    public DefaultFlowRule(FlowRule rule, FlowRuleState state) {
        this(rule.deviceId(), rule.selector(), rule.treatment(),
                rule.priority(), state, rule.id(), rule.appId());
    }

    private DefaultFlowRule(DeviceId deviceId,
            TrafficSelector selector, TrafficTreatment treatment,
            int priority, FlowRuleState state, ApplicationId appId) {
        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.state = state;
        this.life = 0;
        this.packets = 0;
        this.bytes = 0;
        this.appId = appId;

        this.id = FlowId.valueOf((((long) appId().id()) << 32) | (this.hash() & 0xffffffffL));
        this.created = System.currentTimeMillis();
    }

    private DefaultFlowRule(DeviceId deviceId,
            TrafficSelector selector, TrafficTreatment treatment,
            int priority, FlowRuleState state, FlowId flowId, ApplicationId appId) {
        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.state = state;
        this.life = 0;
        this.packets = 0;
        this.bytes = 0;
        this.appId = appId;
        this.id = flowId;
        this.created = System.currentTimeMillis();
    }


    @Override
    public FlowId id() {
        return id;
    }

    @Override
    public ApplicationId appId() {
        return appId;
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
        return Objects.hash(deviceId, id);
    }

    public int hash() {
        return Objects.hash(deviceId, selector, id);
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
        if (obj instanceof DefaultFlowRule) {
            DefaultFlowRule that = (DefaultFlowRule) obj;
            return Objects.equals(deviceId, that.deviceId) &&
                    Objects.equals(id, that.id);
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
                .add("state", state)
                .toString();
    }

    @Override
    public boolean expired() {
        return expired;
    }

}
