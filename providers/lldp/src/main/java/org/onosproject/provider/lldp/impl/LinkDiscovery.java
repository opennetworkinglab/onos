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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.flow.DefaultTrafficTreatment.builder;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ONOSLLDP;
import org.onlab.util.Timer;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link.Type;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

// TODO: add 'fast discovery' mode: drop LLDPs in destination switch but listen for flow_removed messages

/**
 * Run discovery process from a physical switch. Ports are initially labeled as
 * slow ports. When an LLDP is successfully received, label the remote port as
 * fast. Every probeRate milliseconds, loop over all fast ports and send an
 * LLDP, send an LLDP for a single slow port. Based on FlowVisor topology
 * discovery implementation.
 */
public class LinkDiscovery implements TimerTask {

    private final Device device;
    // send 1 probe every probeRate milliseconds
    private final long probeRate;
    private final Set<Long> slowPorts;
    // ports, known to have incoming links
    private final Set<Long> fastPorts;
    // number of unacknowledged probes per port
    private final Map<Long, AtomicInteger> portProbeCount;
    // number of probes to send before link is removed
    private static final short MAX_PROBE_COUNT = 3;
    private final Logger log = getLogger(getClass());
    private final ONOSLLDP lldpPacket;
    private final Ethernet ethPacket;
    private Ethernet bddpEth;
    private final boolean useBDDP;
    private final LinkProviderService linkProvider;
    private final PacketService pktService;
    private final MastershipService mastershipService;
    private Timeout timeout;
    private volatile boolean isStopped;

    /**
     * Instantiates discovery manager for the given physical switch. Creates a
     * generic LLDP packet that will be customized for the port it is sent out on.
     * Starts the the timer for the discovery process.
     *
     * @param device        the physical switch
     * @param pktService    packet service
     * @param masterService mastership service
     * @param providerService link provider service
     * @param useBDDP       flag to also use BDDP for discovery
     */
    public LinkDiscovery(Device device, PacketService pktService,
                         MastershipService masterService,
                         LinkProviderService providerService, Boolean... useBDDP) {
        this.device = device;
        this.probeRate = 3000;
        this.linkProvider = providerService;
        this.pktService = pktService;

        this.mastershipService = checkNotNull(masterService, "WTF!");
        this.slowPorts = Collections.synchronizedSet(new HashSet<>());
        this.fastPorts = Collections.synchronizedSet(new HashSet<>());
        this.portProbeCount = new HashMap<>();
        this.lldpPacket = new ONOSLLDP();
        this.lldpPacket.setChassisId(device.chassisId());
        this.lldpPacket.setDevice(device.id().toString());


        this.ethPacket = new Ethernet();
        this.ethPacket.setEtherType(Ethernet.TYPE_LLDP);
        this.ethPacket.setDestinationMACAddress(ONOSLLDP.LLDP_NICIRA);
        this.ethPacket.setPayload(this.lldpPacket);
        this.ethPacket.setPad(true);
        this.useBDDP = useBDDP.length > 0 ? useBDDP[0] : false;
        if (this.useBDDP) {
            this.bddpEth = new Ethernet();
            this.bddpEth.setPayload(this.lldpPacket);
            this.bddpEth.setEtherType(Ethernet.TYPE_BSN);
            this.bddpEth.setDestinationMACAddress(ONOSLLDP.BDDP_MULTICAST);
            this.bddpEth.setPad(true);
            log.info("Using BDDP to discover network");
        }

        this.isStopped = true;
        start();
        this.log.debug("Started discovery manager for switch {}",
                       device.id());

    }

    /**
     * Add physical port port to discovery process.
     * Send out initial LLDP and label it as slow port.
     *
     * @param port the port
     */
    public void addPort(final Port port) {
        this.log.debug("Sending init probe to port {}@{}",
                       port.number().toLong(), device.id());
        boolean isMaster = mastershipService.getLocalRole(device.id()) == MASTER;
        if (isMaster) {
            sendProbes(port.number().toLong());
        }
        synchronized (this) {
            this.slowPorts.add(port.number().toLong());
        }
    }

    /**
     * Removes physical port from discovery process.
     *
     * @param port the port
     */
    public void removePort(final Port port) {
        // Ignore ports that are not on this switch

        long portnum = port.number().toLong();
        synchronized (this) {
            if (this.slowPorts.contains(portnum)) {
                this.slowPorts.remove(portnum);

            } else if (this.fastPorts.contains(portnum)) {
                this.fastPorts.remove(portnum);
                this.portProbeCount.remove(portnum);
                // no iterator to update
            } else {
                this.log.warn("Tried to dynamically remove non-existing port {}",
                              portnum);
            }
        }
    }

    /**
     * Method called by remote port to acknowledge receipt of LLDP sent by
     * this port. If slow port, updates label to fast. If fast port, decrements
     * number of unacknowledged probes.
     *
     * @param portNumber the port
     */
    public void ackProbe(final Long portNumber) {
        synchronized (this) {
            if (this.slowPorts.contains(portNumber)) {
                this.log.debug("Setting slow port to fast: {}:{}",
                               this.device.id(), portNumber);
                this.slowPorts.remove(portNumber);
                this.fastPorts.add(portNumber);
                this.portProbeCount.put(portNumber, new AtomicInteger(0));
            } else if (this.fastPorts.contains(portNumber)) {
                this.portProbeCount.get(portNumber).set(0);
            } else {
                this.log.debug("Got ackProbe for non-existing port: {}", portNumber);
            }
        }
    }


