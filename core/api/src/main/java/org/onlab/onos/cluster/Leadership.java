package org.onlab.onos.cluster;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Abstract leadership concept.
 */
public class Leadership {

    private final String topic;
    private final ControllerNode leader;
    private final long epoch;

    public Leadership(String topic, ControllerNode leader, long epoch) {
        this.topic = topic;
        this.leader = leader;
        this.epoch = epoch;
    }

    /**
     * The topic for which this leadership applies.
     * @return leadership topic.
     */
    public String topic() {
        return topic;
    }

    /**
     * The leader for this topic.
     * @return leader node.
     */
    public ControllerNode leader() {
        return leader;
    }

    /**
     * The epoch when the leadership was assumed.
     * @return leadership epoch
     */
    public long epoch() {
        return epoch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, leader, epoch);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Leadership) {
            final Leadership other = (Leadership) obj;
            return Objects.equals(this.topic, other.topic) &&
                    Objects.equals(this.leader, other.leader) &&
                    Objects.equals(this.epoch, other.epoch);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
            .add("topic", topic)
            .add("leader", leader)
            .add("epoch", epoch)
            .toString();
    }
}
