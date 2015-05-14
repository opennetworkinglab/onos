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

package org.onosproject.segmentrouting;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Segment Routing Policy Handler.
 */
public class PolicyHandler {

    protected final Logger log = getLogger(getClass());

    private final HashMap<String, Policy> policyMap;

    /**
     * Creates a reference.
     */
    public PolicyHandler() {
        policyMap = new HashMap<>();
    }

    /**
     * Returns the policies.
     *
     * @return policy list
     */
    public List<Policy> getPolicies() {
        List<Policy> policies = new ArrayList<>();
        policyMap.values().forEach(policy -> policies.add(
                new TunnelPolicy((TunnelPolicy) policy)));

        return policies;
    }

    /**
     * Creates a policy using the policy information given.
     *
     * @param policy policy reference to create
     */
    public void createPolicy(Policy policy) {
        policy.create();
        policyMap.put(policy.id(), policy);
    }

    /**
     * Removes the policy given.
     *
     * @param policyInfo policy information to remove
     */
    public void removePolicy(Policy policyInfo) {
        if (policyMap.get(policyInfo.id()) != null) {
            if (policyMap.get(policyInfo.id()).remove()) {
                policyMap.remove(policyInfo.id());
            } else {
                log.error("Failed to remove the policy {}", policyInfo.id());
            }
        } else {
            log.warn("Policy {} was not found", policyInfo.id());
        }
    }

}