    /**
     * Handles an incoming LLDP packet. Creates link in topology and sends ACK
     * to port where LLDP originated.
     * @param context packet context
     * @return true if handled
     */
    public boolean handleLLDP(PacketContext context) {
        Ethernet eth = context.inPacket().parsed();
        if (eth == null) {
            return false;
        }

        ONOSLLDP onoslldp = ONOSLLDP.parseONOSLLDP(eth);
        if (onoslldp != null) {
            final PortNumber dstPort =
                    context.inPacket().receivedFrom().port();
            final PortNumber srcPort = portNumber(onoslldp.getPort());
            final DeviceId srcDeviceId = DeviceId.deviceId(onoslldp.getDeviceString());
            final DeviceId dstDeviceId = context.inPacket().receivedFrom().deviceId();
            this.ackProbe(dstPort.toLong());
            ConnectPoint src = new ConnectPoint(srcDeviceId, srcPort);
            ConnectPoint dst = new ConnectPoint(dstDeviceId, dstPort);

            LinkDescription ld;
            if (eth.getEtherType() == Ethernet.TYPE_BSN) {
                ld = new DefaultLinkDescription(src, dst, Type.INDIRECT);
            } else {
                ld = new DefaultLinkDescription(src, dst, Type.DIRECT);
            }
            linkProvider.linkDetected(ld);
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
    public void run(final Timeout t) {
        if (isStopped()) {
            return;
        }
        boolean isMaster = mastershipService.getLocalRole(device.id()) == MASTER;
        if (!isMaster) {
            if (!isStopped()) {
                // reschedule timer
                timeout = Timer.getTimer().newTimeout(this, this.probeRate, MILLISECONDS);
            }
            return;
        }

        this.log.trace("Sending probes from {}", device.id());
        synchronized (this) {
            final Iterator<Long> fastIterator = this.fastPorts.iterator();
            while (fastIterator.hasNext()) {
                long portNumber = fastIterator.next();
                int probeCount = portProbeCount.get(portNumber).getAndIncrement();

                if (probeCount < LinkDiscovery.MAX_PROBE_COUNT) {
                    this.log.trace("Sending fast probe to port {}", portNumber);
                    sendProbes(portNumber);

                } else {
                    // Link down, demote to slowPorts
                    // Update fast and slow ports
                    fastIterator.remove();
                    this.slowPorts.add(portNumber);
                    this.portProbeCount.remove(portNumber);

                    ConnectPoint cp = new ConnectPoint(device.id(),
                                                       portNumber(portNumber));
                    log.debug("Link down -> {}", cp);
                    linkProvider.linksVanished(cp);
                }
            }

            // send a probe for the next slow port
            for (long portNumber : slowPorts) {
                this.log.trace("Sending slow probe to port {}", portNumber);
                sendProbes(portNumber);
            }
        }

        if (!isStopped()) {
            // reschedule timer
            timeout = Timer.getTimer().newTimeout(this, this.probeRate, MILLISECONDS);
        }
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
    private OutboundPacket createOutBoundLLDP(final Long port) {
        if (port == null) {
            return null;
        }
        this.lldpPacket.setPortId(port.intValue());
        this.ethPacket.setSourceMACAddress("DE:AD:BE:EF:BA:11");

        final byte[] lldp = this.ethPacket.serialize();
        return new DefaultOutboundPacket(this.device.id(),
                                         builder().setOutput(portNumber(port)).build(),
                                         ByteBuffer.wrap(lldp));
    }

    /**
     * Creates packet_out BDDP for specified output port.
     *
     * @param port the port
     * @return Packet_out message with LLDP data
     */
    private OutboundPacket createOutBoundBDDP(final Long port) {
        if (port == null) {
            return null;
        }
        this.lldpPacket.setPortId(port.intValue());
        this.bddpEth.setSourceMACAddress("DE:AD:BE:EF:BA:11");

        final byte[] bddp = this.bddpEth.serialize();
        return new DefaultOutboundPacket(this.device.id(),
                                         builder().setOutput(portNumber(port)).build(),
                                         ByteBuffer.wrap(bddp));
    }

    private void sendProbes(Long portNumber) {
        log.trace("Sending probes out to {}@{}", portNumber, device.id());
        OutboundPacket pkt = this.createOutBoundLLDP(portNumber);
        pktService.emit(pkt);
        if (useBDDP) {
            OutboundPacket bpkt = this.createOutBoundBDDP(portNumber);
            pktService.emit(bpkt);
        }
    }

    public boolean containsPort(Long portNumber) {
        return slowPorts.contains(portNumber) || fastPorts.contains(portNumber);
    }

    public synchronized boolean isStopped() {
        return isStopped || timeout.isCancelled();
    }

}
