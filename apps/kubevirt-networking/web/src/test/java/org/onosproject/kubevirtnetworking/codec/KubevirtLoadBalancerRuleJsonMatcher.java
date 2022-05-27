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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerRule;

/**
 * Hamcrest matcher for kubevirt load balancer.
 */
public final class KubevirtLoadBalancerRuleJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KubevirtLoadBalancerRule rule;

    private static final String PROTOCOL = "protocol";
    private static final String PORT_RANGE_MAX = "portRangeMax";
    private static final String PORT_RANGE_MIN = "portRangeMin";

    private KubevirtLoadBalancerRuleJsonMatcher(KubevirtLoadBalancerRule rule) {
        this.rule = rule;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {
        // check protocol
        JsonNode jsonProtocol = jsonNode.get(PROTOCOL);
        if (jsonProtocol != null) {
            String protocol = rule.protocol();
            if (!jsonProtocol.asText().equals(protocol)) {
                description.appendText("Protocol was " + jsonProtocol);
                return false;
            }
        }

        // check port range max
        JsonNode jsonPortRangeMax = jsonNode.get(PORT_RANGE_MAX);
        if (jsonPortRangeMax != null) {
            int portRangeMax = rule.portRangeMax();
            if (portRangeMax != jsonPortRangeMax.asInt()) {
                description.appendText("PortRangeMax was " + jsonPortRangeMax);
                return false;
            }
        }

        // check port range min
        JsonNode jsonPortRangeMin = jsonNode.get(PORT_RANGE_MIN);
        if (jsonPortRangeMin != null) {
            int portRangeMin = rule.portRangeMin();
            if (portRangeMin != jsonPortRangeMin.asInt()) {
                description.appendText("PortRangeMin was " + jsonPortRangeMin);
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(rule.toString());
    }

    /**
     * Factory to allocate an kubevirt load balancer rule matcher.
     *
     * @param rule kubevirt load balancer rule object we are looking for
     * @return matcher
     */
    public static KubevirtLoadBalancerRuleJsonMatcher
        matchesKubevirtLoadBalancerRule(KubevirtLoadBalancerRule rule) {
        return new KubevirtLoadBalancerRuleJsonMatcher(rule);
    }
}
