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

import java.util.Optional;

/**
 * Represents a final phase of processing an intent.
 */
public abstract class FinalIntentProcessPhase implements IntentProcessPhase {

    @Override
    public final Optional<IntentProcessPhase> execute() {
        preExecute();
        return Optional.empty();
    }

    /**
     * Executes operations that must take place before the phase starts.
     */
    protected void preExecute() {}

    /**
     * Returns the IntentData object being acted on by this phase.
     *
     * @return intent data object for the phase
     */
    public abstract IntentData data();
}
