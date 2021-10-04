/*
 * Copyright 2014-present Open Networking Foundation
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
import org.onosproject.core.ApplicationId;
import org.onosproject.event.ListenerService;

import java.util.List;

/**
 * Service for application submitting or withdrawing their intents.
 */
@Beta
public interface IntentService
    extends ListenerService<IntentEvent, IntentListener> {

    /**
     * Submits an intent into the system.
     * <p>
     * This is an asynchronous request meaning that any compiling or
     * installation activities may be done at later time.
     * </p>
     * @param intent intent to be submitted
     */
    void submit(Intent intent);

    /**
     * Withdraws an intent from the system.
     * <p>
     * This is an asynchronous request meaning that the environment may be
     * affected at later time.
     * </p>
     * @param intent intent to be withdrawn
     */
    void withdraw(Intent intent);

    /**
     * Purges a specific intent from the system if it is <b>FAILED</b> or
     * <b>WITHDRAWN</b>. Otherwise, the intent remains in its current state.
     *
     * @param intent intent to purge
     */
    void purge(Intent intent);

    /**
     * Fetches an intent based on its key.
     *
     * @param key key of the intent
     * @return intent object if the key is found, null otherwise
     */
    Intent getIntent(Key key);

    /**
     * Returns an iterable of intents currently in the system.
     *
     * @return set of intents
     */
    Iterable<Intent> getIntents();

    /**
     * Returns an iterable of all intents with this application ID.
     *
     * @param id the application ID to look up
     * @return collection of intents
     */
    Iterable<Intent> getIntentsByAppId(ApplicationId id);

    /**
     * Adds an intent data object to the pending map for processing.
     * <p>
     * This method is intended to only be called by core components, not
     * applications.
     * </p>
     *
     * @param intentData intent data to be added to pending map
     */
    void addPending(IntentData intentData);

    /**
     * Returns an iterable of intent data objects currently in the system.
     *
     * @return set of intent data objects
     */
    Iterable<IntentData> getIntentData();

    /**
     * Returns the number of intents currently in the system.
     *
     * @return number of intents
     */
    long getIntentCount();

    /**
     * Retrieves the state of an intent by its identifier.
     *
     * @param intentKey intent identifier
     * @return the intent state or null if one with the given identifier is not
     * found
     */
    IntentState getIntentState(Key intentKey);

    /**
     * Returns the list of the installable events associated with the specified
     * top-level intent.
     *
     * @param intentKey top-level intent identifier
     * @return compiled installable intents
     */
    List<Intent> getInstallableIntents(Key intentKey);

    /**
     * Signifies whether the local node is responsible for processing the given
     * intent key.
     *
     * @param intentKey intent key to check
     * @return true if the local node is responsible for the intent key,
     * otherwise false
     */
    boolean isLocal(Key intentKey);

    /**
     * Returns the list of intent requests pending processing.
     *
     * @return intents pending processing
     */
    Iterable<Intent> getPending();

}
