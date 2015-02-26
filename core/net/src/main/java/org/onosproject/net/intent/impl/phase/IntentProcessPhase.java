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
import org.onosproject.net.intent.impl.IntentProcessor;

import java.util.Optional;

import static org.onlab.util.Tools.isNullOrEmpty;

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
        switch (data.state()) {
            case INSTALL_REQ:
                return new InstallRequest(processor, data, Optional.ofNullable(current));
            case WITHDRAW_REQ:
                if (current == null || isNullOrEmpty(current.installables())) {
                    return new Withdrawn(data);
                } else {
                    return new WithdrawRequest(processor, data, current);
                }
            case PURGE_REQ:
                return new PurgeRequest(data, current);
            default:
                // illegal state
                return new CompileFailed(data);
        }
    }

}
