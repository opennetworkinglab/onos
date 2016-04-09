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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.intent.IntentState.CORRUPT;

/**
 * A class representing errors removing or installing intents.
 */
public class Corrupt extends FinalIntentProcessPhase {

    private final IntentData data;

    /**
     * Create an instance with the specified data.
     *
     * @param data intentData
     */
    Corrupt(IntentData data) {
        this.data = checkNotNull(data);
        this.data.setState(CORRUPT);
    }

    @Override
    public IntentData data() {
        return data;
    }
}
