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
package org.onosproject.openstacknode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.behaviour.ControllerInfo;

/**
 * Hamcrest matcher for openstack controller.
 */
public final class OpenstackControllerJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final ControllerInfo controller;
    private static final String IP = "ip";
    private static final String PORT = "port";

    private OpenstackControllerJsonMatcher(ControllerInfo controller) {
        this.controller = controller;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check IP address
        String jsonIp = jsonNode.get(IP).asText();
        String ip = controller.ip().toString();
        if (!jsonIp.equals(ip)) {
            description.appendText("controller IP was " + jsonIp);
            return false;
        }

        // check port
        int jsonPort = jsonNode.get(PORT).asInt();
        int port = controller.port();

        if (jsonPort != port) {
            description.appendText("controller port was " + jsonPort);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(controller.toString());
    }

    /**
     * Factory to allocate an openstack controller matcher.
     *
     * @param controller openstack controller object we are looking for
     * @return matcher
     */
    public static OpenstackControllerJsonMatcher
                    matchesOpenstackController(ControllerInfo controller) {
        return new OpenstackControllerJsonMatcher(controller);
    }
}
