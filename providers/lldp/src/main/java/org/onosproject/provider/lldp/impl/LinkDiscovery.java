/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.provider.lldp.impl;

import com.google.common.collect.Sets;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ONOSLLDP;
import org.onlab.util.Timer;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link.Type;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Set;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.flow.DefaultTrafficTreatment.builder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Run discovery process from a physical switch. Ports are initially labeled as
 * slow ports. When an LLDP is successfully received, label the remote port as
 * fast. Every probeRate milliseconds, loop over all fast ports and send an
 * LLDP, send an LLDP for a single slow port. Based on FlowVisor topology
 * discovery implementation.
 */
class LinkDiscovery implements TimerTask {

    private final Logger log = getLogger(getClass());

    private static final String SRC_MAC = "DE:AD:BE:EF:BA:11";

    private final Device device;
    private final DiscoveryContext context;

    private final ONOSLLDP lldpPacket;
    private final Ethernet ethPacket;
    private final Ethernet bddpEth;

    private Timeout timeout;
    private volatile boolean isStopped;

    // Set of ports to be probed
    private final Set<Long> ports = Sets.newConcurrentHashSet();

    /**
     * Instantiates discovery manager for the given physical switch. Creates a
     * generic LLDP packet that will be customized for the port it is sent out on.
     * Starts the the timer for the discovery process.
     *
     * @param device  the physical switch
     * @param context discovery context
     */
    LinkDiscovery(Device device, DiscoveryContext context) {
        this.device = device;
        this.context = context;

        lldpPacket = new ONOSLLDP();
        lldpPacket.setChassisId(device.chassisId());
        lldpPacket.setDevice(device.id().toString());

        ethPacket = new Ethernet();
        ethPacket.setEtherType(Ethernet.TYPE_LLDP);
        ethPacket.setDestinationMACAddress(ONOSLLDP.LLDP_NICIRA);
        ethPacket.setPayload(this.lldpPacket);
        ethPacket.setPad(true);

        bddpEth = new Ethernet();
        bddpEth.setPayload(lldpPacket);
        bddpEth.setEtherType(Ethernet.TYPE_BSN);
        bddpEth.setDestinationMACAddress(ONOSLLDP.BDDP_MULTICAST);
        bddpEth.setPad(true);

        isStopped = true;
        start();
        log.debug("Started discovery manager for switch {}", device.id());

    }

    synchronized void stop() {
        if (!isStopped) {
            isStopped = true;
            timeout.cancel();
        } else {
            log.warn("LinkDiscovery stopped multiple times?");
        }
    }

    synchronized void start() {
        if (isStopped) {
            isStopped = false;
            timeout = Timer.getTimer().newTimeout(this, 0, MILLISECONDS);
        } else {
            log.warn("LinkDiscovery started multiple times?");
        }
    }

    synchronized boolean isStopped() {
        return isStopped || timeout.isCancelled();
    }

    /**
     * Add physical port to discovery process.
     * Send out initial LLDP and label it as slow port.
     *
     * @param port the port
     */
    void addPort(Port port) {
        boolean newPort = ports.add(port.number().toLong());
        boolean isMaster = context.mastershipService().isLocalMaster(device.id());
        if (newPort && isMaster) {
            log.debug("Sending initial probe to port {}@{}", port.number().toLong(), device.id());
            sendProbes(port.number().toLong());
        }
    }

    /**
     * removed physical port from discovery process.
     * @param port the port number
     */
    void removePort(PortNumber port) {
        ports.remove(port.toLong());
    }

    /**
     * Handles an incoming LLDP packet. Creates link in topology and adds the
     * link for staleness tracking.
     *
     * @param packetContext packet context
     * @return true if handled
     */
    boolean handleLldp(PacketContext packetContext) {
        Ethernet eth = packetContext.inPacket().parsed();
        if (eth == null) {
            return false;
        }

        ONOSLLDP onoslldp = ONOSLLDP.parseONOSLLDP(eth);
        if (onoslldp != null) {
            PortNumber srcPort = portNumber(onoslldp.getPort());
            PortNumber dstPort = packetContext.inPacket().receivedFrom().port();
            DeviceId srcDeviceId = DeviceId.deviceId(onoslldp.getDeviceString());
            DeviceId dstDeviceId = packetContext.inPacket().receivedFrom().deviceId();

            ConnectPoint src = new ConnectPoint(srcDeviceId, srcPort);
            ConnectPoint dst = new ConnectPoint(dstDeviceId, dstPort);

            LinkDescription ld = eth.getEtherType() == Ethernet.TYPE_LLDP ?
                    new DefaultLinkDescription(src, dst, Type.DIRECT) :
                    new DefaultLinkDescription(src, dst, Type.INDIRECT);

            try {
                context.providerService().linkDetected(ld);
                context.touchLink(LinkKey.linkKey(src, dst));
            } catch (IllegalStateException e) {
                return true;
            }
            return true;
        }
        return false;
    }


    /**
     * Execute this method every t milliseconds. Loops over all ports
     * labeled as fast and sends out an LLDP. Send out an LLDP on a single slow
     * port.
     *
     * @param t timeout
     */
    @Override
    public void run(Timeout t) {
        if (isStopped()) {
            return;
        }

        if (context.mastershipService().isLocalMaster(device.id())) {
            log.trace("Sending probes from {}", device.id());
            ports.forEach(this::sendProbes);
        }

        if (!isStopped()) {
            timeout = Timer.getTimer().newTimeout(this, context.probeRate(), MILLISECONDS);
        }
    }

    /**
     * Creates packet_out LLDP for specified output port.
     *
     * @param port the port
     * @return Packet_out message with LLDP data
     */
    private OutboundPacket createOutBoundLldp(Long port) {
        if (port == null) {
            return null;
        }
        lldpPacket.setPortId(port.intValue());
        ethPacket.setSourceMACAddress(SRC_MAC);
        return new DefaultOutboundPacket(device.id(),
                                         builder().setOutput(portNumber(port)).build(),
                                         ByteBuffer.wrap(ethPacket.serialize()));
    }

    /**
     * Creates packet_out BDDP for specified output port.
     *
     * @param port the port
     * @return Packet_out message with LLDP data
     */
    private OutboundPacket createOutBoundBddp(Long port) {
        if (port == null) {
            return null;
        }
        lldpPacket.setPortId(port.intValue());
        bddpEth.setSourceMACAddress(SRC_MAC);
        return new DefaultOutboundPacket(device.id(),
                                         builder().setOutput(portNumber(port)).build(),
                                         ByteBuffer.wrap(bddpEth.serialize()));
    }

    private void sendProbes(Long portNumber) {
        log.trace("Sending probes out to {}@{}", portNumber, device.id());
        OutboundPacket pkt = createOutBoundLldp(portNumber);
        context.packetService().emit(pkt);
        if (context.useBddp()) {
            OutboundPacket bpkt = createOutBoundBddp(portNumber);
            context.packetService().emit(bpkt);
        }
    }

    boolean containsPort(long portNumber) {
        return ports.contains(portNumber);
    }

}
