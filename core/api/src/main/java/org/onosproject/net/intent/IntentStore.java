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
    @Deprecated
    default Intent getIntent(IntentId intentId) {
        throw new UnsupportedOperationException("deprecated");
    }

    /**
     * Returns the state of the specified intent.
     *
     * @param intentId intent identification
     * @return current intent state
     */
    @Deprecated
    default IntentState getIntentState(IntentId intentId) {
        throw new UnsupportedOperationException("deprecated");
    }

    /**
     * Returns the list of the installable events associated with the specified
     * original intent.
     *
     * @param intentId original intent identifier
     * @return compiled installable intents
     */
    @Deprecated
    default List<Intent> getInstallableIntents(IntentId intentId) {
        throw new UnsupportedOperationException("deprecated");
    }

    /**
     * Execute writes in a batch.
     * If the specified BatchWrite is empty, write will not be executed.
     *
     * @param batch BatchWrite to execute
     * @return failed operations
     */
    @Deprecated
    List<Operation> batchWrite(BatchWrite batch);
    default void write(IntentData newData) {}
    default void batchWrite(Iterable<IntentData> updates) {}

    /**
     * Returns the intent with the specified identifier.
     *
     * @param key key
     * @return intent or null if not found
     */
    default Intent getIntent(String key) { //FIXME remove when impl.
        return null;
    }

    /**
     * Returns the intent data object associated with the specified key.
     *
     * @param key key to look up
     * @return intent data object
     */
    default IntentData getIntentData(String key) { //FIXME remove when impl.
        return null;
    }

    /**
     * Adds a new operation, which should be persisted and delegated.
     *
     * @param intent operation
     */
    default void addPending(IntentData intent) {} //FIXME remove when impl.

    /**
     * Checks to see whether the calling instance is the master for processing
     * this intent, or more specifically, the key contained in this intent.
     *
     * @param intent intent to check
     * @return true if master; false, otherwise
     */
    //TODO better name
    default boolean isMaster(Intent intent) { //FIXME remove default when impl.
        return true;
    }
}
