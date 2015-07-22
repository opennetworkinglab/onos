package org.onosproject.store.consistent.impl;

/**
 * Representation of a state machine update.
 */
public class StateMachineUpdate {

    /**
     * Target data structure type this update is for.
     */
    enum Target {
        /**
         * Update is for a map.
         */
        MAP,

        /**
         * Update is for a non-map data structure.
         */
        OTHER
    }

    private final String operationName;
    private final Object input;
    private final Object output;

    public StateMachineUpdate(String operationName, Object input, Object output) {
        this.operationName = operationName;
        this.input = input;
        this.output = output;
    }

    public Target target() {
        // FIXME: This check is brittle
        if (operationName.contains("mapUpdate")) {
            return Target.MAP;
        } else {
            return Target.OTHER;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T input() {
        return (T) input;
    }

    @SuppressWarnings("unchecked")
    public <T> T output() {
        return (T) output;
    }
}