package org.onlab.onos.net.intent;


import java.util.List;
import java.util.concurrent.Future;

/**
 * Service for application submitting or withdrawing their intents.
 */
public interface IntentService {
    /**
     * Submits an intent into the system.
     * <p/>
     * This is an asynchronous request meaning that any compiling or
     * installation activities may be done at later time.
     *
     * @param intent intent to be submitted
     */
    void submit(Intent intent);

    /**
     * Withdraws an intent from the system.
     * <p/>
     * This is an asynchronous request meaning that the environment may be
     * affected at later time.
     *
     * @param intent intent to be withdrawn
     */
    void withdraw(Intent intent);

    // void replace(IntentId oldIntentId, Intent newIntent);

    /**
     * Submits a batch of submit &amp; withdraw operations. Such a batch is
     * assumed to be processed together.
     * <p/>
     * This is an asynchronous request meaning that the environment may be
     * affected at later time.
     *
     * @param operations batch of intent operations
     */
    Future<IntentOperations> execute(IntentOperations operations);

    /**
     * Returns an iterable of intents currently in the system.
     *
     * @return set of intents
     */
    Iterable<Intent> getIntents();

    /**
     * Returns the number of intents currently in the system.
     *
     * @return number of intents
     */
    long getIntentCount();

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
     * @return the intent state or null if one with the given identifier is not
     * found
     */
    IntentState getIntentState(IntentId id);

    /**
     * Returns the list of the installable events associated with the specified
     * top-level intent.
     *
     * @param intentId top-level intent identifier
     * @return compiled installable intents
     */
    List<Intent> getInstallableIntents(IntentId intentId);

    /**
     * Adds the specified listener for intent events.
     *
     * @param listener listener to be added
     */
    void addListener(IntentListener listener);

    /**
     * Removes the specified listener for intent events.
     *
     * @param listener listener to be removed
     */
    void removeListener(IntentListener listener);
}
