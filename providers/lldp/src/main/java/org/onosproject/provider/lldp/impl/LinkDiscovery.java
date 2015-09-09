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

import com.google.common.collect.Maps;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
public class LinkDiscovery implements TimerTask {

    private final Logger log = getLogger(getClass());

    private static final String SRC_MAC = "DE:AD:BE:EF:BA:11";

    private final Device device;
    private final DiscoveryContext context;

    private final ONOSLLDP lldpPacket;
    private final Ethernet ethPacket;
    private Ethernet bddpEth;

    private Timeout timeout;
    private volatile boolean isStopped;

    // Set of ports to be probed
    private final Set<Long> ports = Sets.newConcurrentHashSet();

    // Most recent time a link was seen
    private final Map<LinkKey, Long> linkTimes = Maps.newConcurrentMap();

    /**
     * Instantiates discovery manager for the given physical switch. Creates a
     * generic LLDP packet that will be customized for the port it is sent out on.
     * Starts the the timer for the discovery process.
     *
     * @param device  the physical switch
     * @param context discovery context
     */
    public LinkDiscovery(Device device, DiscoveryContext context) {
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
        log.info("Using BDDP to discover network");

        isStopped = true;
        start();
        log.debug("Started discovery manager for switch {}", device.id());

    }

    /**
     * Add physical port port to discovery process.
     * Send out initial LLDP and label it as slow port.
     *
     * @param port the port
     */
    public void addPort(Port port) {
        boolean newPort = ports.add(port.number().toLong());
        boolean isMaster = context.mastershipService().isLocalMaster(device.id());
        if (newPort && isMaster) {
            log.debug("Sending init probe to port {}@{}", port.number().toLong(), device.id());
            sendProbes(port.number().toLong());
        }
    }

    /**
     * Method called by remote port to acknowledge receipt of LLDP sent by
     * this port. If slow port, updates label to fast. If fast port, decrements
     * number of unacknowledged probes.
     *
     * @param key link key
     */
    private void ackProbe(LinkKey key) {
        long portNumber = key.src().port().toLong();
        if (ports.contains(portNumber)) {
            linkTimes.put(key, System.currentTimeMillis());
        } else {
            log.debug("Got ackProbe for non-existing port: {}", portNumber);
        }
    }


    /**
     * Handles an incoming LLDP packet. Creates link in topology and sends ACK
     * to port where LLDP originated.
     *
     * @param packetContext packet context
     * @return true if handled
     */
    public boolean handleLLDP(PacketContext packetContext) {
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

            ackProbe(LinkKey.linkKey(src, dst));

            LinkDescription ld = eth.getEtherType() == Ethernet.TYPE_LLDP ?
                    new DefaultLinkDescription(src, dst, Type.DIRECT) :
                    new DefaultLinkDescription(src, dst, Type.INDIRECT);

            try {
                context.providerService().linkDetected(ld);
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

        if (!context.mastershipService().isLocalMaster(device.id())) {
            if (!isStopped()) {
                timeout = Timer.getTimer().newTimeout(this, context.probeRate(), MILLISECONDS);
            }
            return;
        }

        // Prune stale links
        linkTimes.entrySet().stream()
                .filter(e -> isStale(e.getKey(), e.getValue()))
                .map(Map.Entry::getKey).collect(Collectors.toSet())
                .forEach(this::pruneLink);

        // Probe ports
        log.trace("Sending probes from {}", device.id());
        ports.forEach(this::sendProbes);

        if (!isStopped()) {
            timeout = Timer.getTimer().newTimeout(this, context.probeRate(), MILLISECONDS);
        }
    }

    private void pruneLink(LinkKey key) {
        linkTimes.remove(key);
        LinkDescription desc = new DefaultLinkDescription(key.src(), key.dst(), Type.DIRECT);
        context.providerService().linkVanished(desc);
    }

    private boolean isStale(LinkKey key, long lastSeen) {
        return lastSeen < (System.currentTimeMillis() - context.staleLinkAge());
    }

    public synchronized void stop() {
        isStopped = true;
        timeout.cancel();
    }

    public synchronized void start() {
        if (isStopped) {
            isStopped = false;
            timeout = Timer.getTimer().newTimeout(this, 0, MILLISECONDS);
        } else {
            log.warn("LinkDiscovery started multiple times?");
        }
    }

    /**
     * Creates packet_out LLDP for specified output port.
     *
     * @param port the port
     * @return Packet_out message with LLDP data
     */
    private OutboundPacket createOutBoundLLDP(Long port) {
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
    private OutboundPacket createOutBoundBDDP(Long port) {
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
        OutboundPacket pkt = createOutBoundLLDP(portNumber);
        context.packetService().emit(pkt);
        if (context.useBDDP()) {
            OutboundPacket bpkt = createOutBoundBDDP(portNumber);
            context.packetService().emit(bpkt);
        }
    }

    public synchronized boolean isStopped() {
        return isStopped || timeout.isCancelled();
    }

    boolean containsPort(long portNumber) {
        return ports.contains(portNumber);
    }
}
