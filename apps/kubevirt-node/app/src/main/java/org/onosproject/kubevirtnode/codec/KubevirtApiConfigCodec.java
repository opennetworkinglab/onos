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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnode.api.DefaultKubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfig.Scheme.HTTPS;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfig.State.DISCONNECTED;

/**
 * KubeVirt API server config codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtApiConfigCodec extends JsonCodec<KubevirtApiConfig> {

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
    private static final String DATACENTER_ID = "datacenterId";
    private static final String CLUSTER_ID = "clusterId";

    private static final String MISSING_MESSAGE = " is required in KubevirtApiConfig";

    @Override
    public ObjectNode encode(KubevirtApiConfig entity, CodecContext context) {
        ObjectNode node = context.mapper().createObjectNode()
                .put(SCHEME, entity.scheme().name())
                .put(IP_ADDRESS, entity.ipAddress().toString())
                .put(PORT, entity.port())
                .put(STATE, entity.state().name())
                .put(DATACENTER_ID, entity.datacenterId())
                .put(CLUSTER_ID, entity.clusterId());

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

        if (entity.serviceFqdn() != null) {
            node.put(SERVICE_FQDN, entity.serviceFqdn());
        }

        if (entity.apiServerFqdn() != null) {
            node.put(API_SERVER_FQDN, entity.apiServerFqdn());
        }

        if (entity.controllerIp() != null) {
            node.put(CONTROLLER_IP, entity.controllerIp().toString());
        }

        return node;
    }

    @Override
    public KubevirtApiConfig decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        KubevirtApiConfig.Scheme scheme = KubevirtApiConfig.Scheme.valueOf(nullIsIllegal(
                json.get(SCHEME).asText(), SCHEME + MISSING_MESSAGE));
        IpAddress ipAddress = IpAddress.valueOf(nullIsIllegal(
                json.get(IP_ADDRESS).asText(), IP_ADDRESS + MISSING_MESSAGE));
        int port = json.get(PORT).asInt();

        KubevirtApiConfig.Builder builder = DefaultKubevirtApiConfig.builder()
                .scheme(scheme)
                .ipAddress(ipAddress)
                .port(port)
                .state(DISCONNECTED);

        JsonNode tokenJson = json.get(TOKEN);
        JsonNode caCertDataJson = json.get(CA_CERT_DATA);
        JsonNode clientCertDataJson = json.get(CLIENT_CERT_DATA);
        JsonNode clientKeyDataJson = json.get(CLIENT_KEY_DATA);
        JsonNode serviceFqdn = json.get(SERVICE_FQDN);
        JsonNode apiServerFqdn = json.get(API_SERVER_FQDN);
        JsonNode controllerIp = json.get(CONTROLLER_IP);
        JsonNode datacenterId = json.get(DATACENTER_ID);
        JsonNode clusterId = json.get(CLUSTER_ID);

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

        if (serviceFqdn != null) {
            builder.serviceFqdn(serviceFqdn.asText());
        }

        if (apiServerFqdn != null) {
            builder.apiServerFqdn(apiServerFqdn.asText());
        }

        if (controllerIp != null) {
            builder.controllerIp(IpAddress.valueOf(controllerIp.asText()));
        }

        if (datacenterId != null) {
            builder.datacenterId(datacenterId.asText());
        }

        if (clusterId != null) {
            builder.clusterId(clusterId.asText());
        }

        return builder.build();
    }
}
