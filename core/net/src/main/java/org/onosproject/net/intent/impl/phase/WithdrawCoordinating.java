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

import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentException;
import org.onosproject.net.intent.impl.IntentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a phase to create a {@link FlowRuleOperations} instance
 * with using registered intent installers.
 */
final class WithdrawCoordinating implements IntentProcessPhase {

    private static final Logger log = LoggerFactory.getLogger(WithdrawCoordinating.class);

    // TODO: define an interface and use it, instead of IntentManager
    private final IntentManager intentManager;
    private final IntentData pending;
    private final IntentData current;

    WithdrawCoordinating(IntentManager intentManager, IntentData pending, IntentData current) {
        this.intentManager = checkNotNull(intentManager);
        this.pending = checkNotNull(pending);
        this.current = checkNotNull(current);
    }

    @Override
    public Optional<IntentProcessPhase> execute() {
        try {
            // Note: current.installables() are not null or empty due to createIntentUpdate check
            FlowRuleOperations flowRules = intentManager.uninstallCoordinate(current, pending);
            pending.setInstallables(current.installables());
            return Optional.of(new Withdrawing(intentManager, pending, flowRules));
        } catch (IntentException e) {
            log.warn("Unable to generate generate a FlowRuleOperations from intent {} due to:", pending.intent(), e);
            return Optional.of(new WithdrawingFailed(pending));
        }
    }
}
