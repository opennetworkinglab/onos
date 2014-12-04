/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.intent;

import org.onosproject.net.intent.BatchWrite.Operation;
import org.onosproject.store.Store;

import java.util.List;

/**
 * Manages inventory of end-station intents; not intended for direct use.
 */
public interface IntentStore extends Store<IntentEvent, IntentStoreDelegate> {

    /**
     * Submits a new intent into the store. If the returned event is not
     * null, the manager is expected to dispatch the event and then to kick
     * off intent compilation process. Otherwise, another node has been elected
     * to perform the compilation process and the node will learn about
     * the submittal and results of the intent compilation via the delegate
     * mechanism.
     *
     * @param intent intent to be submitted
     */
    @Deprecated
    void createIntent(Intent intent);

    /**
     * Removes the specified intent from the inventory.
     *
     * @param intentId intent identification
     */
    @Deprecated
    void removeIntent(IntentId intentId);

    /**
     * Returns the number of intents in the store.
     *
     * @return the number of intents in the store
     */
    long getIntentCount();

    /**
     * Returns a collection of all intents in the store.
     *
     * @return iterable collection of all intents
     */
    Iterable<Intent> getIntents();

    /**
     * Returns the intent with the specified identifier.
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
     */
    void setState(Intent intent, IntentState newState);

    /**
     * Sets the installable intents which resulted from compilation of the
     * specified original intent.
     *
     * @param intentId           original intent identifier
     * @param installableIntents compiled installable intents
     */
    void setInstallableIntents(IntentId intentId, List<Intent> installableIntents);

    /**
     * Returns the list of the installable events associated with the specified
     * original intent.
     *
     * @param intentId original intent identifier
     * @return compiled installable intents
     */
    List<Intent> getInstallableIntents(IntentId intentId);

    /**
     * Removes any installable intents which resulted from compilation of the
     * specified original intent.
     *
     * @param intentId original intent identifier
     */
    void removeInstalledIntents(IntentId intentId);

    /**
     * Execute writes in a batch.
     *
     * @param batch BatchWrite to execute
     * @return failed operations
     */
    List<Operation> batchWrite(BatchWrite batch);

}
