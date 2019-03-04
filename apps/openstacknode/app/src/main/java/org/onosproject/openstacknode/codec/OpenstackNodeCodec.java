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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.openstacknode.api.DefaultKeystoneConfig;
import org.onosproject.openstacknode.api.DefaultOpenstackNode;
import org.onosproject.openstacknode.api.DpdkConfig;
import org.onosproject.openstacknode.api.KeystoneConfig;
import org.onosproject.openstacknode.api.NeutronConfig;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackAuth;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackPhyInterface;
import org.onosproject.openstacknode.api.OpenstackSshAuth;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.openstacknode.api.Constants.CONTROLLER;
import static org.onosproject.openstacknode.api.Constants.DATA_IP;
import static org.onosproject.openstacknode.api.Constants.GATEWAY;
import static org.onosproject.openstacknode.api.Constants.HOST_NAME;
import static org.onosproject.openstacknode.api.Constants.MANAGEMENT_IP;
import static org.onosproject.openstacknode.api.Constants.UPLINK_PORT;
import static org.onosproject.openstacknode.api.Constants.VLAN_INTF_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Openstack node codec used for serializing and de-serializing JSON string.
 */
public final class OpenstackNodeCodec extends JsonCodec<OpenstackNode> {

    private final Logger log = getLogger(getClass());

    private static final String TYPE = "type";
    private static final String INTEGRATION_BRIDGE = "integrationBridge";
    private static final String STATE = "state";
    private static final String PHYSICAL_INTERFACES = "phyIntfs";
    private static final String CONTROLLERS = "controllers";
    private static final String KEYSTONE_CONFIG = "keystoneConfig";
    private static final String ENDPOINT = "endpoint";
    private static final String AUTHENTICATION = "authentication";
    private static final String NEUTRON_CONFIG = "neutronConfig";
    private static final String SSH_AUTH = "sshAuth";
    private static final String DPDK_CONFIG = "dpdkConfig";

    private static final String MISSING_MESSAGE = " is required in OpenstackNode";

