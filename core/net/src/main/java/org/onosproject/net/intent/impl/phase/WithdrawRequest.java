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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a phase of requesting a withdraw of an intent.
 */
public final class WithdrawRequest implements IntentProcessPhase {

    private final IntentProcessor processor;
    private final IntentData pending;
    private final IntentData current;

    public WithdrawRequest(IntentProcessor processor, IntentData intentData, IntentData current) {
        this.processor = checkNotNull(processor);
        this.pending = checkNotNull(intentData);
        this.current = checkNotNull(current);
    }

    @Override
    public Optional<IntentProcessPhase> execute() {
        //TODO perhaps we want to validate that the pending and current are the
        // same version i.e. they are the same
        // Note: this call is not just the symmetric version of submit
        return Optional.of(new WithdrawCoordinating(processor, pending, current));
    }
}
