/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.net.virtual.impl.intent.phase;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.impl.intent.VirtualIntentProcessor;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a phase where an intent is being compiled or recompiled
 * for virtual networks.
 */
public class VirtualIntentCompiling implements VirtualIntentProcessPhase {
    private static final Logger log = LoggerFactory.getLogger(VirtualIntentCompiling.class);

    private final NetworkId networkId;
    private final VirtualIntentProcessor processor;
    private final IntentData data;
    private final Optional<IntentData> stored;

    /**
     * Creates a intent recompiling phase.
     *
     * @param networkId virtual network identifier
     * @param processor intent processor that does work for recompiling
     * @param data      intent data containing an intent to be recompiled
     * @param stored    intent data stored in the store
     */
    VirtualIntentCompiling(NetworkId networkId, VirtualIntentProcessor processor,
                           IntentData data, Optional<IntentData> stored) {
        this.networkId = checkNotNull(networkId);
        this.processor = checkNotNull(processor);
        this.data = checkNotNull(data);
        this.stored = checkNotNull(stored);
    }

    @Override
    public Optional<VirtualIntentProcessPhase> execute() {
        try {
            List<Intent> compiled = processor
                    .compile(networkId, data.intent(),
                             //TODO consider passing an optional here in the future
                             stored.map(IntentData::installables).orElse(null));
            return Optional.of(new VirtualIntentInstalling(networkId, processor,
                                                           IntentData.compiled(data, compiled), stored));
        } catch (IntentException e) {
            log.warn("Unable to compile intent {} due to:", data.intent(), e);
            if (stored.filter(x -> !x.installables().isEmpty()).isPresent()) {
                // removing orphaned flows and deallocating resources
                return Optional.of(
                        new VirtualIntentWithdrawing(networkId, processor,
                                                     IntentData.compiled(data, stored.get().installables())));
            } else {
                return Optional.of(new VirtualIntentFailed(data));
            }
        }
    }
}
