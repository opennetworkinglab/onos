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
package org.onosproject.net.intent.impl;

import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a phase of installing an intent with calling
 * {@link org.onosproject.net.flow.FlowRuleService}.
 */
class Installing implements IntentUpdate {

    private static final Logger log = LoggerFactory.getLogger(Installing.class);

    private final IntentManager intentManager;
    private final IntentData pending;
    private final FlowRuleOperations flowRules;

    // TODO: define an interface and use it, instead of IntentManager
    Installing(IntentManager intentManager, IntentData pending, FlowRuleOperations flowRules) {
        this.intentManager = checkNotNull(intentManager);
        this.pending = checkNotNull(pending);
        this.flowRules = flowRules;
    }

    @Override
    public Optional<IntentUpdate> execute() {
        try {
            intentManager.flowRuleService.apply(flowRules); // FIXME we need to provide a context
            return Optional.of(new Installed(pending));
        // What kinds of exceptions are thrown by FlowRuleService.apply()?
        // Is IntentException a correct exception abstraction?
        } catch (IntentException e) {
            log.warn("Unable to install intent {} due to: {}", pending.intent().id(), e);
            return Optional.of(new InstallingFailed(pending));
        }
    }
}
