package org.onlab.onos.net.flow;

import static com.google.common.base.MoreObjects.toStringHelper;

import org.onlab.onos.net.DeviceId;

public class DefaultFlowEntry extends DefaultFlowRule implements FlowEntry {

    private final int priority;
    private final long created;
    private final FlowId id;

    public DefaultFlowEntry(DefaultFlowEntry entry) {
        super(entry.deviceId(), entry.selector(), entry.treatment());
        this.priority = entry.priority;
        this.created = entry.created;
        this.id = entry.id;
    }

    public DefaultFlowEntry(DeviceId deviceId, TrafficSelector selector,
            TrafficTreatment treatment, int priority) {
        super(deviceId, selector, treatment);
        this.priority = priority;
        this.created = System.currentTimeMillis();
        this.id = FlowId.valueOf(this.hashCode());
    }

    @Override
    public FlowId id() {
        return null;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public long lifeMillis() {
        return (created - System.currentTimeMillis());
    }

    @Override
    public long idleMillis() {
        return 0;
    }

    @Override
    public long packets() {
        return 0;
    }

    @Override
    public long bytes() {
        return 0;
    }

    @Override
    /*
     * currently uses the parts that definitely have a defined hashcode...
     *
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        final int prime = 31;
        int result = prime * this.deviceId().hashCode();
        result = prime * result + this.priority;
        result = prime * result + this.selector().hashCode();
        result = prime * result + this.treatment().hashCode();
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
        if (obj instanceof DefaultFlowEntry) {
            DefaultFlowEntry that = (DefaultFlowEntry) obj;
            if (!this.deviceId().equals(that.deviceId())) {
                return false;
            }
            if (!(this.priority == that.priority)) {
                return false;
            }
            return super.equals(obj);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("deviceId", deviceId())
                .add("priority", priority)
                .add("selector", selector())
                .add("treatment", treatment())
                .toString();
    }

}
