/*
 * Copyright 2015-present Open Networking Laboratory
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
import static org.onosproject.net.intent.IntentState.INSTALLING;

/**
 * Represents a phase where an intent is being installed.
 */
class Installing extends FinalIntentProcessPhase {

    private final IntentProcessor processor;
    private final IntentData data;
    private final Optional<IntentData> stored;

    /**
     * Create an installing phase.
     *
     * @param processor intent processor that does work for installing
     * @param data      intent data containing an intent to be installed
     * @param stored    intent data already stored
     */
    Installing(IntentProcessor processor, IntentData data, Optional<IntentData> stored) {
        this.processor = checkNotNull(processor);
        this.data = checkNotNull(data);
        this.stored = checkNotNull(stored);
        this.data.setState(INSTALLING);
    }

    @Override
    public void preExecute() {
        processor.apply(stored, Optional.of(data));
    }

    @Override
    public IntentData data() {
        return data;
    }
}
