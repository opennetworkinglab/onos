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

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;

public final class KubevirtNetworkJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {

    private final KubevirtNetwork network;
    private String reason = "";

    public KubevirtNetworkJsonArrayMatcher(KubevirtNetwork network) {
        this.network = network;
    }

    @Override
    protected boolean matchesSafely(JsonArray json) {
        boolean networkFound = false;
        for (int jsonNetworkIndex = 0; jsonNetworkIndex < json.size(); jsonNetworkIndex++) {
            final JsonObject jsonNode = json.get(jsonNetworkIndex).asObject();

            final String networkId = network.networkId();
            final String jsonNetworkId = jsonNode.get("networkId").asString();
            if (jsonNetworkId.equals(networkId)) {
                networkFound = true;
            }
        }

        if (!networkFound) {
            reason = "Network with networkId " + network.networkId() + " not found";
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(reason);
    }

    /**
     * Factory to allocate a network array matcher.
     *
     * @param network network object we are looking for
     * @return matcher
     */
    public static KubevirtNetworkJsonArrayMatcher hasNetwork(KubevirtNetwork network) {
        return new KubevirtNetworkJsonArrayMatcher(network);
    }
}
