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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.intent.impl.phase.IntentProcessPhase.transferErrorCount;

/**
 * Represents a phase of requesting a withdraw of an intent.
 */
final class WithdrawRequest implements IntentProcessPhase {

    private static final Logger log = LoggerFactory.getLogger(WithdrawRequest.class);

    private final IntentProcessor processor;
    private final IntentData data;
    private final Optional<IntentData> stored;

    /**
     * Creates a withdraw request phase.
     *
     * @param processor  intent processor to be passed to intent process phases
     *                   generated after this phase
     * @param intentData intent data to be processed
     * @param stored     intent data stored in the store
     */
    WithdrawRequest(IntentProcessor processor, IntentData intentData, Optional<IntentData> stored) {
        this.processor = checkNotNull(processor);
        this.data = checkNotNull(intentData);
        this.stored = checkNotNull(stored);
    }

    @Override
    public Optional<IntentProcessPhase> execute() {
        //TODO perhaps we want to validate that the pending and current are the
        // same version i.e. they are the same
        // Note: this call is not just the symmetric version of submit

        transferErrorCount(data, stored);

        if (!stored.isPresent() || stored.get().installables().isEmpty()) {
            switch (data.request()) {
                case INSTALL_REQ:
                    // illegal state?
                    log.warn("{} was requested to withdraw during installation?", data.intent());
                    return Optional.of(new Failed(data));
                case WITHDRAW_REQ:
                default: //TODO "default" case should not happen
                    return Optional.of(new Withdrawn(data));
            }
        }

        return Optional.of(new Withdrawing(processor, new IntentData(data, stored.get().installables())));
    }
}
