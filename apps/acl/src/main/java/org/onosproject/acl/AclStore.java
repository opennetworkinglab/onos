/*
 * Copyright 2015-present Open Networking Foundation
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
 *
 * Originally created by Pengfei Lu, Network and Cloud Computing Laboratory, Dalian University of Technology, China
 * Advisers: Keqiu Li, Heng Qi and Haisheng Yu
 * This work is supported by the State Key Program of National Natural Science of China(Grant No. 61432002)
 * and Prospective Research Project on Future Networks in Jiangsu Future Networks Innovation Institute.
 */
package org.onosproject.acl;

import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.store.Store;

import java.util.List;
import java.util.Set;

/**
 * Service interface exported by ACL distributed store.
 */
public interface AclStore extends Store {

    /**
     * Gets a list containing all ACL rules.
     *
     * @return a list containing all ACL rules
     */
    List<AclRule> getAclRules();

    /**
     * Adds a new ACL rule.
     *
     * @param rule new ACL rule
     */
    void addAclRule(AclRule rule);

    /**
     * Gets an existing ACL rule.
     *
     * @param ruleId ACL rule id
     * @return ACL rule with the given id
     */
    AclRule getAclRule(RuleId ruleId);

    /**
     * Removes an existing ACL rule by rule id.
     *
     * @param ruleId ACL rule id
     */
    void removeAclRule(RuleId ruleId);

    /**
     * Clears ACL and reset all.
     */
    void clearAcl();

    /**
     * Gets the current priority for new ACL flow rule by device id.
     *
     * @param deviceId device id
     * @return new ACL flow rule's priority in the given device
     */
    int getPriorityByDevice(DeviceId deviceId);

    /**
     * Gets a set containing all ACL flow rules belonging to a given ACL rule.
     *
     * @param ruleId ACL rule id
     * @return a set containing all ACL flow rules belonging to the given ACL rule
     */
    Set<FlowRule> getFlowByRule(RuleId ruleId);

    /**
     * Adds a new mapping from ACL rule to ACL flow rule.
     *
     * @param ruleId   ACL rule id
     * @param flowRule ACL flow rule
     */
    void addRuleToFlowMapping(RuleId ruleId, FlowRule flowRule);

    /**
     * Removes an existing mapping from ACL rule to ACL flow rule.
     *
     * @param ruleId ACL rule id
     */
    void removeRuleToFlowMapping(RuleId ruleId);

    /**
     * Gets a list containing all allowing ACL rules matching a given denying ACL rule.
     *
     * @param denyingRuleId denying ACL rule id
     * @return a list containing all allowing ACL rules matching the given denying ACL rule
     */
    List<RuleId> getAllowingRuleByDenyingRule(RuleId denyingRuleId);

    /**
     * Adds a new mapping from denying ACL rule to allowing ACL rule.
     *
     * @param denyingRuleId  denying ACL rule id
     * @param allowingRuleId allowing ACL rule id
     */
    void addDenyToAllowMapping(RuleId denyingRuleId, RuleId allowingRuleId);

    /**
     * Removes an exsiting mapping from denying ACL rule to allowing ACL rule.
     *
     * @param denyingRuleId denying ACL rule id
     */
    void removeDenyToAllowMapping(RuleId denyingRuleId);

    /**
     * Checks if an existing ACL rule already works in a given device.
     *
     * @param ruleId   ACL rule id
     * @param deviceId device id
     * @return true if the given ACL rule works in the given device
     */
    boolean checkIfRuleWorksInDevice(RuleId ruleId, DeviceId deviceId);

    /**
     * Adds a new mapping from ACL rule to device.
     *
     * @param ruleId   ACL rule id
     * @param deviceId device id
     */
    void addRuleToDeviceMapping(RuleId ruleId, DeviceId deviceId);

    /**
     * Removes an existing mapping from ACL rule to device.
     *
     * @param ruleId ACL rule id
     */
    void removeRuleToDeviceMapping(RuleId ruleId);

}
