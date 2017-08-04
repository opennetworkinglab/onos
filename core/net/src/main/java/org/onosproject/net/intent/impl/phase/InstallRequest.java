/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.intent.impl.phase;

import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.impl.IntentProcessor;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.intent.impl.phase.IntentProcessPhase.transferErrorCount;

/**
 * Represents a phase where intent installation has been requested.
 */
final class InstallRequest implements IntentProcessPhase {

    private final IntentProcessor processor;
    private final IntentData data;
    private final Optional<IntentData> stored;

    /**
     * Creates an install request phase.
     *
     * @param processor  intent processor to be passed to intent process phases
     *                   generated after this phase
     * @param intentData intent data to be processed
     * @param stored     intent data stored in the store
     */
    InstallRequest(IntentProcessor processor, IntentData intentData, Optional<IntentData> stored) {
        this.processor = checkNotNull(processor);
        this.data = checkNotNull(intentData);
        this.stored = checkNotNull(stored);
    }

    @Override
    public Optional<IntentProcessPhase> execute() {
        transferErrorCount(data, stored);

        return Optional.of(new Compiling(processor, data, stored));
    }
}
