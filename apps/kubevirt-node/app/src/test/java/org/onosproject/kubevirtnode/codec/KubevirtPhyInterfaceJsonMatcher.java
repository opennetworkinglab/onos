/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.kubevirtnode.api.KubevirtPhyInterface;

/**
 * Hamcrest matcher for kubevirt physical interface.
 */
public final class KubevirtPhyInterfaceJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KubevirtPhyInterface phyIntf;
    private static final String NETWORK = "network";
    private static final String INTERFACE = "intf";

    private KubevirtPhyInterfaceJsonMatcher(KubevirtPhyInterface phyIntf) {
        this.phyIntf = phyIntf;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {
        // check network name
        String jsonNetwork = jsonNode.get(NETWORK).asText();
        String network = phyIntf.network();
        if (!jsonNetwork.equals(network)) {
            description.appendText("network name was " + jsonNetwork);
            return false;
        }

        // check interface name
        String jsonIntf = jsonNode.get(INTERFACE).asText();
        String intf = phyIntf.intf();
        if (!jsonIntf.equals(intf)) {
            description.appendText("interface name was " + jsonIntf);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(phyIntf.toString());
    }

    /**
     * Factory to allocate an kubevirt physical interface matcher.
     *
     * @param phyIntf kubevirt physical interface object we are looking for
     * @return matcher
     */
    public static KubevirtPhyInterfaceJsonMatcher
                    matchesKubevirtPhyInterface(KubevirtPhyInterface phyIntf) {
        return new KubevirtPhyInterfaceJsonMatcher(phyIntf);
    }
}
