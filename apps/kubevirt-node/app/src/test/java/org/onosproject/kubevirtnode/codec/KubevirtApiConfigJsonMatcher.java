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
package org.onosproject.kubevirtnode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;

/**
 * Hamcrest matcher for KubeVirt API config.
 */
public final class KubevirtApiConfigJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final KubevirtApiConfig kubevirtApiConfig;

    private static final String SCHEME = "scheme";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String PORT = "port";
    private static final String STATE = "state";
    private static final String TOKEN = "token";
    private static final String CA_CERT_DATA = "caCertData";
    private static final String CLIENT_CERT_DATA = "clientCertData";
    private static final String CLIENT_KEY_DATA = "clientKeyData";
    private static final String SERVICE_FQDN = "serviceFqdn";
    private static final String API_SERVER_FQDN = "apiServerFqdn";
    private static final String CONTROLLER_IP = "controllerIp";

    private KubevirtApiConfigJsonMatcher(KubevirtApiConfig kubevirtApiConfig) {
        this.kubevirtApiConfig = kubevirtApiConfig;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {
        // check scheme
        String jsonScheme = jsonNode.get(SCHEME).asText();
        String scheme = kubevirtApiConfig.scheme().name();
        if (!jsonScheme.equals(scheme)) {
            description.appendText("scheme was " + jsonScheme);
            return false;
        }

        // check IP address
        String jsonIpAddress = jsonNode.get(IP_ADDRESS).asText();
        String ipAddress = kubevirtApiConfig.ipAddress().toString();
        if (!jsonIpAddress.equals(ipAddress)) {
            description.appendText("ipAddress was " + jsonIpAddress);
            return false;
        }

        // check port
        int jsonPort = jsonNode.get(PORT).asInt();
        int port = kubevirtApiConfig.port();
        if (jsonPort != port) {
            description.appendText("port was " + jsonPort);
            return false;
        }

        // check state
        JsonNode jsonState = jsonNode.get(STATE);
        String state = kubevirtApiConfig.state().name();
        if (jsonState != null) {
            if (!jsonState.asText().equals(state)) {
                description.appendText("state was " + jsonState);
                return false;
            }
        }

        // check token
        JsonNode jsonToken = jsonNode.get(TOKEN);
        String token = kubevirtApiConfig.token();
        if (jsonToken != null) {
            if (!jsonToken.asText().equals(token)) {
                description.appendText("token was " + jsonToken);
                return false;
            }
        }

        // check caCertData
        JsonNode jsonCaCertData = jsonNode.get(CA_CERT_DATA);
        String caCertData = kubevirtApiConfig.caCertData();
        if (jsonCaCertData != null) {
            if (!jsonCaCertData.asText().equals(caCertData)) {
                description.appendText("caCertData was " + jsonCaCertData);
                return false;
            }
        }

        // check clientCertData
        JsonNode jsonClientCertData = jsonNode.get(CLIENT_CERT_DATA);
        String clientCertData = kubevirtApiConfig.clientCertData();

        if (jsonClientCertData != null) {
            if (!jsonClientCertData.asText().equals(clientCertData)) {
                description.appendText("clientCertData was " + jsonClientCertData);
                return false;
            }
        }

        // check clientKeyData
        JsonNode jsonClientKeyData = jsonNode.get(CLIENT_KEY_DATA);
        String clientKeyData = kubevirtApiConfig.clientKeyData();

        if (jsonClientKeyData != null) {
            if (!jsonClientKeyData.asText().equals(clientKeyData)) {
                description.appendText("clientKeyData was " + jsonClientKeyData);
                return false;
            }
        }

        // service FQDN
        JsonNode jsonServiceFqdn = jsonNode.get(SERVICE_FQDN);
        String serviceFqdn = kubevirtApiConfig.serviceFqdn();

        if (jsonServiceFqdn != null) {
            if (!jsonServiceFqdn.asText().equals(serviceFqdn)) {
                description.appendText("serviceFqdn was " + jsonServiceFqdn);
                return false;
            }
        }

        // API server FQDN
        JsonNode jsonApiServerFqdn = jsonNode.get(API_SERVER_FQDN);
        String apiServerFqdn = kubevirtApiConfig.apiServerFqdn();

        if (jsonApiServerFqdn != null) {
            if (!jsonApiServerFqdn.asText().equals(apiServerFqdn)) {
                description.appendText("apiServerFqdn was " + jsonApiServerFqdn);
                return false;
            }
        }

        // Controller IP
        JsonNode jsonControllerIp = jsonNode.get(CONTROLLER_IP);
        String controllerIp = kubevirtApiConfig.controllerIp().toString();

        if (jsonControllerIp != null) {
            if (!jsonControllerIp.asText().equals(controllerIp)) {
                description.appendText("controller IP was " + jsonControllerIp);
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(kubevirtApiConfig.toString());
    }

    /**
     * Factory to allocate an kubevirtApiConfig matcher.
     *
     * @param config kubevirtApiConfig object we are looking for
     * @return matcher
     */
    public static KubevirtApiConfigJsonMatcher matchesKubevirtApiConfig(KubevirtApiConfig config) {
        return new KubevirtApiConfigJsonMatcher(config);
    }
}
