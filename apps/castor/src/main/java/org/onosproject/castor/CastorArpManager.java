/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.castor;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;

import static org.onlab.packet.Ethernet.TYPE_ARP;
import static org.onosproject.net.packet.PacketPriority.CONTROL;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component for managing the ARPs.
 */

@Component(immediate = true, enabled = true)
@Service
public class CastorArpManager implements ArpService  {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ConnectivityManagerService connectivityManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CastorStore castorStore;

    private ProxyArpProcessor processor = new ProxyArpProcessor();

    private final Logger log = getLogger(getClass());
    private static final int FLOW_PRIORITY = 500;
    private static final MacAddress ARP_SOURCEMAC = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress ARP_DEST = MacAddress.valueOf("00:00:00:00:00:00");
    private static final byte[] ZERO_MAC_ADDRESS = MacAddress.ZERO.toBytes();
    private static final IpAddress ARP_SRC = Ip4Address.valueOf("0.0.0.0");

    private ApplicationId appId;
    Optional<DeviceId> deviceID = null;

    private enum Protocol {
        ARP
    }

    private enum MessageType {
        REQUEST, REPLY
    }

    @Activate
    public void activate() {
        appId = coreService.getAppId(Castor.CASTOR_APP);
        packetService.addProcessor(processor, PacketProcessor.director(1));
        requestPackets();
    }

    @Deactivate
    public void deactivate() {
        withdrawIntercepts();
        packetService.removeProcessor(processor);
        processor = null;
    }

