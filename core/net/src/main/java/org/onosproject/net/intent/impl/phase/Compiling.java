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
 * Represents a phase where an intent is being compiled.
 */
final class Compiling implements IntentProcessPhase {

    private static final Logger log = LoggerFactory.getLogger(Compiling.class);

    private final IntentProcessor processor;
    private final IntentData data;

    /**
     * Creates an compiling phase.
     *
     * @param processor intent processor that does work for compiling
     * @param data      intent data containing an intent to be compiled
     */
    Compiling(IntentProcessor processor, IntentData data) {
        this.processor = checkNotNull(processor);
        this.data = checkNotNull(data);
    }

    @Override
    public Optional<IntentProcessPhase> execute() {
        try {
            data.setInstallables(processor.compile(data.intent(), null));
            return Optional.of(new Installing(processor, data));
        } catch (IntentException e) {
            log.debug("Unable to compile intent {} due to: {}", data.intent(), e);
            return Optional.of(new CompileFailed(data));
        }
    }

}
