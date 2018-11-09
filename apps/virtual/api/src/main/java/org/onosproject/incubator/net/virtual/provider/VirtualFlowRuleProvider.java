/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.incubator.net.virtual.provider;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchOperation;

/**
 * Abstraction of a virtual flow rule provider.
 * This provider virtualizes and de-virtualizes FlowRule.
 * See {@link org.onosproject.net.flow.FlowRule}.
 */
public interface VirtualFlowRuleProvider extends VirtualProvider {

    /**
     * Instructs the provider to apply the specified flow rules to their
     * respective virtual devices.
     *
     * @param networkId the identity of the virtual network where this rule applies
     * @param flowRules one or more flow rules
     */
    void applyFlowRule(NetworkId networkId, FlowRule... flowRules);

    /**
     * Instructs the provider to remove the specified flow rules to their
     * respective virtual devices.
     *
     * @param networkId the identity of the virtual network where this rule applies
     * @param flowRules one or more flow rules
     */
    void removeFlowRule(NetworkId networkId, FlowRule... flowRules);

    /**
     * Installs a batch of flow rules. Each flowrule is associated to an
     * operation which results in either addition, removal or modification.
     *
     * @param networkId the identity of the virtual network where this rule applies
     * @param batch a batch of flow rules
     */
    void executeBatch(NetworkId networkId, FlowRuleBatchOperation batch);
}
