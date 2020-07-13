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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.DHCP_TABLE;
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
    private static final short FIN_ACK_PUSH_FLAG = (short) 0x19;
    private static final byte DATA_OFFSET = (byte) 0x5;
    private static final short URGENT_POINTER = (short) 0x1;
    private static final byte PACKET_TTL = (byte) 127;
    private static final String HTTP_PREFIX = "http://";
    private static final String COLON = ":";

    private static final int IP_HEADER_SIZE = 20;
    private static final int TCP_HEADER_SIZE = 20;

    private static final String INSTANCE_ID_HEADER = "X-Instance-ID";
    private static final String INSTANCE_ID_SIGNATURE_HEADER = "X-Instance-ID-Signature";
    private static final String TENANT_ID_HEADER = "X-Tenant-ID";
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private static final String HTTP_GET_METHOD = "GET";
    private static final String HTTP_POST_METHOD = "POST";
    private static final String HTTP_PUT_METHOD = "PUT";
    private static final String HTTP_DELETE_METHOD = "DELETE";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackFlowRuleService osFlowRuleService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final PacketProcessor packetProcessor = new InternalPacketProcessor();
    private final OpenstackNodeListener osNodeListener = new InternalNodeEventListener();

    private Set<String> excludedHeaders = ImmutableSet.of("content-type", "content-length");

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        osNodeService.addListener(osNodeListener);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));
        leadershipService.runForLeadership(appId.name());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        osNodeService.removeListener(osNodeListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            if (context.isHandled()) {
                return;
            }

            // FIXME: need to find a way to spawn a new thread to check metadata proxy mode
            if (!useMetadataProxy()) {
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

            // (three-way handshaking)
            // reply TCP SYN-ACK packet with receiving TCP SYN packet
            if (tcpPacket.getFlags() == SYN_FLAG) {
                Ethernet ethReply = buildTcpSynAckPacket(ethPacket, ipv4Packet, tcpPacket);
                sendReply(context, ethReply);
                return;
            }

            // (four-way handshaking)
            // reply TCP ACK and TCP FIN-ACK packets with receiving TCP FIN-ACK packet
            if (tcpPacket.getFlags() == FIN_ACK_FLAG) {
                Ethernet ackReply = buildTcpAckPacket(ethPacket, ipv4Packet, tcpPacket);
                sendReply(context, ackReply);
                Ethernet finAckReply = buildTcpFinAckPacket(ethPacket, ipv4Packet, tcpPacket);
                sendReply(context, finAckReply);
                return;
            }

            // normal TCP data transmission
            Data data = (Data) tcpPacket.getPayload();
            byte[] byteData = data.getData();

            if (byteData.length != 0) {
                eventExecutor.execute(() -> {
                    processHttpRequest(context, ethPacket, ipv4Packet, tcpPacket, byteData);
                });
            }
        }

        private void processHttpRequest(PacketContext context, Ethernet ethPacket,
                                        IPv4 ipv4Packet, TCP tcpPacket, byte[] byteData) {
            HttpRequest request = parseHttpRequest(byteData);
            ConnectPoint cp = context.inPacket().receivedFrom();
            InstancePort instPort = instancePortService.instancePort(cp.deviceId(), cp.port());

            if (instPort == null || request == null) {
                log.warn("Cannot send metadata request due to lack of information");
                return;
            }

            // attempt to send HTTP request to the meta-data server (nova-api),
            // obtain the HTTP response, relay the response to VM through packet-out
            CloseableHttpResponse proxyResponse = proxyHttpRequest(request, instPort);

            if (proxyResponse == null) {
                log.warn("No response was received from metadata server");
                return;
            } else {
                log.debug("Metadata response headers {}", Arrays.toString(proxyResponse.getAllHeaders()));
                log.debug("Metadata response entity {}", proxyResponse.getEntity().toString());
            }

            HttpResponse response = new BasicHttpResponse(proxyResponse.getStatusLine());
            response.setEntity(proxyResponse.getEntity());
            response.setHeaders(proxyResponse.getAllHeaders());

            Network osNetwork = osNetworkService.network(instPort.networkId());
            int tcpPayloadSize = osNetwork.getMTU() - IP_HEADER_SIZE - TCP_HEADER_SIZE;

            List<TCP> tcpReplies = buildTcpDataPackets(tcpPacket,
                    byteData.length, response, tcpPayloadSize);
            List<Ethernet> ethReplies = buildEthFrames(ethPacket, ipv4Packet, tcpReplies);
            ethReplies.forEach(e -> sendReply(context, e));

            try {
                proxyResponse.close();
            } catch (IOException e) {
                log.warn("Failed to close the response connection due to {}", e);
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
        private List<TCP> buildTcpDataPackets(TCP tcpRequest, int requestLength,
                                              HttpResponse response, int payloadSize) {
            List<TCP> tcpReplies = Lists.newArrayList();

            Http httpResponse = new Http();
            httpResponse.setType(RESPONSE);
            httpResponse.setMessage(response);

            byte[] httpBytes = httpResponse.serialize();

            int numOfSegments = (int) Math.ceil((double) httpBytes.length / payloadSize);

            if (numOfSegments == 1) {
                TCP tcpReply = new TCP();
                tcpReply.setSourcePort(tcpRequest.getDestinationPort());
                tcpReply.setDestinationPort(tcpRequest.getSourcePort());
                tcpReply.setSequence(tcpRequest.getAcknowledge());
                tcpReply.setAcknowledge(tcpRequest.getSequence() + requestLength);
                tcpReply.setDataOffset(DATA_OFFSET);        // no options, 20 bytes
                tcpReply.setFlags(ACK_FLAG);
                tcpReply.setWindowSize(WINDOW_SIZE);
                tcpReply.setUrgentPointer(URGENT_POINTER);

                Data data = new Data(httpBytes);
                tcpReply.setPayload(data);

                tcpReplies.add(tcpReply);
            }

            if (numOfSegments > 1) {

                for (int i = 0; i < numOfSegments; i++) {

                    int byteStartIndex = i * payloadSize;
                    int byteEndIndex;

                    TCP tcpReply = new TCP();
                    tcpReply.setSourcePort(tcpRequest.getDestinationPort());
                    tcpReply.setDestinationPort(tcpRequest.getSourcePort());
                    tcpReply.setSequence(tcpRequest.getAcknowledge() + byteStartIndex);
                    tcpReply.setAcknowledge(tcpRequest.getSequence() + requestLength);
                    tcpReply.setDataOffset(DATA_OFFSET);        // no options, 20 bytes
                    tcpReply.setWindowSize(WINDOW_SIZE);
                    tcpReply.setUrgentPointer(URGENT_POINTER);

                    if (i == numOfSegments - 1) {
                        tcpReply.setFlags(FIN_ACK_PUSH_FLAG);
                        byteEndIndex = httpBytes.length;
                    } else {
                        tcpReply.setFlags(ACK_FLAG);
                        byteEndIndex = (i + 1) * payloadSize;
                    }

                    byte[] httpSegmentBytes = Arrays.copyOfRange(httpBytes,
                            byteStartIndex, byteEndIndex);

                    Data data = new Data(httpSegmentBytes);
                    tcpReply.setPayload(data);

                    tcpReplies.add(tcpReply);
                }
            }

            return tcpReplies;
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
         * Builds a set of ethernet frames with the given IPv4 and TCP payload.
         *
         * @param ethRequest    ethernet request frame
         * @param ipv4Request   IPv4 request
         * @param tcpReplies      TCP replies
         * @return a set of ethernet frames
         */
        private List<Ethernet> buildEthFrames(Ethernet ethRequest,
                                              IPv4 ipv4Request,
                                              List<TCP> tcpReplies) {

            List<Ethernet> ethReplies = Lists.newArrayList();

            for (TCP tcpReply : tcpReplies) {

                Ethernet ethReply = new Ethernet();
                ethReply.setSourceMACAddress(ethRequest.getDestinationMAC());
                ethReply.setDestinationMACAddress(ethRequest.getSourceMAC());
                ethReply.setEtherType(ethRequest.getEtherType());

                IPv4 ipv4Reply = new IPv4();
                ipv4Reply.setSourceAddress(ipv4Request.getDestinationAddress());
                ipv4Reply.setDestinationAddress(ipv4Request.getSourceAddress());
                ipv4Reply.setTtl(PACKET_TTL);

                ipv4Reply.setProtocol(IPv4.PROTOCOL_TCP);
                ipv4Reply.setPayload(tcpReply);

                ethReply.setPayload(ipv4Reply);

                ethReplies.add(ethReply);
            }

            return ethReplies;
        }

        /**
         * Obtains the metadata path.
         *
         * @param uri metadata request URI
         * @return full metadata path
         */
        private String getMetadataPath(String uri) {
            OpenstackNode controller = osNodeService.completeNodes(CONTROLLER).
                    stream().findFirst().orElse(null);
            if (controller == null) {
                return null;
            }

            String novaMetadataIpTmp = controller.neutronConfig().novaMetadataIp();
            String novaMetadataIp = novaMetadataIpTmp != null
                    ? novaMetadataIpTmp : controller.managementIp().toString();
            Integer novaMetadataPortTmp = controller.neutronConfig().novaMetadataPort();
            int novaMetadataPort = novaMetadataPortTmp != null
                    ? novaMetadataPortTmp : METADATA_SERVER_PORT;

            return HTTP_PREFIX + novaMetadataIp + COLON + novaMetadataPort + uri;
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
            String url = getMetadataPath(oldRequest.getRequestLine().getUri());

            if (StringUtils.isEmpty(url)) {
                log.warn("The metadata endpoint is not configured!");
                return null;
            }

            HttpRequestBase request;

            String method = oldRequest.getRequestLine().getMethod().toUpperCase();

            log.debug("Sending HTTP {} request to metadata endpoint {}...", method, url);

            switch (method) {
                case HTTP_POST_METHOD:
                    request = new HttpPost(url);
                    HttpEntityEnclosingRequest postRequest =
                            (HttpEntityEnclosingRequest) oldRequest;
                    ((HttpPost) request).setEntity(postRequest.getEntity());
                    break;
                case HTTP_PUT_METHOD:
                    request = new HttpPut(url);
                    HttpEntityEnclosingRequest putRequest =
                            (HttpEntityEnclosingRequest) oldRequest;
                    ((HttpPut) request).setEntity(putRequest.getEntity());
                    break;
                case HTTP_DELETE_METHOD:
                    request = new HttpDelete(url);
                    break;
                case HTTP_GET_METHOD:
                default:
                    request = new HttpGet(url);
                    break;
            }

            // configure headers from original HTTP request
            for (Header header : oldRequest.getAllHeaders()) {
                if (method.equals(HTTP_POST_METHOD) ||
                        method.equals(HTTP_PUT_METHOD)) {
                    // we DO NOT add duplicated HTTP headers for POST and PUT methods
                    if (excludedHeaders.contains(header.getName().toLowerCase())) {
                        continue;
                    }
                }
                request.addHeader(header);
            }

            request.setProtocolVersion(oldRequest.getProtocolVersion());

            Port port = osNetworkService.port(instPort.portId());

            request.addHeader(new BasicHeader(INSTANCE_ID_HEADER, port.getDeviceId()));
            request.addHeader(new BasicHeader(TENANT_ID_HEADER, port.getTenantId()));
            request.addHeader(new BasicHeader(FORWARDED_FOR_HEADER,
                                                instPort.ipAddress().toString()));
            if (metadataSecret() != null) {
                request.addHeader(new BasicHeader(INSTANCE_ID_SIGNATURE_HEADER,
                        hmacEncrypt(metadataSecret(), port.getDeviceId())));
            }

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

        private String metadataSecret() {
            OpenstackNode controller = osNodeService.completeNodes(CONTROLLER)
                    .stream().findFirst().orElse(null);

            if (controller != null && controller.neutronConfig() != null) {
                return controller.neutronConfig().metadataProxySecret();
            }

            return null;
        }
    }

    private class InternalNodeEventListener implements OpenstackNodeListener {
        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            return event.subject().type() == COMPUTE;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()))
                    && useMetadataProxy();
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();
            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(osNode));
                    break;
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_REMOVED:
                case OPENSTACK_NODE_INCOMPLETE:
                default:
                    break;
            }
        }

        private void processNodeCompletion(OpenstackNode osNode) {
            if (!isRelevantHelper()) {
                return;
            }

            setMetadataRule(osNode, true);
        }

        private void processNodeIncompletion(OpenstackNode osNode) {
            if (!isRelevantHelper()) {
                return;
            }

            setMetadataRule(osNode, false);
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
                    DHCP_TABLE,
                    install);
        }
    }

    private boolean useMetadataProxy() {
        OpenstackNode gw = osNodeService.completeNodes(CONTROLLER)
                .stream().findFirst().orElse(null);

        if (gw != null && gw.neutronConfig() != null) {
            return gw.neutronConfig().useMetadataProxy();
        }

        return false;
    }

    /**
     * Implements Http packet format.
     */
    protected static class Http extends BasePacket {

        /**
         * HTTP packet type.
         */
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

        Http() {
        }

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