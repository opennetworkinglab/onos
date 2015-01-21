/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.intent.impl;

import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentState;
import org.onosproject.store.Timestamp;

/**
 * Information published by GossipIntentStore to notify peers of an intent
 * creation or state update event.
 */
public class InternalIntentEvent {

    private final IntentId intentId;
    private final Intent intent;
    private final IntentState state;
    private final Timestamp timestamp;

    public InternalIntentEvent(IntentId intentId, Intent intent, IntentState state,
                               Timestamp timestamp) {
        this.intentId = intentId;
        this.intent = intent;
        this.state = state;
        this.timestamp = timestamp;
    }

    public IntentId intentId() {
        return intentId;
    }

    public Intent intent() {
        return intent;
    }

    public IntentState state() {
        return state;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    // Needed for serialization.
    @SuppressWarnings("unused")
    private InternalIntentEvent() {
        intentId = null;
        intent = null;
        state = null;
        timestamp = null;
    }
}
