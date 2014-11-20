package org.onlab.onos.cluster;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Abstract leadership concept.
 */
public class Leadership {

    private final String topic;
    private final ControllerNode leader;
    private final long term;

    public Leadership(String topic, ControllerNode leader, long term) {
        this.topic = topic;
        this.leader = leader;
        this.term = term;
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
     * The term number associated with this leadership.
     * @return leadership term
     */
    public long term() {
        return term;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, leader, term);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
            .add("topic", topic)
            .add("leader", leader)
            .add("term", term)
            .toString();
    }
}