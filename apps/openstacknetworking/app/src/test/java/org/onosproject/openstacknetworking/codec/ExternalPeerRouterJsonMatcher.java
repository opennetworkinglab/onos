/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;

/**
 * Hamcrest matcher for external peer router.
 */
public final class ExternalPeerRouterJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private static final String MAC_ADDRESS = "macAddress";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String VLAN_ID = "vlanId";

    private final ExternalPeerRouter router;

    private ExternalPeerRouterJsonMatcher(ExternalPeerRouter router) {
        this.router = router;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check MAC address
        String jsonMacAddress = jsonNode.get(MAC_ADDRESS).asText();
        String macAddress = router.macAddress().toString();
        if (!jsonMacAddress.equals(macAddress)) {
            description.appendText("macAddress was " + jsonMacAddress);
            return false;
        }

        // check IP address
        String jsonIpAddress = jsonNode.get(IP_ADDRESS).asText();
        String ipAddress = router.ipAddress().toString();
        if (!jsonIpAddress.equals(ipAddress)) {
            description.appendText("ipAddress was " + jsonIpAddress);
            return false;
        }

        // check VLAN ID
        String jsonVlanId = jsonNode.get(VLAN_ID).asText();
        String vlanId = router.vlanId().toString();
        if (!jsonVlanId.equals(vlanId)) {
            description.appendText("VLAN ID was " + jsonIpAddress);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(router.toString());
    }

    /**
     * Factory to allocate an external peer router matcher.
     *
     * @param router openstack external peer router we are looking for
     * @return matcher
     */
    public static ExternalPeerRouterJsonMatcher
                        matchesExternalPeerRouter(ExternalPeerRouter router) {
        return new ExternalPeerRouterJsonMatcher(router);
    }
}
