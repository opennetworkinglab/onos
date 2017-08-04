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

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a phase of processing an intent.
 */
public interface IntentProcessPhase {

    /**
     * Execute the procedure represented by the instance
     * and generates the next update instance.
     *
     * @return next update
     */
    Optional<IntentProcessPhase> execute();

    /**
     * Create a starting intent process phase according to intent data this class holds.
     *
     * @param processor intent processor to be passed to intent process phases
     *                  generated while this instance is working
     * @param data intent data to be processed
     * @param current intent date that is stored in the store
     * @return starting intent process phase
     */
    static IntentProcessPhase newInitialPhase(IntentProcessor processor,
                                              IntentData data, IntentData current) {
        switch (data.request()) {
            case INSTALL_REQ:
                return new InstallRequest(processor, data, Optional.ofNullable(current));
            case WITHDRAW_REQ:
                return new WithdrawRequest(processor, data, Optional.ofNullable(current));
            case PURGE_REQ:
                return new PurgeRequest(data, Optional.ofNullable(current));
            default:
                // illegal state
                return new Failed(data);
        }
    }

    static FinalIntentProcessPhase process(IntentProcessPhase initial) {
        Optional<IntentProcessPhase> currentPhase = Optional.of(initial);
        IntentProcessPhase previousPhase = initial;

        while (currentPhase.isPresent()) {
            previousPhase = currentPhase.get();
            currentPhase = previousPhase.execute();
        }
        return (FinalIntentProcessPhase) previousPhase;
    }

    static void transferErrorCount(IntentData data, Optional<IntentData> stored) {
        stored.ifPresent(storedData -> {
            if (Objects.equals(data.intent(), storedData.intent()) &&
                    Objects.equals(data.request(), storedData.request())) {
                data.setErrorCount(storedData.errorCount());
            } else {
                data.setErrorCount(0);
            }
        });
    }
}
