package org.onlab.onos.net.flow;

import org.onlab.onos.event.AbstractEvent;

/**
 * Describes flow rule event.
 */
public class FlowRuleEvent extends AbstractEvent<FlowRuleEvent.Type, FlowRule> {

    /**
     * Type of flow rule events.
     */
    public enum Type {
        /**
         * Signifies that a new flow rule has been detected.
         */
        RULE_ADDED,

        /**
         * Signifies that a flow rule has been removed.
         */
        RULE_REMOVED,

        /**
         * Signifies that a rule has been updated.
         */
        RULE_UPDATED
    }

    /**
     * Creates an event of a given type and for the specified flow rule and the
     * current time.
     *
     * @param type     flow rule event type
     * @param flowRule event flow rule subject
     */
    public FlowRuleEvent(Type type, FlowRule flowRule) {
        super(type, flowRule);
    }

    /**
     * Creates an event of a given type and for the specified flow rule and time.
     *
     * @param type     flow rule event type
     * @param flowRule event flow rule subject
     * @param time     occurrence time
     */
    public FlowRuleEvent(Type type, FlowRule flowRule, long time) {
        super(type, flowRule, time);
    }

}
