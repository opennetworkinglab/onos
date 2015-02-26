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
import org.onosproject.net.intent.IntentException;
import org.onosproject.net.intent.impl.IntentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a phase to replace an intent.
 */
final class Replacing implements IntentProcessPhase {

    private static final Logger log = LoggerFactory.getLogger(Replacing.class);

    private final IntentProcessor processor;
    private final IntentData data;
    private final IntentData stored;

    /**
     * Creates a replacing phase.
     *
     * @param processor intent processor that does work for replacing
     * @param data      intent data containing an intent to be replaced
     * @param stored    intent data stored in the store
     */
    Replacing(IntentProcessor processor, IntentData data, IntentData stored) {
        this.processor = checkNotNull(processor);
        this.data = checkNotNull(data);
        this.stored = checkNotNull(stored);
    }

    @Override
    public Optional<IntentProcessPhase> execute() {
        try {
            processor.uninstall(stored);
            return Optional.of(new Installing(processor, data));
        } catch (IntentException e) {
            log.warn("Unable to generate a FlowRuleOperations from intent {} due to:", data.intent().id(), e);
            return Optional.of(new ReplaceFailed(data));
        }
    }
}
