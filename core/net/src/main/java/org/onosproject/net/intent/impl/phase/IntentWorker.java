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
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.isNullOrEmpty;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;

/**
 * Worker to process a submitted intent. {@link #call()} method generates
 */
public final class IntentWorker implements Callable<FinalIntentProcessPhase> {

    private final IntentProcessor processor;
    private final IntentData data;
    private final IntentData current;

    /**
     * Create an instance with the specified arguments.
     *
     * @param processor intent processor to be passed to intent process phases
     *                  generated while this instance is working
     * @param data intent data to be processed
     * @param current intent date that is stored in the store
     */
    public IntentWorker(IntentProcessor processor, IntentData data, IntentData current) {
        this.processor = checkNotNull(processor);
        this.data = checkNotNull(data);
        this.current = current;
    }

    @Override
    public FinalIntentProcessPhase call() throws Exception {
        IntentProcessPhase update = createInitialPhase();
        Optional<IntentProcessPhase> currentPhase = Optional.of(update);
        IntentProcessPhase previousPhase = update;

        while (currentPhase.isPresent()) {
            previousPhase = currentPhase.get();
            currentPhase = previousPhase.execute();
        }
        return (FinalIntentProcessPhase) previousPhase;
    }

    /**
     * Create a starting intent process phase according to intent data this class holds.
     *
     * @return starting intent process phase
     */
    private IntentProcessPhase createInitialPhase() {
        switch (data.state()) {
            case INSTALL_REQ:
                return new InstallRequest(processor, data, Optional.ofNullable(current));
            case WITHDRAW_REQ:
                if (current == null || isNullOrEmpty(current.installables())) {
                    return new Withdrawn(data, WITHDRAWN);
                } else {
                    return new WithdrawRequest(processor, data, current);
                }
            default:
                // illegal state
                return new CompilingFailed(data);
        }
    }
}
