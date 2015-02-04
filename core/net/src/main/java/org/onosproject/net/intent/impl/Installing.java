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

import com.google.common.collect.ImmutableList;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO: better naming because install() method actually generate FlowRuleBatchOperations
class Installing implements IntentUpdate {

    private static final Logger log = LoggerFactory.getLogger(Installing.class);

    private final IntentManager intentManager;
    private final Intent intent;
    private final List<Intent> installables;

    // TODO: define an interface and use it, instead of IntentManager
    Installing(IntentManager intentManager, Intent intent, List<Intent> installables) {
        this.intentManager = checkNotNull(intentManager);
        this.intent = checkNotNull(intent);
        this.installables = ImmutableList.copyOf(checkNotNull(installables));
    }

    @Override
    public Optional<IntentUpdate> execute() {
        try {
            List<FlowRuleBatchOperation> converted = intentManager.convert(installables);
            // TODO: call FlowRuleService API to push FlowRules and track resources,
            // which the submitted intent will use.
            return Optional.of(new Installed(intentManager, intent, installables, converted));
        } catch (FlowRuleBatchOperationConversionException e) {
            log.warn("Unable to install intent {} due to:", intent.id(), e.getCause());
            return Optional.of(new InstallingFailed(intentManager, intent, installables, e.converted()));
        }
    }
}
