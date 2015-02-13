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
package org.onosproject.net.intent.impl.phase;

import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentState;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.intent.IntentState.WITHDRAWING;

/**
 * Represents a phase where an intent has been withdrawn.
 */
public final class Withdrawn extends FinalIntentProcessPhase {

    private final IntentData intentData;

    public Withdrawn(IntentData intentData) {
        this(intentData, WITHDRAWING);
    }

    public Withdrawn(IntentData intentData, IntentState newState) {
        this.intentData = checkNotNull(intentData);
        this.intentData.setState(newState);
    }

    @Override
    public IntentData data() {
        return intentData;
    }
}
