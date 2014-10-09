package org.onlab.onos.net.intent;

import org.onlab.onos.event.AbstractEvent;

/**
 * A class to represent an intent related event.
 */
public class IntentEvent extends AbstractEvent<IntentEvent.Type, Intent> {

    public enum Type {
        /**
         * Signifies that a new intent has been submitted to the system.
         */
        SUBMITTED,

        /**
         * Signifies that an intent has been successfully installed.
         */
        INSTALLED,

        /**
         * Signifies that an intent has failed compilation or installation.
         */
        FAILED,

        /**
         * Signifies that an intent has been withdrawn from the system.
         */
        WITHDRAWN
    }

    /**
     * Creates an event of a given type and for the specified intent and the
     * current time.
     *
     * @param type   event type
     * @param intent subject intent
     * @param time   time the event created in milliseconds since start of epoch
     */
    public IntentEvent(Type type, Intent intent, long time) {
        super(type, intent, time);
    }

    /**
     * Creates an event of a given type and for the specified intent and the
     * current time.
     *
     * @param type   event type
     * @param intent subject intent
     */
    public IntentEvent(Type type, Intent intent) {
        super(type, intent);
    }

}
