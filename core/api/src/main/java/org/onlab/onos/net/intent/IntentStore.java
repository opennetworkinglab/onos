package org.onlab.onos.net.intent;

import org.onlab.onos.store.Store;

import java.util.List;

/**
 * Manages inventory of end-station intents; not intended for direct use.
 */
public interface IntentStore extends Store<IntentEvent, IntentStoreDelegate> {

    /**
     * Creates a new intent.
     *
     * @param intent intent
     * @return appropriate event or null if no change resulted
     */
    IntentEvent createIntent(Intent intent);

    /**
     * Removes the specified intent from the inventory.
     *
     * @param intentId intent identification
     * @return removed state transition event or null if intent was not found
     */
    IntentEvent removeIntent(IntentId intentId);

    /**
     * Returns the number of intents in the store.
     */
    long getIntentCount();

    /**
     * Returns a collection of all intents in the store.
     *
     * @return iterable collection of all intents
     */
    Iterable<Intent> getIntents();

    /**
     * Returns the intent with the specified identifer.
     *
     * @param intentId intent identification
     * @return intent or null if not found
     */
    Intent getIntent(IntentId intentId);

    /**
     * Returns the state of the specified intent.
     *
     * @param intentId intent identification
     * @return current intent state
     */
    IntentState getIntentState(IntentId intentId);

    /**
     * Sets the state of the specified intent to the new state.
     *
     * @param intent   intent whose state is to be changed
     * @param newState new state
     * @return state transition event
     */
    IntentEvent setState(Intent intent, IntentState newState);

    /**
     * Adds the installable intents which resulted from compilation of the
     * specified original intent.
     *
     * @param intentId           original intent identifier
     * @param installableIntents compiled installable intents
     * @return compiled state transition event
     */
    IntentEvent addInstallableIntents(IntentId intentId,
                                      List<InstallableIntent> installableIntents);

    /**
     * Returns the list of the installable events associated with the specified
     * original intent.
     *
     * @param intentId original intent identifier
     * @return compiled installable intents
     */
    List<InstallableIntent> getInstallableIntents(IntentId intentId);

    // TODO: this should be triggered from with the store as a result of removeIntent call

    /**
     * Removes any installable intents which resulted from compilation of the
     * specified original intent.
     *
     * @param intentId original intent identifier
     * @return compiled state transition event
     */
    void removeInstalledIntents(IntentId intentId);

}
