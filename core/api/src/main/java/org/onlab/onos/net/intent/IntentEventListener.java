package org.onlab.onos.net.intent;

/**
 * Listener for {@link IntentEvent intent events}.
 */
public interface IntentEventListener {
    /**
     * Processes the specified intent event.
     *
     * @param event the event to process
     */
    void event(IntentEvent event);
}
