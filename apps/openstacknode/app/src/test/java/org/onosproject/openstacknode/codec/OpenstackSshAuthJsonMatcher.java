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
import org.onosproject.openstacknode.api.OpenstackSshAuth;

/**
 * Hamcrest matcher for openstack SSH auth.
 */
public final class OpenstackSshAuthJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final OpenstackSshAuth sshAuth;

    private static final String ID = "id";
    private static final String PASSWORD = "password";

    private OpenstackSshAuthJsonMatcher(OpenstackSshAuth sshAuth) {
        this.sshAuth = sshAuth;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check id
        String jsonId = jsonNode.get(ID).asText();
        String id = sshAuth.id();
        if (!jsonId.equals(id)) {
            description.appendText("id was " + jsonId);
            return false;
        }

        // check password
        String jsonPassword = jsonNode.get(PASSWORD).asText();
        String password = sshAuth.id();
        if (!jsonPassword.equals(password)) {
            description.appendText("password was " + jsonId);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(sshAuth.toString());
    }

    /**
     * Factory to allocate an openstack SSH auth matcher.
     *
     * @param sshAuth openstack SSH auth object we are looking for
     * @return matcher
     */
    public static OpenstackSshAuthJsonMatcher matchOpenstackSshAuth(OpenstackSshAuth sshAuth) {
        return new OpenstackSshAuthJsonMatcher(sshAuth);
    }
}
