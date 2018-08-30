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

package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.onlab.packet.BasePacket;
import org.onlab.packet.Data;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TCP;
import org.onlab.packet.TpPort;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Port;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Objects;

import static org.onosproject.openstacknetworking.api.Constants.DHCP_ARP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_DHCP_RULE;
import static org.onosproject.openstacknetworking.impl.OpenstackMetadataProxyHandler.Http.Type.RESPONSE;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.hmacEncrypt;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.parseHttpRequest;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.unparseHttpResponseBody;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.unparseHttpResponseHeader;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles metadata requests for the virtual instances.
 */
@Component(immediate = true)
public class OpenstackMetadataProxyHandler {
    protected final Logger log = getLogger(getClass());

    private static final String METADATA_SERVER_IP = "169.254.169.254";
    private static final int METADATA_SERVER_PORT = 8775;
    private static final int HTTP_SERVER_PORT = 80;
    private static final int PREFIX_LENGTH = 32;
    private static final short WINDOW_SIZE = (short) 0x1000;
    private static final short FIN_FLAG = (short) 0x01;
    private static final short SYN_FLAG = (short) 0x02;
    private static final short ACK_FLAG = (short) 0x10;
    private static final short SYN_ACK_FLAG = (short) 0x12;
    private static final short FIN_ACK_FLAG = (short) 0x11;
    private static final byte DATA_OFFSET = (byte) 0x5;
    private static final short URGENT_POINTER = (short) 0x1;
    private static final byte PACKET_TTL = (byte) 127;
    private static final String HTTP_PREFIX = "http://";
    private static final String COLON = ":";

    private static final String INSTANCE_ID_HEADER = "X-Instance-ID";
    private static final String INSTANCE_ID_SIGNATURE_HEADER = "X-Instance-ID-Signature";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private static final String HTTP_GET_METHOD = "GET";
    private static final String HTTP_POST_METHOD = "POST";

    private static final String METADATA_SECRET = "metadataSecret";
    private static final String DEFAULT_METADATA_SECRET = "nova";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Property(name = METADATA_SECRET, value = DEFAULT_METADATA_SECRET,
            label = "Metadata secret")
    private String metadataSecret = DEFAULT_METADATA_SECRET;

