/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.flow;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.provider.Provider;

import java.util.concurrent.Future;

/**
 * Abstraction of a flow rule provider.
 */
public interface FlowRuleProvider extends Provider {

    static final int POLL_INTERVAL = 10;

    /**
     * Instructs the provider to apply the specified flow rules to their
     * respective devices.
     * @param flowRules one or more flow rules
     * throws SomeKindOfException that indicates which ones were applied and
     *                  which ones failed
     */
    void applyFlowRule(FlowRule... flowRules);

    /**
     * Instructs the provider to remove the specified flow rules to their
     * respective devices.
     * @param flowRules one or more flow rules
     * throws SomeKindOfException that indicates which ones were applied and
     *                  which ones failed
     */
    void removeFlowRule(FlowRule... flowRules);

    /**
     * Removes rules by their id.
     * @param id the id to remove
     * @param flowRules one or more flow rules
     */
    void removeRulesById(ApplicationId id, FlowRule... flowRules);

    /**
     * Installs a batch of flow rules. Each flowrule is associated to an
     * operation which results in either addition, removal or modification.
     * @param batch a batch of flow rules
     * @return a future indicating the status of this execution
     */
    Future<CompletedBatchOperation> executeBatch(BatchOperation<FlowRuleBatchEntry> batch);

}
