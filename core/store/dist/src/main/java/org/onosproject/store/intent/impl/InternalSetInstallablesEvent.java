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
import org.onosproject.store.Timestamp;

import java.util.List;

/**
 * Information published by GossipIntentStore to notify peers of an intent
 * set installables event.
 */
public class InternalSetInstallablesEvent {

    private final IntentId intentId;
    private final List<Intent> installables;
    private final Timestamp timestamp;

    public InternalSetInstallablesEvent(IntentId intentId,
                                        List<Intent> installables,
                                        Timestamp timestamp) {
        this.intentId = intentId;
        this.installables = installables;
        this.timestamp = timestamp;
    }

    public IntentId intentId() {
        return intentId;
    }

    public List<Intent> installables() {
        return installables;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    // Needed for serialization.
    @SuppressWarnings("unused")
    private InternalSetInstallablesEvent() {
        intentId = null;
        installables = null;
        timestamp = null;
    }
}
