/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onlab.packet.IpAddress;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerRule;

/**
 * Hamcrest matcher for load balancer.
 */
public final class KubevirtLoadBalancerJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String VIP = "vip";
    private static final String NETWORK_ID = "networkId";
    private static final String MEMBERS = "members";
    private static final String RULES = "rules";

    private final KubevirtLoadBalancer lb;

    private KubevirtLoadBalancerJsonMatcher(KubevirtLoadBalancer lb) {
        this.lb = lb;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {
        // check name
        String jsonName = jsonNode.get(NAME).asText();
        String name = lb.name();
        if (!jsonName.equals(name)) {
            description.appendText("Name was " + jsonName);
            return false;
        }

        // check description
        JsonNode jsonDescription = jsonNode.get(DESCRIPTION);
        if (jsonDescription != null) {
            String myDescription = lb.description();
            if (!jsonDescription.asText().equals(myDescription)) {
                description.appendText("Description was " + jsonDescription);
                return false;
            }
        }

        // check VIP
        String jsonVip = jsonNode.get(VIP).asText();
        String vip = lb.vip().toString();
        if (!jsonVip.equals(vip)) {
            description.appendText("VIP was " + jsonVip);
            return false;
        }

        // check network ID
        String jsonNetworkId = jsonNode.get(NETWORK_ID).asText();
        String networkId = lb.networkId();
        if (!jsonNetworkId.equals(networkId)) {
            description.appendText("NetworkId was " + jsonNetworkId);
            return false;
        }

        // check members
        ArrayNode jsonMembers = (ArrayNode) jsonNode.get(MEMBERS);
        if (jsonMembers != null) {
            // check size of members array
            if (jsonMembers.size() != lb.members().size()) {
                description.appendText("Members was " + jsonMembers.size());
                return false;
            }

            for (JsonNode jsonMember : jsonMembers) {
                IpAddress member = IpAddress.valueOf(jsonMember.asText());
                if (!lb.members().contains(member)) {
                    description.appendText("Member not found " + jsonMember.toString());
                    return false;
                }
            }
        }

        ArrayNode jsonRules = (ArrayNode) jsonNode.get(RULES);
        if (jsonRules != null) {
            // check size of rules array
            if (jsonRules.size() != lb.rules().size()) {
                description.appendText("Rules was " + jsonRules.size());
                return false;
            }

            // check rules
            for (KubevirtLoadBalancerRule rule : lb.rules()) {
                boolean ruleFound = false;
                for (int ruleIndex = 0; ruleIndex < jsonRules.size(); ruleIndex++) {
                    KubevirtLoadBalancerRuleJsonMatcher ruleMatcher =
                            KubevirtLoadBalancerRuleJsonMatcher
                                    .matchesKubevirtLoadBalancerRule(rule);
                    if (ruleMatcher.matches(jsonRules.get(ruleIndex))) {
                        ruleFound = true;
                        break;
                    }
                }

                if (!ruleFound) {
                    description.appendText("Rule not found " + rule.toString());
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(lb.toString());
    }

    /**
     * Factory to allocate a kubevirt load balancer matcher.
     *
     * @param lb kubevirt load balancer object we are looking for
     * @return matcher
     */
    public static KubevirtLoadBalancerJsonMatcher
        matchesKubevirtLoadBalancer(KubevirtLoadBalancer lb) {
        return new KubevirtLoadBalancerJsonMatcher(lb);
    }
}
