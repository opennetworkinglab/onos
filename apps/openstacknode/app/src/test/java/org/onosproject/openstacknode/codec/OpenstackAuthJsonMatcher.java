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
import org.onosproject.openstacknode.api.OpenstackAuth;

/**
 * Hamcrest matcher for openstack auth.
 */
public final class OpenstackAuthJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final OpenstackAuth auth;

    private static final String VERSION = "version";
    private static final String PORT = "port";
    private static final String PROTOCOL = "protocol";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String PROJECT = "project";
    private static final String PERSPECTIVE = "perspective";

    private OpenstackAuthJsonMatcher(OpenstackAuth auth) {
        this.auth = auth;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check version
        String jsonVersion = jsonNode.get(VERSION).asText();
        String version = auth.version();
        if (!jsonVersion.equals(version)) {
            description.appendText("version was " + jsonVersion);
            return false;
        }

        // check protocol
        String jsonProtocol = jsonNode.get(PROTOCOL).asText();
        String protocol = auth.protocol().name();
        if (!jsonProtocol.equals(protocol)) {
            description.appendText("protocol was " + jsonProtocol);
            return false;
        }

        // check username
        String jsonUsername = jsonNode.get(USERNAME).asText();
        String username = auth.username();
        if (!jsonUsername.equals(username)) {
            description.appendText("username was " + jsonUsername);
            return false;
        }

        // check password
        String jsonPassword = jsonNode.get(PASSWORD).asText();
        String password = auth.password();
        if (!jsonPassword.equals(password)) {
            description.appendText("password was " + jsonPassword);
            return false;
        }

        // check project
        String jsonProject = jsonNode.get(PROJECT).asText();
        String project = auth.project();
        if (!jsonProject.equals(project)) {
            description.appendText("project was " + jsonProject);
            return false;
        }

        // check perspective
        String jsonPerspective = jsonNode.get(PERSPECTIVE).asText();
        String perspective = auth.perspective().name();
        if (!jsonPerspective.equals(perspective)) {
            description.appendText("perspective was " + jsonPerspective);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(auth.toString());
    }

    /**
     * Factory to allocate an openstack auth matcher.
     *
     * @param auth openstack auth object we are looking for
     * @return matcher
     */
    public static OpenstackAuthJsonMatcher matchOpenstackAuth(OpenstackAuth auth) {
        return new OpenstackAuthJsonMatcher(auth);
    }
}
