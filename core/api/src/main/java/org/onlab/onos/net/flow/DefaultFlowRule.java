package org.onlab.onos.net.flow;

import static com.google.common.base.MoreObjects.toStringHelper;

import org.onlab.onos.net.DeviceId;

public class DefaultFlowRule implements FlowRule {

    private final DeviceId deviceId;
    private final int priority;
    private final TrafficSelector selector;
    private final TrafficTreatment treatment;
    private final FlowId id;
    private final long created;
    private long life;
    private long idle;
    private long packets;
    private long bytes;


    public DefaultFlowRule(DeviceId deviceId,
            TrafficSelector selector, TrafficTreatment treatment, int priority) {
        this.deviceId = deviceId;
        this.priority = priority;
        this.selector = selector;
        this.treatment = treatment;
        this.life = 0;
        this.idle = 0;
        this.packets = 0;
        this.bytes = 0;
        this.id = FlowId.valueOf(this.hashCode());
        this.created = System.currentTimeMillis();
    }

    // TODO: Decide whether to take the flowId from the underlying flowentry.
    public DefaultFlowRule(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, int priority,
            long life, long idle, long packets, long bytes) {
        this(deviceId, selector, treatment, priority);
        this.life = life;
        this.idle = idle;
        this.packets = packets;
        this.bytes = bytes;
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
    public long idleMillis() {
        return idle;
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
    /*
     * The priority and statistics can change on a given treatment and selector
     *
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
            return true;
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
