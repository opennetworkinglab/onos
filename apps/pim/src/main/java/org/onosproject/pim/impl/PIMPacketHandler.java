/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.pim.impl;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.PIM;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handing Incoming and outgoing PIM packets.
 */
public final class PIMPacketHandler {
    private final Logger log = getLogger(getClass());

    private static PIMPacketHandler instance = null;

    private PacketService packetService;
    private PIMPacketProcessor processor = new PIMPacketProcessor();
    private MacAddress pimDestinationMac = MacAddress.valueOf("01:00:5E:00:00:0d");

    // Utility class
    private PIMPacketHandler() {}

    public static PIMPacketHandler getInstance() {
        if (null == instance) {
            instance = new PIMPacketHandler();
        }
        return instance;
    }

    /**
     * Initialize the packet handling service.
     *
     * @param ps the packetService
     * @param appId our application ID
     */
    public void initialize(PacketService ps, ApplicationId appId) {
        packetService = ps;

        // Build a traffic selector for all multicast traffic
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPProtocol(IPv4.PROTOCOL_PIM);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);

        packetService.addProcessor(processor, PacketProcessor.director(1));
    }

    /**
     * Shutdown the packet handling service.
     */
    public void stop() {
        packetService.removeProcessor(processor);
        processor = null;
    }

    /**
     * Packet processor responsible for handling IGMP packets.
     */
    public class PIMPacketProcessor implements PacketProcessor {
        private final Logger log = getLogger(getClass());

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            if (pkt == null) {
                return;
            }

            Ethernet ethPkt = pkt.parsed();
            if (ethPkt == null) {
                return;
            }

            /*
             * IPv6 MLD packets are handled by ICMP6. We'll only deal
             * with IPv4.
             */
            if (ethPkt.getEtherType() != Ethernet.TYPE_IPV4) {
                return;
            }

            IPv4 ip = (IPv4) ethPkt.getPayload();
            IpAddress gaddr = IpAddress.valueOf(ip.getDestinationAddress());
            IpAddress saddr = Ip4Address.valueOf(ip.getSourceAddress());
            log.debug("Packet (" + saddr.toString() + ", " + gaddr.toString() +
                    "\tingress port: " + context.inPacket().receivedFrom().toString());

            if (ip.getProtocol() != IPv4.PROTOCOL_PIM) {
                log.debug("PIM Picked up a non PIM packet: IP protocol: " + ip.getProtocol());
                return;
            }

            // TODO: check incoming to be PIM.PIM_ADDRESS or "Our" address.
            IpPrefix spfx = IpPrefix.valueOf(saddr, 32);
            IpPrefix gpfx = IpPrefix.valueOf(gaddr, 32);

            PIM pim = (PIM) ip.getPayload();
            switch (pim.getPimMsgType()) {

                case PIM.TYPE_HELLO:
                    processHello(ethPkt, context.inPacket().receivedFrom());
                    break;

                case PIM.TYPE_JOIN_PRUNE_REQUEST:
                    // Create the function
                    break;

                case PIM.TYPE_ASSERT:
                case PIM.TYPE_BOOTSTRAP:
                case PIM.TYPE_CANDIDATE_RP_ADV:
                case PIM.TYPE_GRAFT:
                case PIM.TYPE_GRAFT_ACK:
                case PIM.TYPE_REGISTER:
                case PIM.TYPE_REGISTER_STOP:
                    log.debug("Unsupported PIM message type: " + pim.getPimMsgType());
                    break;

                default:
                    log.debug("Unkown PIM message type: " + pim.getPimMsgType());
                    break;
            }
        }

        /**
         * Process incoming hello message, we will need the Macaddress and IP address of the sender.
         *
         * @param ethPkt the ethernet header
         * @param receivedFrom the connect point we recieved this message from
         */
        private void processHello(Ethernet ethPkt, ConnectPoint receivedFrom) {
            checkNotNull(ethPkt);
            checkNotNull(receivedFrom);

            // It is a problem if we don't have the
            PIMInterfaces pintfs = PIMInterfaces.getInstance();
            PIMInterface intf = pintfs.getInterface(receivedFrom);
            if (intf == null) {
                log.error("We received a PIM message on an interface we were not supposed to");
                return;
            }
            intf.processHello(ethPkt, receivedFrom);
        }
    }

    // Create an ethernet header and serialize then send
    public void sendPacket(PIM pim, PIMInterface pimIntf) {

        Interface theInterface = pimIntf.getInterface();

        // Create the ethernet packet
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(pimDestinationMac);
        eth.setSourceMACAddress(theInterface.mac());
        eth.setEtherType(Ethernet.TYPE_IPV4);
        if (theInterface.vlan() != VlanId.NONE) {
            eth.setVlanID(theInterface.vlan().toShort());
        }

        // Create the IP Packet
        IPv4 ip = new IPv4();
        ip.setVersion((byte) 4);
        ip.setTtl((byte) 20);
        ip.setProtocol(IPv4.PROTOCOL_PIM);
        ip.setChecksum((short) 0);
        ip.setSourceAddress(checkNotNull(pimIntf.getIpAddress()).getIp4Address().toInt());
        ip.setDestinationAddress(PIM.PIM_ADDRESS.getIp4Address().toInt());
        eth.setPayload(ip);
        ip.setParent(eth);

        // Now set pim
        ip.setPayload(pim);
        pim.setParent(ip);

        ConnectPoint cp = theInterface.connectPoint();
        checkNotNull(cp);

        TrafficTreatment treat = DefaultTrafficTreatment.builder().setOutput(cp.port()).build();
        ByteBuffer bb = ByteBuffer.wrap(eth.serialize());
        OutboundPacket packet = new DefaultOutboundPacket(cp.deviceId(), treat, bb);
        checkNotNull(packet);

        packetService.emit(packet);
    }
}
