package org.onlab.onos.net.intent;

import java.util.List;

import org.onlab.onos.store.Store;

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
     * @return remove event or null if intent was not found
     */
    IntentEvent removeIntent(IntentId intent);

    /**
     * Returns the number of intents in the store.
     *
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

    IntentState getIntentState(IntentId id);

    /**
     * Sets the state of the specified intent to the new state.
     *
     * @param intent intent whose state is to be changed
     * @param newState new state
     */
    IntentEvent setState(Intent intent, IntentState newState);

    IntentEvent addInstallableIntents(IntentId intentId, List<InstallableIntent> result);

    List<InstallableIntent> getInstallableIntents(IntentId intentId);

    void removeInstalledIntents(IntentId intentId);
}
