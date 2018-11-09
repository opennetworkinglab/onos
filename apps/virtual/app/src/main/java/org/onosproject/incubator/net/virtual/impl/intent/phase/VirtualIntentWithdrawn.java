/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.impl.intent.phase;

import org.onosproject.net.intent.IntentData;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;

/**
 * Represents a phase where an intent has been withdrawn for a virtual network.
 */
final class VirtualIntentWithdrawn extends VirtualFinalIntentProcessPhase {

    private final IntentData data;

    /**
     * Create a withdrawn phase.
     *
     * @param data intent data containing an intent to be withdrawn
     */
    VirtualIntentWithdrawn(IntentData data) {
        this.data = IntentData.nextState(checkNotNull(data), WITHDRAWN);
    }

    @Override
    public IntentData data() {
        return data;
    }
}
