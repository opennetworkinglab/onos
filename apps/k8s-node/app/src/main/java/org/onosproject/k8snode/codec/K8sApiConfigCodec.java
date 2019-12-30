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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.k8snode.api.DefaultK8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfig.Scheme;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.k8snode.api.K8sApiConfig.Scheme.HTTPS;
import static org.onosproject.k8snode.api.K8sApiConfig.State.DISCONNECTED;

/**
 * Kubernetes API server config codec used for serializing and de-serializing JSON string.
 */
public final class K8sApiConfigCodec extends JsonCodec<K8sApiConfig> {

    private static final String SCHEME = "scheme";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String PORT = "port";
    private static final String STATE = "state";
    private static final String TOKEN = "token";
    private static final String CA_CERT_DATA = "caCertData";
    private static final String CLIENT_CERT_DATA = "clientCertData";
    private static final String CLIENT_KEY_DATA = "clientKeyData";

    private static final String MISSING_MESSAGE = " is required in K8sApiConfig";

    @Override
    public ObjectNode encode(K8sApiConfig entity, CodecContext context) {
        ObjectNode node = context.mapper().createObjectNode()
                .put(SCHEME, entity.scheme().name())
                .put(IP_ADDRESS, entity.ipAddress().toString())
                .put(PORT, entity.port())
                .put(STATE, entity.state().name());

        if (entity.scheme() == HTTPS) {
            node.put(CA_CERT_DATA, entity.caCertData())
                .put(CLIENT_CERT_DATA, entity.clientCertData())
                .put(CLIENT_KEY_DATA, entity.clientKeyData());

            if (entity.token() != null) {
                node.put(TOKEN, entity.token());
            }

        } else {
            if (entity.token() != null) {
                node.put(TOKEN, entity.token());
            }

            if (entity.caCertData() != null) {
                node.put(CA_CERT_DATA, entity.caCertData());
            }

            if (entity.clientCertData() != null) {
                node.put(CLIENT_CERT_DATA, entity.clientCertData());
            }

            if (entity.clientKeyData() != null) {
                node.put(CLIENT_KEY_DATA, entity.clientKeyData());
            }
        }

        return node;
    }

    @Override
    public K8sApiConfig decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        Scheme scheme = Scheme.valueOf(nullIsIllegal(
                json.get(SCHEME).asText(), SCHEME + MISSING_MESSAGE));
        IpAddress ipAddress = IpAddress.valueOf(nullIsIllegal(
                json.get(IP_ADDRESS).asText(), IP_ADDRESS + MISSING_MESSAGE));
        int port = json.get(PORT).asInt();

        K8sApiConfig.Builder builder = DefaultK8sApiConfig.builder()
                .scheme(scheme)
                .ipAddress(ipAddress)
                .port(port)
                .state(DISCONNECTED);

        JsonNode tokenJson = json.get(TOKEN);
        JsonNode caCertDataJson = json.get(CA_CERT_DATA);
        JsonNode clientCertDataJson = json.get(CLIENT_CERT_DATA);
        JsonNode clientKeyDataJson = json.get(CLIENT_KEY_DATA);

        String token = "";
        String caCertData = "";
        String clientCertData = "";
        String clientKeyData = "";

        if (scheme == HTTPS) {
            caCertData = nullIsIllegal(caCertDataJson.asText(),
                    CA_CERT_DATA + MISSING_MESSAGE);
            clientCertData = nullIsIllegal(clientCertDataJson.asText(),
                    CLIENT_CERT_DATA + MISSING_MESSAGE);
            clientKeyData = nullIsIllegal(clientKeyDataJson.asText(),
                    CLIENT_KEY_DATA + MISSING_MESSAGE);

            if (tokenJson != null) {
                token = tokenJson.asText();
            }

        } else {
            if (tokenJson != null) {
                token = tokenJson.asText();
            }

            if (caCertDataJson != null) {
                caCertData = caCertDataJson.asText();
            }

            if (clientCertDataJson != null) {
                clientCertData = clientCertDataJson.asText();
            }

            if (clientKeyDataJson != null) {
                clientKeyData = clientKeyDataJson.asText();
            }
        }

        if (StringUtils.isNotEmpty(token)) {
            builder.token(token);
        }

        if (StringUtils.isNotEmpty(caCertData)) {
            builder.caCertData(caCertData);
        }

        if (StringUtils.isNotEmpty(clientCertData)) {
            builder.clientCertData(clientCertData);
        }

        if (StringUtils.isNotEmpty(clientKeyData)) {
            builder.clientKeyData(clientKeyData);
        }

        return builder.build();
    }
}
