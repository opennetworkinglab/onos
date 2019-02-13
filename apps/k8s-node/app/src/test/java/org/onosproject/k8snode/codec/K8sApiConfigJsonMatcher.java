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
package org.onosproject.k8snode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.k8snode.api.K8sApiConfig;

/**
 * Hamcrest matcher for kubernetes API config.
 */
public final class K8sApiConfigJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final K8sApiConfig k8sApiConfig;

    private static final String SCHEME = "scheme";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String PORT = "port";
    private static final String STATE = "state";
    private static final String TOKEN = "token";
    private static final String CA_CERT_DATA = "caCertData";
    private static final String CLIENT_CERT_DATA = "clientCertData";
    private static final String CLIENT_KEY_DATA = "clientKeyData";

    private K8sApiConfigJsonMatcher(K8sApiConfig k8sApiConfig) {
        this.k8sApiConfig = k8sApiConfig;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check scheme
        String jsonScheme = jsonNode.get(SCHEME).asText();
        String scheme = k8sApiConfig.scheme().name();
        if (!jsonScheme.equals(scheme)) {
            description.appendText("scheme was " + jsonScheme);
            return false;
        }

        // check IP address
        String jsonIpAddress = jsonNode.get(IP_ADDRESS).asText();
        String ipAddress = k8sApiConfig.ipAddress().toString();
        if (!jsonIpAddress.equals(ipAddress)) {
            description.appendText("ipAddress was " + jsonIpAddress);
            return false;
        }

        // check port
        int jsonPort = jsonNode.get(PORT).asInt();
        int port = k8sApiConfig.port();
        if (jsonPort != port) {
            description.appendText("port was " + jsonPort);
            return false;
        }

        // check state
        JsonNode jsonState = jsonNode.get(STATE);
        String state = k8sApiConfig.state().name();
        if (jsonState != null) {
            if (!jsonState.asText().equals(state)) {
                description.appendText("state was " + jsonState);
                return false;
            }
        }

        // check token
        JsonNode jsonToken = jsonNode.get(TOKEN);
        String token = k8sApiConfig.token();
        if (jsonToken != null) {
            if (!jsonToken.asText().equals(token)) {
                description.appendText("token was " + jsonToken);
                return false;
            }
        }

        // check caCertData
        JsonNode jsonCaCertData = jsonNode.get(CA_CERT_DATA);
        String caCertData = k8sApiConfig.caCertData();
        if (jsonCaCertData != null) {
            if (!jsonCaCertData.asText().equals(caCertData)) {
                description.appendText("caCertData was " + jsonCaCertData);
                return false;
            }
        }

        // check clientCertData
        JsonNode jsonClientCertData = jsonNode.get(CLIENT_CERT_DATA);
        String clientCertData = k8sApiConfig.clientCertData();

        if (jsonClientCertData != null) {
            if (!jsonClientCertData.asText().equals(clientCertData)) {
                description.appendText("clientCertData was " + jsonClientCertData);
                return false;
            }
        }

        // check clientKeyData
        JsonNode jsonClientKeyData = jsonNode.get(CLIENT_KEY_DATA);
        String clientKeyData = k8sApiConfig.clientKeyData();

        if (jsonClientKeyData != null) {
            if (!jsonClientKeyData.asText().equals(clientKeyData)) {
                description.appendText("clientKeyData was " + jsonClientKeyData);
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(k8sApiConfig.toString());
    }

    /**
     * Factory to allocate an k8sApiConfig matcher.
     *
     * @param config k8sApiConfig object we are looking for
     * @return matcher
     */
    public static K8sApiConfigJsonMatcher matchesK8sApiConfig(K8sApiConfig config) {
        return new K8sApiConfigJsonMatcher(config);
    }
}
