package org.onlab.onos.net.intent;

import java.util.Set;

/**
 * Service for application submitting or withdrawing their intents.
 */
public interface IntentService {
    /**
     * Submits an intent into the system.
     *
     * This is an asynchronous request meaning that any compiling
     * or installation activities may be done at later time.
     *
     * @param intent intent to be submitted
     */
    void submit(Intent intent);

    /**
     * Withdraws an intent from the system.
     *
     * This is an asynchronous request meaning that the environment
     * may be affected at later time.
     *
     * @param intent intent to be withdrawn
     */
    void withdraw(Intent intent);

    /**
     * Submits a batch of submit &amp; withdraw operations. Such a batch is
     * assumed to be processed together.
     *
     * This is an asynchronous request meaning that the environment
     * may be affected at later time.
     *
     * @param operations batch of intent operations
     */
    void execute(IntentOperations operations);

    /**
     * Returns immutable set of intents currently in the system.
     *
     * @return set of intents
     */
    Set<Intent> getIntents();

    /**
     * Retrieves the intent specified by its identifier.
     *
     * @param id intent identifier
     * @return the intent or null if one with the given identifier is not found
     */
    Intent getIntent(IntentId id);

    /**
     * Retrieves the state of an intent by its identifier.
     *
     * @param id intent identifier
     * @return the intent state or null if one with the given identifier is not found
     */
    IntentState getIntentState(IntentId id);

    /**
     * Adds the specified listener for intent events.
     *
     * @param listener listener to be added
     */
    void addListener(IntentEventListener listener);

    /**
     * Removes the specified listener for intent events.
     *
     * @param listener listener to be removed
     */
    void removeListener(IntentEventListener listener);
}
