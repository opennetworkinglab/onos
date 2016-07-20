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
import static org.onosproject.net.intent.IntentState.WITHDRAWING;

/**
 * Represents a phase where an intent is withdrawing.
 */
class Withdrawing extends FinalIntentProcessPhase {

    private final IntentProcessor processor;
    private final IntentData data;

    /**
     * Creates a withdrawing phase.
     *
     * @param processor intent processor that does work for withdrawing
     * @param data      intent data containing an intent to be withdrawn
     */
    Withdrawing(IntentProcessor processor, IntentData data) {
        this.processor = checkNotNull(processor);
        this.data = checkNotNull(data);
        this.data.setState(WITHDRAWING);
    }

    @Override
    protected void preExecute() {
        processor.apply(Optional.of(data), Optional.empty());
    }

    @Override
    public IntentData data() {
        return data;
    }
}
