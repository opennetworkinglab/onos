/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual;

import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentStoreDelegate;
import org.onosproject.net.intent.Key;

import java.util.List;

public interface VirtualNetworkIntentStore
        extends VirtualStore<IntentEvent, IntentStoreDelegate> {

    /**
     * Returns the number of intents in the store.
     *
     * @param networkId the virtual network identifier
     * @return the number of intents in the store
     */
    long getIntentCount(NetworkId networkId);

    /**
     * Returns an iterable of all intents in the store.
     *
     * @param networkId the virtual network identifier
     * @return iterable  of all intents
     */
    Iterable<Intent> getIntents(NetworkId networkId);

    /**
     * Returns an iterable of all intent data objects in the store.
     *
     * @param networkId the virtual network identifier
     * @param localOnly should only intents for which this instance is master
     *                  be returned
     * @param olderThan specified duration in milliseconds (0 for "now")
     * @return iterable of all intent data objects
     */
    Iterable<IntentData> getIntentData(NetworkId networkId, boolean localOnly,
                                       long olderThan);

    /**
     * Returns the state of the specified intent.
     *
     * @param networkId the virtual network identifier
     * @param intentKey intent identification
     * @return current intent state
     */
    IntentState getIntentState(NetworkId networkId, Key intentKey);

    /**
     * Returns the list of the installable events associated with the specified
     * original intent.
     *
     * @param networkId the virtual network identifier
     * @param intentKey original intent identifier
     * @return compiled installable intents, or null if no installables exist
     */
    List<Intent> getInstallableIntents(NetworkId networkId, Key intentKey);

    /**
     * Writes an IntentData object to the store.
     *
     * @param networkId the virtual network identifier
     * @param newData new intent data to write
     */
    void write(NetworkId networkId, IntentData newData);

    /**
     * Writes a batch of IntentData objects to the store. A batch has no
     * semantics, this is simply a convenience API.
     *
     * @param networkId the virtual network identifier
     * @param updates collection of intent data objects to write
     */
    void batchWrite(NetworkId networkId, Iterable<IntentData> updates);

    /**
     * Returns the intent with the specified identifier.
     *
     * @param networkId the virtual network identifier
     * @param key key
     * @return intent or null if not found
     */
    Intent getIntent(NetworkId networkId, Key key);

    /**
     * Returns the intent data object associated with the specified key.
     *
     * @param networkId the virtual network identifier
     * @param key key to look up
     * @return intent data object
     */
    IntentData getIntentData(NetworkId networkId, Key key);

    /**
     * Adds a new operation, which should be persisted and delegated.
     *
     * @param networkId the virtual network identifier
     * @param intent operation
     */
    void addPending(NetworkId networkId, IntentData intent);

    /**
     * Checks to see whether the calling instance is the master for processing
     * this intent, or more specifically, the key contained in this intent.
     *
     * @param networkId the virtual network identifier
     * @param intentKey intentKey to check
     * @return true if master; false, otherwise
     */
    //TODO better name
    boolean isMaster(NetworkId networkId, Key intentKey);

    /**
     * Returns the intent requests pending processing.
     *
     * @param networkId the virtual network identifier
     * @return pending intents
     */
    Iterable<Intent> getPending(NetworkId networkId);

    /**
     * Returns the intent data objects that are pending processing.
     *
     * @param networkId the virtual network identifier
     * @return pending intent data objects
     */
    Iterable<IntentData> getPendingData(NetworkId networkId);

    /**
     * Returns the intent data object that are pending processing for a specfied
     * key.
     *
     * @param networkId the virtual network identifier
     * @param intentKey key to look up
     * @return pending intent data object
     */
    IntentData getPendingData(NetworkId networkId, Key intentKey);

    /**
     * Returns the intent data objects that are pending processing for longer
     * than the specified duration.
     *
     * @param networkId the virtual network identifier
     * @param localOnly  should only intents for which this instance is master
     *                   be returned
     * @param olderThan specified duration in milliseconds (0 for "now")
     * @return pending intent data objects
     */
    Iterable<IntentData> getPendingData(NetworkId networkId, boolean localOnly, long olderThan);

}
