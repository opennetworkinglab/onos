/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacknode.codec;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.onosproject.openstacknode.api.OpenstackNode;

public class OpenstackNodeJsonArrayMatcher extends TypeSafeMatcher<JsonArray> {

    private final OpenstackNode node;
    private String reason = "";

    public OpenstackNodeJsonArrayMatcher(OpenstackNode node) {
        this.node = node;
    }

    @Override
    protected boolean matchesSafely(JsonArray json) {
        boolean nodeFound = false;
        for (int jsonNodeIndex = 0; jsonNodeIndex < json.size(); jsonNodeIndex++) {
            final JsonObject jsonNode = json.get(jsonNodeIndex).asObject();

            final String hostname = node.hostname();
            final String jsonHostname = jsonNode.get("hostname").asString();
            if (jsonHostname.equals(hostname)) {
                nodeFound = true;
            }
        }

        if (!nodeFound) {
            reason = "Node with hostname " + node.hostname() + " not found";
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
     * Factory to allocate a node array matcher.
     *
     * @param node node object we are looking for
     * @return matcher
     */
    public static OpenstackNodeJsonArrayMatcher hasNode(OpenstackNode node) {
        return new OpenstackNodeJsonArrayMatcher(node);
    }
}
