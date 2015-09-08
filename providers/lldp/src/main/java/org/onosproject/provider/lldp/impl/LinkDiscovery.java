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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.flow.DefaultTrafficTreatment.builder;
import static org.slf4j.LoggerFactory.getLogger;

// TODO: add 'fast discovery' mode: drop LLDPs in destination switch but listen for flow_removed messages
// FIXME: add ability to track links using port pairs or the link inventory

/**
 * Run discovery process from a physical switch. Ports are initially labeled as
 * slow ports. When an LLDP is successfully received, label the remote port as
 * fast. Every probeRate milliseconds, loop over all fast ports and send an
 * LLDP, send an LLDP for a single slow port. Based on FlowVisor topology
 * discovery implementation.
 */
public class LinkDiscovery implements TimerTask {

    private final Logger log = getLogger(getClass());

    private static final short MAX_PROBE_COUNT = 3; // probes to send before link is removed
    private static final short DEFAULT_PROBE_RATE = 3000; // millis
    private static final String SRC_MAC = "DE:AD:BE:EF:BA:11";
    private static final String SERVICE_NULL = "Service cannot be null";

    private final Device device;

    // send 1 probe every probeRate milliseconds
    private final long probeRate = DEFAULT_PROBE_RATE;

    private final Set<Long> slowPorts = Sets.newConcurrentHashSet();
    // ports, known to have incoming links
    private final Set<Long> fastPorts = Sets.newConcurrentHashSet();

    // number of unacknowledged probes per port
    private final Map<Long, AtomicInteger> portProbeCount = Maps.newHashMap();

    private final ONOSLLDP lldpPacket;
    private final Ethernet ethPacket;
    private Ethernet bddpEth;
    private final boolean useBDDP;

    private Timeout timeout;
    private volatile boolean isStopped;

    private final LinkProviderService linkProvider;
    private final PacketService pktService;
    private final MastershipService mastershipService;

    /**
     * Instantiates discovery manager for the given physical switch. Creates a
     * generic LLDP packet that will be customized for the port it is sent out on.
     * Starts the the timer for the discovery process.
     *
     * @param device          the physical switch
     * @param pktService      packet service
     * @param masterService   mastership service
     * @param providerService link provider service
     * @param useBDDP         flag to also use BDDP for discovery
     */
    public LinkDiscovery(Device device, PacketService pktService,
                         MastershipService masterService,
                         LinkProviderService providerService, Boolean... useBDDP) {
        this.device = device;
        this.linkProvider = checkNotNull(providerService, SERVICE_NULL);
        this.pktService = checkNotNull(pktService, SERVICE_NULL);
        this.mastershipService = checkNotNull(masterService, SERVICE_NULL);

        lldpPacket = new ONOSLLDP();
        lldpPacket.setChassisId(device.chassisId());
        lldpPacket.setDevice(device.id().toString());

        ethPacket = new Ethernet();
        ethPacket.setEtherType(Ethernet.TYPE_LLDP);
        ethPacket.setDestinationMACAddress(ONOSLLDP.LLDP_NICIRA);
        ethPacket.setPayload(this.lldpPacket);
        ethPacket.setPad(true);

        this.useBDDP = useBDDP.length > 0 ? useBDDP[0] : false;
        if (this.useBDDP) {
            bddpEth = new Ethernet();
            bddpEth.setPayload(lldpPacket);
            bddpEth.setEtherType(Ethernet.TYPE_BSN);
            bddpEth.setDestinationMACAddress(ONOSLLDP.BDDP_MULTICAST);
            bddpEth.setPad(true);
            log.info("Using BDDP to discover network");
        }

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
        boolean newPort = false;
        synchronized (this) {
            if (!containsPort(port.number().toLong())) {
                newPort = true;
                slowPorts.add(port.number().toLong());
            }
        }

        boolean isMaster = mastershipService.isLocalMaster(device.id());
        if (newPort && isMaster) {
            log.debug("Sending init probe to port {}@{}", port.number().toLong(), device.id());
            sendProbes(port.number().toLong());
        }
    }

    /**
     * Removes physical port from discovery process.
     *
     * @param port the port
     */
    public void removePort(Port port) {
        // Ignore ports that are not on this switch
        long portnum = port.number().toLong();
        synchronized (this) {
            if (slowPorts.contains(portnum)) {
                slowPorts.remove(portnum);

            } else if (fastPorts.contains(portnum)) {
                fastPorts.remove(portnum);
                portProbeCount.remove(portnum);
                // no iterator to update
            } else {
                log.warn("Tried to dynamically remove non-existing port {}", portnum);
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
    public void ackProbe(Long portNumber) {
        synchronized (this) {
            if (slowPorts.contains(portNumber)) {
                log.debug("Setting slow port to fast: {}:{}", device.id(), portNumber);
                slowPorts.remove(portNumber);
                fastPorts.add(portNumber);
                portProbeCount.put(portNumber, new AtomicInteger(0));
            } else if (fastPorts.contains(portNumber)) {
                portProbeCount.get(portNumber).set(0);
            } else {
                log.debug("Got ackProbe for non-existing port: {}", portNumber);
            }
        }
    }


    /**
     * Handles an incoming LLDP packet. Creates link in topology and sends ACK
     * to port where LLDP originated.
     *
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
            PortNumber srcPort = portNumber(onoslldp.getPort());
            PortNumber dstPort = context.inPacket().receivedFrom().port();
            DeviceId srcDeviceId = DeviceId.deviceId(onoslldp.getDeviceString());
            DeviceId dstDeviceId = context.inPacket().receivedFrom().deviceId();
            ackProbe(dstPort.toLong());

            ConnectPoint src = new ConnectPoint(srcDeviceId, srcPort);
            ConnectPoint dst = new ConnectPoint(dstDeviceId, dstPort);

            LinkDescription ld = eth.getEtherType() == Ethernet.TYPE_LLDP ?
                    new DefaultLinkDescription(src, dst, Type.DIRECT) :
                    new DefaultLinkDescription(src, dst, Type.INDIRECT);

            try {
                linkProvider.linkDetected(ld);
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
        if (!mastershipService.isLocalMaster(device.id())) {
            if (!isStopped()) {
                // reschedule timer
                timeout = Timer.getTimer().newTimeout(this, probeRate, MILLISECONDS);
            }
            return;
        }

        log.trace("Sending probes from {}", device.id());
        synchronized (this) {
            Iterator<Long> fastIterator = fastPorts.iterator();
            while (fastIterator.hasNext()) {
                long portNumber = fastIterator.next();
                int probeCount = portProbeCount.get(portNumber).getAndIncrement();

                if (probeCount < LinkDiscovery.MAX_PROBE_COUNT) {
                    log.trace("Sending fast probe to port {}", portNumber);
                    sendProbes(portNumber);

                } else {
                    // Link down, demote to slowPorts; update fast and slow ports
                    fastIterator.remove();
                    slowPorts.add(portNumber);
                    portProbeCount.remove(portNumber);

                    ConnectPoint cp = new ConnectPoint(device.id(), portNumber(portNumber));
                    log.debug("Link down -> {}", cp);
                    linkProvider.linksVanished(cp);
                }
            }

            // send a probe for the next slow port
            for (long portNumber : slowPorts) {
                log.trace("Sending slow probe to port {}", portNumber);
                sendProbes(portNumber);
            }
        }

        if (!isStopped()) {
            // reschedule timer
            timeout = Timer.getTimer().newTimeout(this, probeRate, MILLISECONDS);
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
        pktService.emit(pkt);
        if (useBDDP) {
            OutboundPacket bpkt = createOutBoundBDDP(portNumber);
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
