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
package org.onosproject.openstackvtap.gui;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.Set;
import java.util.Iterator;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapAdminService;
import org.onosproject.openstackvtap.impl.DefaultOpenstackVtapCriterion;

import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getProtocolTypeFromString;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getVtapTypeFromString;


import static org.onosproject.net.HostId.hostId;

/**
 * Message handler for Openstack Vtap related messages.
 */
public class OpenstackVtapViewMessageHandler extends UiMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String OSV_IS_ACTIVATED_REQ = "openstackVtapIsActivatedRequest";
    private static final String OSV_IS_ACTIVATED_RESP = "openstackVtapIsActivatedResponse";
    private static final String OSV_CREATE_REQ = "openstackVtapCreateRequest";
    private static final String OSV_CREATE_RESP = "openstackVtapCreateResponse";

    private static final String SOURCE = "src";
    private static final String DESTINATION = "dst";
    private static final String SOURCE_IP = "srcIp";
    private static final String DESTINATION_IP = "dstIp";
    private static final String SOURCE_TRANSPORT_PORT = "srcPort";
    private static final String DESTINATION_TRANSPORT_PORT = "dstPort";
    private static final String SOURCE_HOST_NAME = "srcName";
    private static final String DESTINATION_HOST_NAME = "dstName";
    private static final String IP_PROTOCOL = "ipProto";
    private static final String VTAP_TYPE = "vtapType";
    private static final String IP_PROTOCOL_LIST = "ipProtoList";
    private static final String VTAP_TYPE_LIST = "vtapTypeList";

    private static final String RESULT = "result";
    private static final String VALUE = "value";
    private static final String SUCCESS = "Success";
    private static final String FAILED = "Failed";
    private static final String INVALID_VTAP_TYPE = "Invalid vtap type";
    private static final String FAILED_TO_CREATE_VTAP = "Failed to create OpenstackVtap";
    private static final String WRONG_IP_ADDRESS =
            "Inputted valid source & destination IP in CIDR (e.g., \"10.1.0.4/32\")";
    private static final String INVALID_TRANSPORT_PORT =
            "Invalid source & destination transport port has been entered";

    private static final String[] IP_PROTOCOL_ARRAY = {"Any", "TCP", "UDP", "ICMP"};
    private static final String[] VTAP_TYPE_ARRAY = {"All", "RX", "TX"};

    private HostService hostService;
    private OpenstackVtapAdminService vtapService;

    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);

        hostService = directory.get(HostService.class);
        vtapService = directory.get(OpenstackVtapAdminService.class);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new VtapIsActivatedRequestHandler(),
                new VtapCreateRequestHandler()
        );

    }

    private final class VtapIsActivatedRequestHandler extends RequestHandler {

        private VtapIsActivatedRequestHandler() {
            super(OSV_IS_ACTIVATED_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            String srcId = string(payload, SOURCE, null);
            String dstId = string(payload, DESTINATION, null);

            if (srcId != null && dstId != null) {
                HostId sHostId = hostId(srcId);
                HostId dHostId = hostId(dstId);

                Host sHost = hostService.getHost(sHostId);
                Host dHost = hostService.getHost(dHostId);

                if (sHost != null && dHost != null) {
                    ArrayNode ipProtos = arrayNode();
                    ArrayNode types = arrayNode();
                    String sHostName = ipForHost(sHost);
                    String dHostName = ipForHost(dHost);

                    for (String proto : IP_PROTOCOL_ARRAY) {
                        ipProtos.add(proto);
                    }

                    for (String type : VTAP_TYPE_ARRAY) {
                        types.add(type);
                    }

                    payload.put(SOURCE_HOST_NAME, sHostName);
                    payload.put(DESTINATION_HOST_NAME, dHostName);
                    payload.put(IP_PROTOCOL_LIST, ipProtos);
                    payload.put(VTAP_TYPE_LIST, types);

                    sendMessage(OSV_IS_ACTIVATED_RESP, payload);
                }
            }
        }

        // Returns the first of the given host's set of IP addresses as a string.
        private String ipForHost(Host host) {
            Set<IpAddress> ipAddresses = host.ipAddresses();
            Iterator<IpAddress> it = ipAddresses.iterator();
            return it.hasNext() ? it.next().toString() + "/32" : "unknown";
        }
    }

    private final class VtapCreateRequestHandler extends RequestHandler {
        private String srcIp;
        private String dstIp;
        private String ipProto;
        private String srcTpPort;
        private String dstTpPort;
        private String vtapTypeStr;
        private ObjectNode result = objectNode();

        private VtapCreateRequestHandler() {
            super(OSV_CREATE_REQ);
        }

        @Override
        public void process(ObjectNode payload) {
            srcIp = string(payload, SOURCE_IP, null);
            dstIp = string(payload, DESTINATION_IP, null);
            ipProto = string(payload, IP_PROTOCOL, null);
            srcTpPort = string(payload, SOURCE_TRANSPORT_PORT, null);
            dstTpPort = string(payload, DESTINATION_TRANSPORT_PORT, null);
            vtapTypeStr = string(payload, VTAP_TYPE, null);
            log.trace("VtapCreateRequestHandler payload srcIp:{}, dstIp:{}, ipPro:{}, " +
                            "srcTpPort:{}, dstTpPort:{}, vtapType:{}", srcIp, dstIp, ipProto,
                    srcTpPort, dstTpPort, vtapTypeStr);

            DefaultOpenstackVtapCriterion.Builder vtapCriterionBuilder = DefaultOpenstackVtapCriterion.builder();
            if (makeCriterion(vtapCriterionBuilder)) {
                OpenstackVtap.Type type = getVtapTypeFromString(vtapTypeStr.toLowerCase());
                if (type == null) {
                    log.warn(INVALID_VTAP_TYPE);
                    result.put(RESULT, FAILED);
                    result.put(VALUE, INVALID_VTAP_TYPE);
                    sendMessage(OSV_CREATE_RESP, result);
                    return;
                }

                OpenstackVtap vtap = vtapService.createVtap(type, vtapCriterionBuilder.build());
                if (vtap != null) {
                    log.info("Created OpenstackVtap with id {}", vtap.id().toString());
                    result.put(RESULT, SUCCESS);
                    result.put(VALUE, "vtap id: " + vtap.id().toString());
                } else {
                    log.warn(FAILED_TO_CREATE_VTAP);
                    result.put(RESULT, FAILED);
                    result.put(VALUE, FAILED_TO_CREATE_VTAP);
                }
            }
            sendMessage(OSV_CREATE_RESP, result);
        }

        private boolean makeCriterion(DefaultOpenstackVtapCriterion.Builder vtapCriterionBuilder) {
            try {
                vtapCriterionBuilder.srcIpPrefix(IpPrefix.valueOf(srcIp));
                vtapCriterionBuilder.dstIpPrefix(IpPrefix.valueOf(dstIp));
            } catch (Exception e) {
                log.warn(WRONG_IP_ADDRESS);
                result.put(RESULT, FAILED);
                result.put(VALUE, WRONG_IP_ADDRESS);
                return false;
            }

            vtapCriterionBuilder.ipProtocol(getProtocolTypeFromString(ipProto.toLowerCase()));

            try {
                vtapCriterionBuilder.srcTpPort(TpPort.tpPort(Integer.valueOf(srcTpPort)));
                vtapCriterionBuilder.dstTpPort(TpPort.tpPort(Integer.valueOf(dstTpPort)));
            } catch (Exception e) {
                log.warn(INVALID_TRANSPORT_PORT);
                result.put(RESULT, FAILED);
                result.put(VALUE, INVALID_TRANSPORT_PORT);
                return false;
            }

            return true;
        }
    }
}