    private final PacketProcessor packetProcessor = new InternalPacketProcessor();
    private final OpenstackNodeListener osNodeListener = new InternalNodeEventListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        configService.registerProperties(getClass());
        osNodeService.addListener(osNodeListener);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));
        leadershipService.runForLeadership(appId.name());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        configService.unregisterProperties(getClass(), false);
        osNodeService.removeListener(osNodeListener);
        leadershipService.withdraw(appId.name());

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        String updatedMetadataSecret;

        updatedMetadataSecret = Tools.get(properties, METADATA_SECRET);

        if (!Strings.isNullOrEmpty(updatedMetadataSecret) &&
                !updatedMetadataSecret.equals(metadataSecret)) {
            metadataSecret = updatedMetadataSecret;
        }

        log.info("Modified");
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            Ethernet ethPacket = context.inPacket().parsed();
            if (ethPacket == null || ethPacket.getEtherType() != Ethernet.TYPE_IPV4) {
                return;
            }

            IPv4 ipv4Packet = (IPv4) ethPacket.getPayload();
            if (ipv4Packet.getProtocol() != IPv4.PROTOCOL_TCP ||
                    !IpAddress.valueOf(ipv4Packet.getDestinationAddress()).
                            equals(IpAddress.valueOf(METADATA_SERVER_IP))) {
                return;
            }

            TCP tcpPacket = (TCP) ipv4Packet.getPayload();
            if (tcpPacket.getDestinationPort() != HTTP_SERVER_PORT) {
                return;
            }

            if (tcpPacket.getFlags() == SYN_FLAG) {
                Ethernet ethReply = buildTcpSynAckPacket(ethPacket, ipv4Packet, tcpPacket);
                sendReply(context, ethReply);
                return;
            }

            if (tcpPacket.getFlags() == FIN_ACK_FLAG) {
                Ethernet ackReply = buildTcpAckPacket(ethPacket, ipv4Packet, tcpPacket);
                sendReply(context, ackReply);
                Ethernet finAckReply = buildTcpFinAckPacket(ethPacket, ipv4Packet, tcpPacket);
                sendReply(context, finAckReply);
                return;
            }

            Data data = (Data) tcpPacket.getPayload();
            byte[] byteData = data.getData();

            if (byteData.length != 0) {
                HttpRequest request = parseHttpRequest(byteData);
                ConnectPoint cp = context.inPacket().receivedFrom();
                InstancePort instPort = instancePortService.instancePort(cp.deviceId(), cp.port());

                if (instPort == null || request == null) {
                    log.warn("Cannot send metadata request due to lack of information");
                    return;
                }

                // attempt to send HTTP request to the meta-data server (nova-api),
                // obtain the HTTP response
                CloseableHttpResponse proxyResponse = proxyHttpRequest(request, instPort);

                if (proxyResponse == null) {
                    log.warn("No response was received from metadata server");
                    return;
                }

                HttpResponse response = new BasicHttpResponse(proxyResponse.getStatusLine());
                response.setEntity(proxyResponse.getEntity());
                response.setHeaders(proxyResponse.getAllHeaders());

                Http httpResponse = new Http();
                httpResponse.setType(RESPONSE);
                httpResponse.setMessage(response);

                TCP tcpReply = buildTcpDataPacket(tcpPacket, byteData.length, response);
                Ethernet ethReply = buildEthFrame(ethPacket, ipv4Packet, tcpReply);
                sendReply(context, ethReply);

                try {
                    proxyResponse.close();
                } catch (IOException e) {
                    log.warn("Failed to close the response connection due to {}", e);
                }
            }
        }

        /**
         * Builds an ethernet frame contains TCP sync-ack packet generated
         * from the given TCP sync request packet.
         *
         * @param ethRequest    ethernet request frame
         * @param ipv4Request   IPv4 request
         * @param tcpRequest    TCP request
         * @return an ethernet frame contains newly generated TCP reply
         */
        private Ethernet buildTcpSynAckPacket(Ethernet ethRequest,
                                              IPv4 ipv4Request, TCP tcpRequest) {

            TCP tcpReply = buildTcpSignalPacket(tcpRequest, tcpRequest.getSequence(),
                    tcpRequest.getSequence() + 1, SYN_ACK_FLAG);

            return buildEthFrame(ethRequest, ipv4Request, tcpReply);
        }

        /**
         * Builds a TCP ACK packet receiving SYN packet.
         *
         * @param ethRequest    ethernet request frame
         * @param ipv4Request   IPv4 request
         * @param tcpRequest    TCP request
         * @return an ethernet frame contains newly generated TCP reply
         */
        private Ethernet buildTcpAckPacket(Ethernet ethRequest,
                                           IPv4 ipv4Request, TCP tcpRequest) {
            TCP tcpReply = buildTcpSignalPacket(tcpRequest, tcpRequest.getAcknowledge(),
                    tcpRequest.getSequence() + 1, ACK_FLAG);

            return buildEthFrame(ethRequest, ipv4Request, tcpReply);
        }

        /**
         * Builds a TCP FIN-ACK packet receiving FIN-ACK packet.
         *
         * @param ethRequest    ethernet request frame
         * @param ipv4Request   IPv4 request
         * @param tcpRequest    TCP request
         * @return an ethernet frame contains newly generated TCP reply
         */
        private Ethernet buildTcpFinAckPacket(Ethernet ethRequest,
                                              IPv4 ipv4Request, TCP tcpRequest) {
            TCP tcpReply = buildTcpSignalPacket(tcpRequest, tcpRequest.getAcknowledge(),
                    tcpRequest.getSequence() + 1, FIN_ACK_FLAG);

            return buildEthFrame(ethRequest, ipv4Request, tcpReply);
        }

        /**
         * Builds a TCP signaling packet.
         *
         * @param tcpRequest    TCP request
         * @param seq           sequence number
         * @param ack           ack number
         * @param flags         TCP flags
         * @return TCP signal packet
         */
        private TCP buildTcpSignalPacket(TCP tcpRequest, int seq, int ack, short flags) {
            TCP tcpReply = new TCP();
            tcpReply.setSourcePort(tcpRequest.getDestinationPort());
            tcpReply.setDestinationPort(tcpRequest.getSourcePort());
            tcpReply.setSequence(seq);
            tcpReply.setAcknowledge(ack);
            tcpReply.setDataOffset(DATA_OFFSET);
            tcpReply.setFlags(flags);
            tcpReply.setWindowSize(WINDOW_SIZE);
            tcpReply.setUrgentPointer(URGENT_POINTER);

            return tcpReply;
        }

        /**
         * Builds a TCP data packet.
         *
         * @param tcpRequest    TCP request
         * @param requestLength TCP request data length
         * @param response      HTTP response
         * @return a TCP data packet
         */
        private TCP buildTcpDataPacket(TCP tcpRequest, int requestLength,
                                       HttpResponse response) {
            TCP tcpReply = new TCP();
            tcpReply.setSourcePort(tcpRequest.getDestinationPort());
            tcpReply.setDestinationPort(tcpRequest.getSourcePort());
            tcpReply.setSequence(tcpRequest.getAcknowledge());
            tcpReply.setAcknowledge(tcpRequest.getSequence() + requestLength);
            tcpReply.setDataOffset(DATA_OFFSET);        // no options
            tcpReply.setFlags(ACK_FLAG);
            tcpReply.setWindowSize(WINDOW_SIZE);
            tcpReply.setUrgentPointer(URGENT_POINTER);

            Http httpResponse = new Http();
            httpResponse.setType(RESPONSE);
            httpResponse.setMessage(response);

            tcpReply.setPayload(httpResponse);

            return tcpReply;
        }

        /**
         * Builds an ethernet frame with the given IPv4 and TCP payload.
         *
         * @param ethRequest    ethernet request frame
         * @param ipv4Request   IPv4 request
         * @param tcpReply      TCP reply
         * @return an ethernet frame contains TCP payload
         */
        private Ethernet buildEthFrame(Ethernet ethRequest, IPv4 ipv4Request,
                                       TCP tcpReply) {
            Ethernet ethReply = new Ethernet();
            ethReply.setSourceMACAddress(ethRequest.getDestinationMAC());
            ethReply.setDestinationMACAddress(ethRequest.getSourceMAC());
            ethReply.setEtherType(ethRequest.getEtherType());

            IPv4 ipv4Reply = new IPv4();
            ipv4Reply.setSourceAddress(ipv4Request.getDestinationAddress());
            ipv4Reply.setDestinationAddress(ipv4Request.getSourceAddress());
            ipv4Reply.setTtl(PACKET_TTL);

            ipv4Reply.setPayload(tcpReply);
            ethReply.setPayload(ipv4Reply);

            return ethReply;
        }

        /**
         * Proxyies HTTP request.
         *
         * @param oldRequest    HTTP request
         * @param instPort      instance port
         * @return HTTP response
         */
        private CloseableHttpResponse proxyHttpRequest(HttpRequest oldRequest,
                                                       InstancePort instPort) {

            CloseableHttpClient client = HttpClientBuilder.create().build();
            OpenstackNode controller = osNodeService.completeNodes(CONTROLLER).
                    stream().findFirst().orElse(null);
            if (controller == null) {
                return null;
            }

            String path = oldRequest.getRequestLine().getUri();
            String url = HTTP_PREFIX + controller.managementIp().toString() +
                    COLON + METADATA_SERVER_PORT + path;

            if (StringUtils.isEmpty(url)) {
                log.warn("The metadata endpoint is not configured!");
                return null;
            }

            log.info("Sending request to metadata endpoint {}...", url);

            HttpRequestBase request;

            switch (oldRequest.getRequestLine().getMethod()) {
                case HTTP_GET_METHOD:
                    request = new HttpGet(url);
                    break;
                case HTTP_POST_METHOD:
                    request = new HttpPost(url);
                    HttpEntityEnclosingRequest entityRequest =
                            (HttpEntityEnclosingRequest) oldRequest;
                    ((HttpPost) request).setEntity(entityRequest.getEntity());
                    break;
                default:
                    request = new HttpGet(url);
                    break;
            }

            // configure headers from original HTTP request
            for (Header header : oldRequest.getAllHeaders()) {
                request.addHeader(header);
            }

            request.setProtocolVersion(oldRequest.getProtocolVersion());

            Port port = osNetworkService.port(instPort.portId());

            request.addHeader(new BasicHeader(INSTANCE_ID_HEADER, port.getDeviceId()));
            request.addHeader(new BasicHeader(INSTANCE_ID_SIGNATURE_HEADER,
                    hmacEncrypt(metadataSecret, port.getDeviceId())));
            request.addHeader(new BasicHeader(TENANT_ID_HEADER, port.getTenantId()));
            request.addHeader(new BasicHeader(
                    FORWARDED_FOR_HEADER, instPort.ipAddress().toString()));

            try {
                return client.execute(request);
            } catch (IOException e) {
                log.warn("Failed to get response from metadata server due to {}", e);
            }

            return null;
        }

        /**
         * Sends out ethernet frame.
         *
         * @param context   packet context
         * @param ethReply  ethernet frame
         */
        private void sendReply(PacketContext context, Ethernet ethReply) {
            if (ethReply == null) {
                return;
            }
            ConnectPoint srcPoint = context.inPacket().receivedFrom();
            TrafficTreatment treatment = DefaultTrafficTreatment
                    .builder()
                    .setOutput(srcPoint.port())
                    .build();

            packetService.emit(new DefaultOutboundPacket(
                    srcPoint.deviceId(),
                    treatment,
                    ByteBuffer.wrap(ethReply.serialize())));
            context.block();
        }
    }

    private class InternalNodeEventListener implements OpenstackNodeListener {
        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNodeId, leader) &&
                    event.subject().type() == COMPUTE;
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();
            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    setMetadataRule(osNode, true);
                    break;
                case OPENSTACK_NODE_INCOMPLETE:
                    setMetadataRule(osNode, false);
                    break;
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_REMOVED:
                default:
                    break;
            }
        }

        /**
         * Installs metadata rule for receiving all metadata request packets.
         *
         * @param osNode    openstack node
         * @param install   installation flag
         */
        private void setMetadataRule(OpenstackNode osNode, boolean install) {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPProtocol(IPv4.PROTOCOL_TCP)
                    .matchIPDst(IpPrefix.valueOf(
                            IpAddress.valueOf(METADATA_SERVER_IP), PREFIX_LENGTH))
                    .matchTcpDst(TpPort.tpPort(HTTP_SERVER_PORT))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .punt()
                    .build();

            osFlowRuleService.setRule(
                    appId,
                    osNode.intgBridge(),
                    selector,
                    treatment,
                    PRIORITY_DHCP_RULE,
                    DHCP_ARP_TABLE,
                    install);
        }
    }

    /**
     * Implements Http packet format.
     */
    protected static class Http extends BasePacket {

        public enum Type {

            /**
             * Signifies that this is a Http REQUEST packet.
             */
            REQUEST,

            /**
             * Signifies that this is a Http RESPONSE packet.
             */
            RESPONSE,
        }

        private Type type;
        private HttpMessage message;

        /**
         * Obtains the Http type.
         *
         * @return Http type
         */
        public Type getType() {
            return type;
        }

        /**
         * Configures the Http type.
         *
         * @param type Http type
         */
        public void setType(Type type) {
            this.type = type;
        }

        /**
         * Obtains the Http message.
         *
         * @return Http message
         */
        public HttpMessage getMessage() {
            return message;
        }

        /**
         * Configures the Http message.
         *
         * @param message Http message
         */
        public void setMessage(HttpMessage message) {
            this.message = message;
        }

        @Override
        public byte[] serialize() {
            if (type == RESPONSE) {

                byte[] header = unparseHttpResponseHeader((HttpResponse) message);
                byte[] body = unparseHttpResponseBody((HttpResponse) message);

                if (header == null || body == null) {
                    return new byte[0];
                }

                final byte[] data = new byte[header.length + body.length];
                final ByteBuffer bb = ByteBuffer.wrap(data);
                bb.put(header);
                bb.put(body);

                return data;
            }
            return new byte[0];
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            if (!super.equals(o)) {
                return false;
            }

            Http http = (Http) o;
            return type == http.type &&
                    com.google.common.base.Objects.equal(message, http.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, message);
        }
    }
}