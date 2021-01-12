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
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;

/**
 * Hamcrest matcher for kubevirt IP pool interface.
 */
public final class KubevirtIpPoolJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KubevirtIpPool ipPool;
    private static final String START = "start";
    private static final String END = "end";

    private KubevirtIpPoolJsonMatcher(KubevirtIpPool ipPool) {
        this.ipPool = ipPool;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {
        // check start
        String jsonStart = jsonNode.get(START).asText();
        String start = ipPool.start().toString();
        if (!jsonStart.equals(start)) {
            description.appendText("start was " + jsonStart);
            return false;
        }

        // check end
        String jsonEnd = jsonNode.get(END).asText();
        String end = ipPool.end().toString();
        if (!jsonEnd.equals(end)) {
            description.appendText("end was " + jsonEnd);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(ipPool.toString());
    }

    /**
     * Factory to allocate an kubevirt IP pool matcher.
     *
     * @param ipPool kubevirt IP pool object we are looking for
     * @return matcher
     */
    public static KubevirtIpPoolJsonMatcher matchesKubevirtIpPool(KubevirtIpPool ipPool) {
        return new KubevirtIpPoolJsonMatcher(ipPool);
    }
}