    /**
     * Used to request the ARP packets.
     */
    private void requestPackets() {
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_ARP);
        packetService.requestPackets(selectorBuilder.build(), CONTROL, appId);
    }

    /**
     * Withdraws the requested ARP packets.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selectorBuilder =
                DefaultTrafficSelector.builder();
        selectorBuilder.matchEthType(TYPE_ARP);
        packetService.cancelPackets(selectorBuilder.build(), CONTROL, appId, deviceID);
    }

    /**
     * Forwards the ARP packet to the specified connect point via packet out.
     *
     * @param context The packet context
     */
    private void forward(MessageContext context) {

        TrafficTreatment.Builder builder = null;
        Ethernet eth = context.packet();
        ByteBuffer buf = ByteBuffer.wrap(eth.serialize());

        IpAddress target = context.target();
        String value = getMatchingConnectPoint(target);
        if (value != null) {
            ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(value);
            builder = DefaultTrafficTreatment.builder();
            builder.setOutput(connectPoint.port());
            packetService.emit(new DefaultOutboundPacket(connectPoint.deviceId(), builder.build(), buf));
        }
    }

    @Override
    public void createArp(Peer peer) {

        Ethernet packet = null;
        packet = buildArpRequest(peer);
        ByteBuffer buf = ByteBuffer.wrap(packet.serialize());
        ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(peer.getPort());

        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        builder.setOutput(connectPoint.port());
        packetService.emit(new DefaultOutboundPacket(connectPoint.deviceId(), builder.build(), buf));

    }

    /**
     * Builds the ARP request when MAC is not known.
     *
     * @param peer The Peer whose MAC is not known.
     * @return Ethernet
     */
    private Ethernet buildArpRequest(Peer peer) {
        ARP arp = new ARP();
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET)
                .setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH)
                .setProtocolType(ARP.PROTO_TYPE_IP)
                .setProtocolAddressLength((byte) IpAddress.INET_BYTE_LENGTH)
                .setOpCode(ARP.OP_REQUEST);

        arp.setSenderHardwareAddress(ARP_SOURCEMAC.toBytes())
                .setSenderProtocolAddress(ARP_SRC.toOctets())
                .setTargetHardwareAddress(ZERO_MAC_ADDRESS)
                .setTargetProtocolAddress(IpAddress.valueOf(peer.getIpAddress()).toOctets());

        Ethernet ethernet = new Ethernet();
        ethernet.setEtherType(Ethernet.TYPE_ARP)
                .setDestinationMACAddress(MacAddress.BROADCAST)
                .setSourceMACAddress(ARP_SOURCEMAC)
                .setPayload(arp);
        ethernet.setPad(true);

        return ethernet;
    }

    /**
     * Gets the matching connect point corresponding to the peering IP address.
     *
     * @param target Target IP address
     * @return Connect point as a String
     */
    private String getMatchingConnectPoint(IpAddress target) {
        Set<Peer> peers = castorStore.getAllPeers();
        for (Peer peer : peers) {
            IpAddress match = IpAddress.valueOf(peer.getIpAddress());
            if (match.equals(target)) {
                return peer.getPort();
            }
        }
        return null;
    }

    /**
     * Returns the matching Peer or route server on a Connect Point.
     *
     * @param connectPoint The peering connect point.
     * @return Peer or Route Server
     */
    private Peer getMatchingPeer(ConnectPoint connectPoint) {

        for (Peer peer : castorStore.getAllPeers()) {
            if (connectPoint.equals(ConnectPoint.deviceConnectPoint(peer.getPort()))) {
                return peer;
            }
        }
        return null;
    }

    /**
     * Returns matching BGP Peer on a connect point.
     *
     * @param connectPoint The peering connect point.
     * @return The Peer
     */
    private Peer getMatchingCustomer(ConnectPoint connectPoint) {

        for (Peer peer : castorStore.getCustomers()) {
            if (connectPoint.equals(ConnectPoint.deviceConnectPoint(peer.getPort()))) {
                return peer;
            }
        }
        return null;
    }

    /**
     * Updates the IP address to mac address map.
     *
     * @param context The message context.
     */
    private void updateMac(MessageContext context) {

        if ((castorStore.getAddressMap()).containsKey(context.sender())) {
            return;
        }
        Ethernet eth = context.packet();
        MacAddress macAddress = eth.getSourceMAC();
        IpAddress ipAddress = context.sender();
        castorStore.setAddressMap(ipAddress, macAddress);
    }

    /**
     * Setup the layer two flows if not already installed after an ARP packet is received.
     * If the layer 2 status is true, means layer two flows are already provisioned.
     * If the status was false, layer 2 flows will be installed at this point. This
     * happens when the mac address of a peer was not known at the time of its addition.
     *
     * @param msgContext The message context.
     */
    private void handleArpForL2(MessageContext msgContext) {

        ConnectPoint cp = msgContext.inPort();
        Peer peer = getMatchingCustomer(cp);

        if (peer != null && !peer.getl2Status()) {
            connectivityManager.setUpL2(peer);
        }
    }

    @Override
    public boolean handlePacket(PacketContext context) {

        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();

        if (ethPkt == null) {
            return false;
        }

        MessageContext msgContext = createContext(ethPkt, pkt.receivedFrom());

        if (msgContext == null) {
            return false;
        }
        switch (msgContext.type()) {
            case REPLY:
                forward(msgContext);
                updateMac(msgContext);
                handleArpForL2(msgContext);
                break;
            case REQUEST:
                forward(msgContext);
                updateMac(msgContext);
                handleArpForL2(msgContext);
                break;
            default:
                return false;
        }
        context.block();
        return true;
    }

    private MessageContext createContext(Ethernet eth, ConnectPoint inPort) {
        if (eth.getEtherType() == Ethernet.TYPE_ARP) {
            return createArpContext(eth, inPort);
        }
        return null;
    }

    /**
     * Extracts context information from ARP packets.
     *
     * @param eth input Ethernet frame that is thought to be ARP
     * @param inPort in port
     * @return MessageContext object if the packet was a valid ARP packet,
     * otherwise null
     */
    private MessageContext createArpContext(Ethernet eth, ConnectPoint inPort) {
        if (eth.getEtherType() != Ethernet.TYPE_ARP) {
            return null;
        }

        ARP arp = (ARP) eth.getPayload();

        IpAddress target = Ip4Address.valueOf(arp.getTargetProtocolAddress());
        IpAddress sender = Ip4Address.valueOf(arp.getSenderProtocolAddress());

        MessageType type;
        if (arp.getOpCode() == ARP.OP_REQUEST) {
            type = MessageType.REQUEST;
        } else if (arp.getOpCode() == ARP.OP_REPLY) {
            type = MessageType.REPLY;
        } else {
            return null;
        }
        return new MessageContext(eth, inPort, Protocol.ARP, type, target, sender);
    }

    private class MessageContext {
        private Protocol protocol;
        private MessageType type;

        private IpAddress target;
        private IpAddress sender;

        private Ethernet eth;
        private ConnectPoint inPort;

        public MessageContext(Ethernet eth, ConnectPoint inPort,
                              Protocol protocol, MessageType type,
                              IpAddress target, IpAddress sender) {
            this.eth = eth;
            this.inPort = inPort;
            this.protocol = protocol;
            this.type = type;
            this.target = target;
            this.sender = sender;
        }

        public ConnectPoint inPort() {
            return inPort;
        }

        public Ethernet packet() {
            return eth;
        }

        public Protocol protocol() {
            return protocol;
        }

        public MessageType type() {
            return type;
        }

        public VlanId vlan() {
            return VlanId.vlanId(eth.getVlanID());
        }

        public MacAddress srcMac() {
            return MacAddress.valueOf(eth.getSourceMACAddress());
        }

        public IpAddress target() {
            return target;
        }

        public IpAddress sender() {
            return sender;
        }
    }
    private class ProxyArpProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            if (context.isHandled()) {
                return;
            }
            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            if (ethPkt == null) {
                return;
            }
            if (ethPkt.getEtherType() == TYPE_ARP) {
                //handle the arp packet.
                handlePacket(context);
            } else {
                return;
            }
        }
    }
}
