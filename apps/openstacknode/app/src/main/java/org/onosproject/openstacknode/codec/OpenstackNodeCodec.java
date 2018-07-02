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
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackAuth;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackPhyInterface;
import org.onosproject.openstacknode.api.DefaultOpenstackNode;
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
    private static final String AUTHENTICATION = "authentication";
    private static final String END_POINT = "endPoint";

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

        if (type == OpenstackNode.NodeType.GATEWAY) {
            result.put(UPLINK_PORT, node.uplinkPort());
        }

        if (type != OpenstackNode.NodeType.CONTROLLER) {
            result.put(INTEGRATION_BRIDGE, node.intgBridge().toString());
        } else {
            result.put(END_POINT, node.endPoint());
        }

        if (node.vlanIntf() != null) {
            result.put(VLAN_INTF_NAME, node.vlanIntf());
        }

        if (node.dataIp() != null) {
            result.put(DATA_IP, node.dataIp().toString());
        }

        // TODO: need to find a way to not refer to ServiceDirectory from
        // DefaultOpenstackNode

        ArrayNode phyIntfs = context.mapper().createArrayNode();
        node.phyIntfs().forEach(phyIntf -> {
            ObjectNode phyIntfJson = context.codec(OpenstackPhyInterface.class).encode(phyIntf, context);
            phyIntfs.add(phyIntfJson);
        });
        result.set(PHYSICAL_INTERFACES, phyIntfs);

        if (node.authentication() != null) {
            ObjectNode authJson = context.codec(OpenstackAuth.class)
                    .encode(node.authentication(), context);
            result.put(AUTHENTICATION, authJson);
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
        if (!type.equals(CONTROLLER)) {
            String iBridge = nullIsIllegal(json.get(INTEGRATION_BRIDGE).asText(),
                    INTEGRATION_BRIDGE + MISSING_MESSAGE);
            nodeBuilder.intgBridge(DeviceId.deviceId(iBridge));
        } else {
            String endPoint = nullIsIllegal(json.get(END_POINT).asText(),
                    END_POINT + MISSING_MESSAGE);
            nodeBuilder.endPoint(endPoint);
        }
        if (json.get(VLAN_INTF_NAME) != null) {
            nodeBuilder.vlanIntf(json.get(VLAN_INTF_NAME).asText());
        }
        if (json.get(DATA_IP) != null) {
            nodeBuilder.dataIp(IpAddress.valueOf(json.get(DATA_IP).asText()));
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

        // parse authentication
        JsonNode authJson = json.get(AUTHENTICATION);
        if (json.get(AUTHENTICATION) != null) {

            final JsonCodec<OpenstackAuth> authCodec = context.codec(OpenstackAuth.class);

            OpenstackAuth auth = authCodec.decode((ObjectNode) authJson.deepCopy(), context);
            nodeBuilder.authentication(auth);
        }

        log.trace("node is {}", nodeBuilder.build().toString());

        return nodeBuilder.build();
    }
}
