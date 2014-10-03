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

    private final int timeout;

    /**
     * Creates a flow rule given the following paremeters.
     * @param deviceId the device where the rule should be installed
     * @param selector the traffic selection
     * @param treatment how the seleted traffic should be handled
     * @param priority the rule priority cannot be less than FlowRule.MIN_PRIORITY
     * @param state the state in which the rule is
     * @param life how long it has existed for (ms)
     * @param packets number of packets it has seen
     * @param bytes number of bytes  it has seen
     * @param flowId the identifier
     * @param timeout the rule's timeout (idle) not to exceed
     *  FlowRule.MAX_TIMEOUT of idle time
     */
    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, int priority, FlowRuleState state,
            long life, long packets, long bytes, long flowId,
            int timeout) {
        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.state = state;
        this.appId = ApplicationId.valueOf((int) (flowId >> 32));
        this.id = FlowId.valueOf(flowId);
        this.life = life;
        this.packets = packets;
        this.bytes = bytes;
        this.created = System.currentTimeMillis();
        this.timeout = timeout;
    }

    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatement, int priority, ApplicationId appId,
            int timeout) {
        this(deviceId, selector, treatement, priority,
                FlowRuleState.CREATED, appId, timeout);
    }

    public DefaultFlowRule(FlowRule rule, FlowRuleState state) {
        this(rule.deviceId(), rule.selector(), rule.treatment(),
                rule.priority(), state, rule.id(), rule.appId(),
                rule.timeout());
    }

    private DefaultFlowRule(DeviceId deviceId,
            TrafficSelector selector, TrafficTreatment treatment,
            int priority, FlowRuleState state, ApplicationId appId,
            int timeout) {
        if (priority < MIN_PRIORITY) {
            throw new IllegalArgumentException("Priority cannot be less than " + MIN_PRIORITY);
        }
        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.state = state;
        this.life = 0;
        this.packets = 0;
        this.bytes = 0;
        this.appId = appId;

        this.timeout = timeout;

        this.id = FlowId.valueOf((((long) appId().id()) << 32) | (this.hash() & 0xffffffffL));
        this.created = System.currentTimeMillis();
    }

    private DefaultFlowRule(DeviceId deviceId,
            TrafficSelector selector, TrafficTreatment treatment,
            int priority, FlowRuleState state, FlowId flowId, ApplicationId appId,
            int timeout) {
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
        this.timeout = timeout;
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
    public long life() {
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
        return Objects.hash(deviceId, selector, priority);
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
                    //Objects.equals(id, that.id) &&
                    Objects.equals(priority, that.priority) &&
                    Objects.equals(selector, that.selector);

        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("deviceId", deviceId)
                .add("priority", priority)
                .add("selector", selector.criteria())
                .add("treatment", treatment == null ? "N/A" : treatment.instructions())
                .add("created", created)
                .add("state", state)
                .toString();
    }

    @Override
    public int timeout() {
        return timeout > MAX_TIMEOUT ? MAX_TIMEOUT : this.timeout;
    }

}
