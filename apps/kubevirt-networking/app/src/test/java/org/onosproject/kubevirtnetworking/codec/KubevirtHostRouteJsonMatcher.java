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
import org.onosproject.kubevirtnetworking.api.KubevirtHostRoute;

/**
 * Hamcrest matcher for kubevirt host route interface.
 */
public final class KubevirtHostRouteJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KubevirtHostRoute hostRoute;

    private static final String DESTINATION = "destination";
    private static final String NEXTHOP = "nexthop";

    private KubevirtHostRouteJsonMatcher(KubevirtHostRoute hostRoute) {
        this.hostRoute = hostRoute;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {
        // check destination
        String jsonDestination = jsonNode.get(DESTINATION).asText();
        String destination = hostRoute.destination().toString();
        if (!jsonDestination.equals(destination)) {
            description.appendText("destination was " + jsonDestination);
            return false;
        }

        // check nexthop
        JsonNode jsonNexthop = jsonNode.get(NEXTHOP);
        if (jsonNexthop != null) {
            String nexthop = hostRoute.nexthop().toString();
            if (!jsonNexthop.asText().equals(nexthop)) {
                description.appendText("nexthop was " + jsonNexthop);
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(description.toString());
    }

    /**
     * Factory to allocate an kubevirt host route matcher.
     *
     * @param route kubevirt host route object we are looking for
     * @return matcher
     */
    public static KubevirtHostRouteJsonMatcher matchesKubevirtHostRoute(KubevirtHostRoute route) {
        return new KubevirtHostRouteJsonMatcher(route);
    }
}
