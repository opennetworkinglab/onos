/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Collection;

/**
 * Flow rule programmable device behaviour.
 */
public interface FlowRuleProgrammable extends HandlerBehaviour {

    /**
     * Retrieves the collection of flow rule entries currently installed on the device.
     *
     * @return collection of flow rules
     */
    Collection<FlowEntry> getFlowEntries();

    /**
     * Applies the specified collection of flow rules to the device.
     *
     * @param rules flow rules to be added
     * @return collection of flow rules that were added successfully
     */
    Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules);

    /**
     * Removes the specified collection of flow rules from the device.
     *
     * @param rules flow rules to be removed
     * @return collection of flow rules that were removed successfully
     */
    Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules);

}
