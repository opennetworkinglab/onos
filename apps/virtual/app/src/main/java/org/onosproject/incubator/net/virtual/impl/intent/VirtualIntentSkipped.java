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

package org.onosproject.incubator.net.virtual.impl.intent;

import org.onosproject.incubator.net.virtual.impl.intent.phase.VirtualFinalIntentProcessPhase;
import org.onosproject.net.intent.IntentData;

/**
 * Represents a phase where an intent is not compiled for a virtual network.
 * This should be used if a new version of the intent will immediately override
 * this one.
 */
public final class VirtualIntentSkipped extends VirtualFinalIntentProcessPhase {

    private static final VirtualIntentSkipped SINGLETON = new VirtualIntentSkipped();

    /**
     * Returns a shared skipped phase.
     *
     * @return skipped phase
     */
    public static VirtualIntentSkipped getPhase() {
        return SINGLETON;
    }

    // Prevent object construction; use getPhase()
    private VirtualIntentSkipped() {
    }

    @Override
    public IntentData data() {
        return null;
    }
}
