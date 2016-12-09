/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.annotations.Beta;
import org.onosproject.store.Store;

import java.util.List;

/**
 * Manages inventory of end-station intents; not intended for direct use.
 */
@Beta
public interface IntentStore extends Store<IntentEvent, IntentStoreDelegate> {

    /**
     * Returns the number of intents in the store.
     *
     * @return the number of intents in the store
     */
    long getIntentCount();

    /**
     * Returns an iterable of all intents in the store.
     *
     * @return iterable  of all intents
     */
    Iterable<Intent> getIntents();


    /**
     * Returns an iterable of all intent data objects in the store.
     *
     * @param localOnly should only intents for which this instance is master
     *                  be returned
     * @param olderThan specified duration in milliseconds (0 for "now")
     * @return iterable of all intent data objects
     */
    Iterable<IntentData> getIntentData(boolean localOnly, long olderThan);

    /**
     * Returns the state of the specified intent.
     *
     * @param intentKey intent identification
     * @return current intent state
     */
    IntentState getIntentState(Key intentKey);

    /**
     * Returns the list of the installable events associated with the specified
     * original intent.
     *
     * @param intentKey original intent identifier
     * @return compiled installable intents, or null if no installables exist
     */
    List<Intent> getInstallableIntents(Key intentKey);

    /**
     * Writes an IntentData object to the store.
     *
     * @param newData new intent data to write
     */
    void write(IntentData newData);

    /**
     * Writes a batch of IntentData objects to the store. A batch has no
     * semantics, this is simply a convenience API.
     *
     * @param updates collection of intent data objects to write
     */
    void batchWrite(Iterable<IntentData> updates);

    /**
     * Returns the intent with the specified identifier.
     *
     * @param key key
     * @return intent or null if not found
     */
    Intent getIntent(Key key);

    /**
     * Returns the intent data object associated with the specified key.
     *
     * @param key key to look up
     * @return intent data object
     */
    IntentData getIntentData(Key key);

    /**
     * Adds a new operation, which should be persisted and delegated.
     *
     * @param intent operation
     */
    void addPending(IntentData intent);

    /**
     * Checks to see whether the calling instance is the master for processing
     * this intent, or more specifically, the key contained in this intent.
     *
     * @param intentKey intentKey to check
     * @return true if master; false, otherwise
     */
    //TODO better name
    boolean isMaster(Key intentKey);

    /**
     * Returns the intent requests pending processing.
     *
     * @return pending intents
     */
    Iterable<Intent> getPending();

    /**
     * Returns the intent data objects that are pending processing.
     *
     * @return pending intent data objects
     */
    Iterable<IntentData> getPendingData();

    /**
     * Returns the intent data object that are pending processing for a specfied
     * key.
     *
     * @param intentKey key to look up
     * @return pending intent data object
     */
    IntentData getPendingData(Key intentKey);

    /**
     * Returns the intent data objects that are pending processing for longer
     * than the specified duration.
     *
     * @param localOnly  should only intents for which this instance is master
     *                   be returned
     * @param olderThan specified duration in milliseconds (0 for "now")
     * @return pending intent data objects
     */
    Iterable<IntentData> getPendingData(boolean localOnly, long olderThan);
}
