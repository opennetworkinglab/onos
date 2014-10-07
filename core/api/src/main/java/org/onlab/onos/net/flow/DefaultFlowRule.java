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

    private final FlowId id;

    private final ApplicationId appId;

    private final int timeout;


    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, int priority, long flowId,
            int timeout) {
        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.timeout = timeout;
        this.created = System.currentTimeMillis();

        this.appId = ApplicationId.valueOf((int) (flowId >> 32));
        this.id = FlowId.valueOf(flowId);
    }

    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatement, int priority, ApplicationId appId,
            int timeout) {

        if (priority < FlowRule.MIN_PRIORITY) {
            throw new IllegalArgumentException("Priority cannot be less than " + MIN_PRIORITY);
        }

        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatement;
        this.appId = appId;
        this.timeout = timeout;
        this.created = System.currentTimeMillis();

        this.id = FlowId.valueOf((((long) appId().id()) << 32) | (this.hash() & 0xffffffffL));
    }

    public DefaultFlowRule(FlowRule rule) {
        this.deviceId = rule.deviceId();
        this.priority = rule.priority();
        this.selector = rule.selector();
        this.treatment = rule.treatment();
        this.appId = rule.appId();
        this.id = rule.id();
        this.timeout = rule.timeout();
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
        if (obj instanceof DefaultFlowRule) {
            DefaultFlowRule that = (DefaultFlowRule) obj;
            return Objects.equals(deviceId, that.deviceId) &&
                    Objects.equals(id, that.id) &&
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
                .toString();
    }

    @Override
    public int timeout() {
        return timeout;
    }

}