    @Override
    public ObjectNode encode(OpenstackNode node, CodecContext context) {
        checkNotNull(node, "Openstack node cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(HOST_NAME, node.hostname())
                .put(TYPE, node.type().name())
                .put(STATE, node.state().name())
                .put(MANAGEMENT_IP, node.managementIp().toString());

        OpenstackNode.NodeType type = node.type();

        // serialize uplink port only for gateway node
        if (type == OpenstackNode.NodeType.GATEWAY) {
            result.put(UPLINK_PORT, node.uplinkPort());
        }

        // serialize keystone config for controller node
        if (type == OpenstackNode.NodeType.CONTROLLER) {

            ObjectNode keystoneConfigJson = context.codec(KeystoneConfig.class)
                    .encode(node.keystoneConfig(), context);

            result.set(KEYSTONE_CONFIG, keystoneConfigJson);

            // serialize neutron config for controller node
            if (node.neutronConfig() != null) {
                ObjectNode neutronConfigJson = context.codec(NeutronConfig.class)
                        .encode(node.neutronConfig(), context);

                result.set(NEUTRON_CONFIG, neutronConfigJson);
            }
        }

        // serialize integration bridge config
        if (node.intgBridge() != null) {
            result.put(INTEGRATION_BRIDGE, node.intgBridge().toString());
        }

        // serialize VLAN interface, it is valid only if any VLAN interface presents
        if (node.vlanIntf() != null) {
            result.put(VLAN_INTF_NAME, node.vlanIntf());
        }

        // serialize data IP only if it presents
        if (node.dataIp() != null) {
            result.put(DATA_IP, node.dataIp().toString());
        }

        // serialize physical interfaces, it is valid only if any of physical interface presents
        if (node.phyIntfs() != null && !node.phyIntfs().isEmpty()) {
            ArrayNode phyIntfs = context.mapper().createArrayNode();
            node.phyIntfs().forEach(phyIntf -> {
                ObjectNode phyIntfJson =
                        context.codec(OpenstackPhyInterface.class).encode(phyIntf, context);
                phyIntfs.add(phyIntfJson);
            });
            result.set(PHYSICAL_INTERFACES, phyIntfs);
        }

        // serialize controllers, it is valid only if any of controller presents
        if (node.controllers() != null && !node.controllers().isEmpty()) {
            ArrayNode controllers = context.mapper().createArrayNode();
            node.controllers().forEach(controller -> {
                ObjectNode controllerJson =
                        context.codec(ControllerInfo.class).encode(controller, context);
                controllers.add(controllerJson);
            });
            result.set(CONTROLLERS, controllers);
        }

        // serialize SSH authentication info, it is valid only if auth info presents
        if (node.sshAuthInfo() != null) {
            ObjectNode sshAuthJson = context.codec(OpenstackSshAuth.class)
                    .encode(node.sshAuthInfo(), context);
            result.set(SSH_AUTH, sshAuthJson);
        }

        // serialize DPDK config, it is valid only if dpdk config presents
        if (node.dpdkConfig() != null) {
            ObjectNode dpdkConfigJson = context.codec(DpdkConfig.class)
                    .encode(node.dpdkConfig(), context);
            result.set(DPDK_CONFIG, dpdkConfigJson);
        }

        return result;
    }

    @Override
    public OpenstackNode decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String hostname = nullIsIllegal(json.get(HOST_NAME).asText(),
                HOST_NAME + MISSING_MESSAGE);
        String type = nullIsIllegal(json.get(TYPE).asText(),
                TYPE + MISSING_MESSAGE);
        String mIp = nullIsIllegal(json.get(MANAGEMENT_IP).asText(),
                MANAGEMENT_IP + MISSING_MESSAGE);

        DefaultOpenstackNode.Builder nodeBuilder = DefaultOpenstackNode.builder()
                .hostname(hostname)
                .type(OpenstackNode.NodeType.valueOf(type))
                .state(NodeState.INIT)
                .managementIp(IpAddress.valueOf(mIp));

        if (type.equals(GATEWAY)) {
            nodeBuilder.uplinkPort(nullIsIllegal(json.get(UPLINK_PORT).asText(),
                    UPLINK_PORT + MISSING_MESSAGE));
        }
        if (type.equals(CONTROLLER)) {

            JsonNode keystoneConfigJson = json.get(KEYSTONE_CONFIG);

            KeystoneConfig keystoneConfig;
            if (keystoneConfigJson != null) {
                final JsonCodec<KeystoneConfig> keystoneConfigCodec =
                                        context.codec(KeystoneConfig.class);
                keystoneConfig = keystoneConfigCodec.decode((ObjectNode)
                                        keystoneConfigJson.deepCopy(), context);
            } else {
                JsonNode authJson = json.get(AUTHENTICATION);
                final JsonCodec<OpenstackAuth> authCodec = context.codec(OpenstackAuth.class);
                OpenstackAuth auth = authCodec.decode((ObjectNode) authJson.deepCopy(), context);

                String endpoint = nullIsIllegal(json.get(ENDPOINT).asText(),
                        ENDPOINT + MISSING_MESSAGE);

                keystoneConfig = DefaultKeystoneConfig.builder()
                        .authentication(auth)
                        .endpoint(endpoint)
                        .build();
            }

            nodeBuilder.keystoneConfig(keystoneConfig);
        }
        if (json.get(VLAN_INTF_NAME) != null) {
            nodeBuilder.vlanIntf(json.get(VLAN_INTF_NAME).asText());
        }
        if (json.get(DATA_IP) != null) {
            nodeBuilder.dataIp(IpAddress.valueOf(json.get(DATA_IP).asText()));
        }

        JsonNode intBridgeJson = json.get(INTEGRATION_BRIDGE);
        if (intBridgeJson != null) {
            nodeBuilder.intgBridge(DeviceId.deviceId(intBridgeJson.asText()));
        }

        // parse physical interfaces
        List<OpenstackPhyInterface> phyIntfs = new ArrayList<>();
        JsonNode phyIntfsJson = json.get(PHYSICAL_INTERFACES);
        if (phyIntfsJson != null) {

            final JsonCodec<OpenstackPhyInterface>
                    phyIntfCodec = context.codec(OpenstackPhyInterface.class);

            IntStream.range(0, phyIntfsJson.size()).forEach(i -> {
                ObjectNode intfJson = get(phyIntfsJson, i);
                phyIntfs.add(phyIntfCodec.decode(intfJson, context));
            });
        }
        nodeBuilder.phyIntfs(phyIntfs);

        // parse customized controllers
        List<ControllerInfo> controllers = new ArrayList<>();
        JsonNode controllersJson = json.get(CONTROLLERS);
        if (controllersJson != null) {

            final JsonCodec<ControllerInfo>
                    controllerCodec = context.codec(ControllerInfo.class);

            IntStream.range(0, controllersJson.size()).forEach(i -> {
                ObjectNode controllerJson = get(controllersJson, i);
                controllers.add(controllerCodec.decode(controllerJson, context));
            });
        }
        nodeBuilder.controllers(controllers);

        // parse neutron config
        JsonNode neutronConfigJson = json.get(NEUTRON_CONFIG);
        if (neutronConfigJson != null) {
            final JsonCodec<NeutronConfig> neutronConfigJsonCodec =
                                context.codec(NeutronConfig.class);

            NeutronConfig neutronConfig =
                    neutronConfigJsonCodec.decode((ObjectNode)
                            neutronConfigJson.deepCopy(), context);
            nodeBuilder.neutronConfig(neutronConfig);
        }

        // parse ssh authentication
        JsonNode sshAuthJson = json.get(SSH_AUTH);
        if (sshAuthJson != null) {
            final JsonCodec<OpenstackSshAuth> sshAuthJsonCodec =
                                context.codec(OpenstackSshAuth.class);

            OpenstackSshAuth sshAuth = sshAuthJsonCodec.decode((ObjectNode)
                            sshAuthJson.deepCopy(), context);
            nodeBuilder.sshAuthInfo(sshAuth);
        }

        // parse DPDK configuration
        JsonNode dpdkConfigJson = json.get(DPDK_CONFIG);
        if (dpdkConfigJson != null) {
            final JsonCodec<DpdkConfig> dpdkConfigJsonCodec =
                                context.codec(DpdkConfig.class);

            DpdkConfig dpdkConfig = dpdkConfigJsonCodec.decode((ObjectNode)
                                dpdkConfigJson.deepCopy(), context);
            nodeBuilder.dpdkConfig(dpdkConfig);
        }

        log.trace("node is {}", nodeBuilder.build().toString());

        return nodeBuilder.build();
    }
}
