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
import org.onosproject.net.intent.IntentState;
import org.slf4j.Logger;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Represents a phase of requesting a purge of an intent.
 * <p>
 * Note: The purge will only succeed if the intent is FAILED or WITHDRAWN.
 * </p>
 */
final class PurgeRequest extends FinalIntentProcessPhase {

    private static final Logger log = getLogger(PurgeRequest.class);

    private final IntentData data;
    private final Optional<IntentData> stored;

    PurgeRequest(IntentData intentData, Optional<IntentData> stored) {
        this.data = checkNotNull(intentData);
        this.stored = checkNotNull(stored);
    }

    private boolean shouldAcceptPurge() {
        if (!stored.isPresent()) {
            log.info("Purge for intent {}, but intent is not present",
                     data.key());
            return true;
        }

        IntentData storedData = stored.get();
        if (storedData.state() == IntentState.WITHDRAWN
                || storedData.state() == IntentState.FAILED) {
            return true;
        }
        log.info("Purge for intent {} is rejected because intent state is {}",
                 data.key(), storedData.state());
        return false;
    }

    @Override
    public IntentData data() {
        if (shouldAcceptPurge()) {
            return data;
        } else {
            return stored.get();
        }
    }
}
