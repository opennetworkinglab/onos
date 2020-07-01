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
package org.onosproject.k8snode.codec;


import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.k8snode.api.K8sHost;

/**
 * Hamcrest matcher for kubernetes host.
 */
public final class K8sHostJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final K8sHost host;

    private static final String HOST_IP = "hostIp";
    private static final String NODE_NAMES = "nodeNames";
    private static final String STATE = "state";

    private K8sHostJsonMatcher(K8sHost host) {
        this.host = host;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check host IP
        String jsonHostIp = jsonNode.get(HOST_IP).asText();
        String hostIp = host.hostIp().toString();
        if (!jsonHostIp.equals(hostIp)) {
            description.appendText("host IP was " + jsonHostIp);
            return false;
        }

        // check state
        String jsonState = jsonNode.get(STATE).asText();
        String state = host.state().name();
        if (!jsonState.equals(state)) {
            description.appendText("state was " + jsonState);
            return false;
        }

        // check node names size
        JsonNode jsonNames = jsonNode.get(NODE_NAMES);
        if (jsonNames.size() != host.nodeNames().size()) {
            description.appendText("Node names size was " + jsonNames.size());
            return false;
        }

        // check node names
        for (String name : host.nodeNames()) {
            boolean nameFound = false;
            for (int nameIndex = 0; nameIndex < jsonNames.size(); nameIndex++) {
                if (name.equals(jsonNames.get(nameIndex).asText())) {
                    nameFound = true;
                    break;
                }
            }

            if (!nameFound) {
                description.appendText("Name not found " + name);
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(host.toString());
    }

    /**
     * Factory to allocate an k8s host matcher.
     *
     * @param host k8s host object we are looking for
     * @return matcher
     */
    public static K8sHostJsonMatcher matchesK8sHost(K8sHost host) {
        return new K8sHostJsonMatcher(host);
    }
}
