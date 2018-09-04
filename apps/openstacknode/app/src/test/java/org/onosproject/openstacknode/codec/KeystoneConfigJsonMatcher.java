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
import org.onosproject.openstacknode.api.KeystoneConfig;
import org.onosproject.openstacknode.api.OpenstackAuth;

/**
 * Hamcrest matcher for keystone config.
 */
public final class KeystoneConfigJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KeystoneConfig keystoneConfig;

    private static final String ENDPOINT = "endpoint";
    private static final String AUTHENTICATION = "authentication";

    private KeystoneConfigJsonMatcher(KeystoneConfig keystoneConfig) {
        this.keystoneConfig = keystoneConfig;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check endpoint
        JsonNode jsonEndpoint = jsonNode.get(ENDPOINT);
        if (jsonEndpoint != null) {
            String endpoint = keystoneConfig.endpoint();
            if (!jsonEndpoint.asText().equals(endpoint)) {
                description.appendText("endpoint was " + jsonEndpoint);
                return false;
            }
        }

        // check openstack auth
        JsonNode jsonAuth = jsonNode.get(AUTHENTICATION);
        if (jsonAuth != null) {
            OpenstackAuth auth = keystoneConfig.authentication();
            OpenstackAuthJsonMatcher authMatcher =
                    OpenstackAuthJsonMatcher.matchOpenstackAuth(auth);
            return authMatcher.matches(jsonAuth);
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(keystoneConfig.toString());
    }

    /**
     * Factory to allocate keystone config matcher.
     *
     * @param config keystone config object we are looking for
     * @return matcher
     */
    public static KeystoneConfigJsonMatcher matchKeystoneConfig(KeystoneConfig config) {
        return new KeystoneConfigJsonMatcher(config);
    }
}
