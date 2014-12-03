/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.provider.of.link.impl;

import static org.onosproject.openflow.controller.Dpid.uri;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link.Type;
import org.onosproject.net.PortNumber;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ONLabLddp;
import org.onlab.packet.ONLabLddp.DPIDandPort;
import org.onlab.util.Timer;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;



/**
 * Run discovery process from a physical switch. Ports are initially labeled as
 * slow ports. When an LLDP is successfully received, label the remote port as
 * fast. Every probeRate milliseconds, loop over all fast ports and send an
 * LLDP, send an LLDP for a single slow port. Based on FlowVisor topology
 * discovery implementation.
 */
@Deprecated
public class LinkDiscovery implements TimerTask {

    private final OpenFlowSwitch sw;
    // send 1 probe every probeRate milliseconds
    private final long probeRate;
    private final Set<Integer> slowPorts;
    private final Set<Integer> fastPorts;
    // number of unacknowledged probes per port
    private final Map<Integer, AtomicInteger> portProbeCount;
    // number of probes to send before link is removed
    private static final short MAX_PROBE_COUNT = 3;
    private Iterator<Integer> slowIterator;
    private final OFFactory ofFactory;
    private final Logger log = getLogger(getClass());
    private final ONLabLddp lldpPacket;
    private final Ethernet ethPacket;
    private Ethernet bddpEth;
    private final boolean useBDDP;
    private final OpenFlowController ctrl;
    private final LinkProviderService linkProvider;
    protected final Map<Integer, OFPortDesc> ports;
    private Timeout timeout;

    /*
     * Instantiates discovery manager for the given physical switch. Creates a
     * generic LLDP packet that will be customized for the port it is sent out on.
     * Starts the the timer for the discovery process.
     *
     * @param sw the physical switch
     * @param useBDDP flag to also use BDDP for discovery
     */
    public LinkDiscovery(final OpenFlowSwitch sw,
            OpenFlowController ctrl, LinkProviderService providerService, Boolean... useBDDP) {
        this.sw = sw;
        this.ofFactory = sw.factory();
        this.ctrl = ctrl;
        this.probeRate = 3000;
        this.linkProvider = providerService;
        this.slowPorts = Collections.synchronizedSet(new HashSet<Integer>());
        this.fastPorts = Collections.synchronizedSet(new HashSet<Integer>());
        this.ports = new ConcurrentHashMap<>();
        this.portProbeCount = new HashMap<Integer, AtomicInteger>();
        this.lldpPacket = new ONLabLddp();
        this.lldpPacket.setSwitch(this.sw.getId());
        this.ethPacket = new Ethernet();
        this.ethPacket.setEtherType(Ethernet.TYPE_LLDP);
        this.ethPacket.setDestinationMACAddress(ONLabLddp.LLDP_NICIRA);
        this.ethPacket.setPayload(this.lldpPacket);
        this.ethPacket.setPad(true);
        this.useBDDP = useBDDP.length > 0 ? useBDDP[0] : false;
        if (this.useBDDP) {
            this.bddpEth = new Ethernet();
            this.bddpEth.setPayload(this.lldpPacket);
            this.bddpEth.setEtherType(Ethernet.TYPE_BSN);
            this.bddpEth.setDestinationMACAddress(ONLabLddp.BDDP_MULTICAST);
            this.bddpEth.setPad(true);
            log.info("Using BDDP to discover network");
        }
        for (OFPortDesc port : sw.getPorts()) {
            if (port.getPortNo() != OFPort.LOCAL) {
                addPort(port);
            }
        }
        timeout = Timer.getTimer().newTimeout(this, 0,
                TimeUnit.MILLISECONDS);
        this.log.info("Started discovery manager for switch {}",
                sw.getId());

    }

    /**
     * Add physical port port to discovery process.
     * Send out initial LLDP and label it as slow port.
     *
     * @param port the port
     */
    public void addPort(final OFPortDesc port) {
        // Ignore ports that are not on this switch, or already booted. */
        this.ports.put(port.getPortNo().getPortNumber(), port);
        synchronized (this) {
            this.log.debug("sending init probe to port {}",
                    port.getPortNo().getPortNumber());
            OFPacketOut pkt;

            pkt = this.createLLDPPacketOut(port);
            this.sw.sendMsg(pkt);
            if (useBDDP) {
                OFPacketOut bpkt = this.createBDDPPacketOut(port);
                this.sw.sendMsg(bpkt);
            }

            this.slowPorts.add(port.getPortNo().getPortNumber());
        }

    }

    /**
     * Removes physical port from discovery process.
     *
     * @param port the port
     */
    public void removePort(final OFPortDesc port) {
        // Ignore ports that are not on this switch

        int portnum = port.getPortNo().getPortNumber();
        this.ports.remove(portnum);
        synchronized (this) {
            if (this.slowPorts.contains(portnum)) {
                this.slowPorts.remove(portnum);

            } else if (this.fastPorts.contains(portnum)) {
                this.fastPorts.remove(portnum);
                this.portProbeCount.remove(portnum);
                // no iterator to update
            } else {
                this.log.warn(
                        "tried to dynamically remove non-existing port {}",
                        portnum);
            }
        }
        ConnectPoint cp = new ConnectPoint(
                DeviceId.deviceId(uri(sw.getId())),
                PortNumber.portNumber(port.getPortNo().getPortNumber()));
        linkProvider.linksVanished(cp);

    }

    /**
     * Method called by remote port to acknowledge receipt of LLDP sent by
     * this port. If slow port, updates label to fast. If fast port, decrements
     * number of unacknowledged probes.
     *
     * @param port the port
     */
    public void ackProbe(final Integer port) {
        final int portNumber = port;
        synchronized (this) {
            if (this.slowPorts.contains(portNumber)) {
                this.log.debug("Setting slow port to fast: {}:{}",
                        this.sw.getId(), portNumber);
                this.slowPorts.remove(portNumber);
                this.fastPorts.add(portNumber);
                this.portProbeCount.put(portNumber, new AtomicInteger(0));
            } else {
                if (this.fastPorts.contains(portNumber)) {
                    this.portProbeCount.get(portNumber).set(0);
                } else {
                    this.log.debug(
                            "Got ackProbe for non-existing port: {}",
                            portNumber);
                }
            }
        }
    }

    /**
     * Creates packet_out LLDP for specified output port.
     *
     * @param port the port
     * @return Packet_out message with LLDP data
     */
    private OFPacketOut createLLDPPacketOut(final OFPortDesc port) {
        if (port == null) {
            return null;
        }
        OFPacketOut.Builder packetOut = this.ofFactory.buildPacketOut();
        packetOut.setBufferId(OFBufferId.NO_BUFFER);
        OFAction act = this.ofFactory.actions().buildOutput()
                .setPort(port.getPortNo()).build();
        packetOut.setActions(Collections.singletonList(act));
        this.lldpPacket.setPort(port.getPortNo().getPortNumber());
        this.ethPacket.setSourceMACAddress(port.getHwAddr().getBytes());

        final byte[] lldp = this.ethPacket.serialize();
        packetOut.setData(lldp);
        return packetOut.build();
    }

    /**
     * Creates packet_out BDDP for specified output port.
     *
     * @param port the port
     * @return Packet_out message with LLDP data
     */
    private OFPacketOut createBDDPPacketOut(final OFPortDesc port) {
        if (port == null) {
            return null;
        }
        OFPacketOut.Builder packetOut = sw.factory().buildPacketOut();

        packetOut.setBufferId(OFBufferId.NO_BUFFER);

        OFActionOutput.Builder act = sw.factory().actions().buildOutput()
                .setPort(port.getPortNo());
        OFAction out = act.build();
        packetOut.setActions(Collections.singletonList(out));
        this.lldpPacket.setPort(port.getPortNo().getPortNumber());
        this.bddpEth.setSourceMACAddress(port.getHwAddr().getBytes());

        final byte[] bddp = this.bddpEth.serialize();
        packetOut.setData(bddp);

        return packetOut.build();
    }


    private void sendMsg(final OFMessage msg) {
        if (msg == null) {
            return;
        }
        this.sw.sendMsg(msg);
    }

    public String getName() {
        return "LinkDiscovery " + this.sw.getStringId();
    }

    /*
     * Handles an incoming LLDP packet. Creates link in topology and sends ACK
     * to port where LLDP originated.
     */
    public boolean handleLLDP(final byte[] pkt, Integer inPort) {

        short ethType = ONLabLddp.isOVXLLDP(pkt);
        if (ethType == Ethernet.TYPE_LLDP || ethType == Ethernet.TYPE_BSN) {
            final Integer dstPort = inPort;
            final DPIDandPort dp = ONLabLddp.parseLLDP(pkt);
            final OpenFlowSwitch srcSwitch = ctrl.getSwitch(new Dpid(dp.getDpid()));
            final Integer srcPort = dp.getPort();
            if (srcSwitch == null) {
                return true;
            }
            this.ackProbe(srcPort);
            ConnectPoint src = new ConnectPoint(
                    DeviceId.deviceId(uri(srcSwitch.getId())),
                    PortNumber.portNumber(srcPort));

            ConnectPoint dst = new ConnectPoint(
                    DeviceId.deviceId(uri(sw.getId())),
                    PortNumber.portNumber(dstPort));
            LinkDescription ld;
            if (ethType == Ethernet.TYPE_BSN) {
                ld = new DefaultLinkDescription(src, dst, Type.INDIRECT);
            } else {
                ld = new DefaultLinkDescription(src, dst, Type.DIRECT);
            }
            linkProvider.linkDetected(ld);
            return true;
        } else {
            this.log.debug("Ignoring unknown LLDP");
            return false;
        }
    }

    private OFPortDesc findPort(Integer inPort) {
        return ports.get(inPort);
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
        this.log.debug("sending probes");
        synchronized (this) {
            final Iterator<Integer> fastIterator = this.fastPorts.iterator();
            while (fastIterator.hasNext()) {
                final Integer portNumber = fastIterator.next();
                OFPortDesc port = findPort(portNumber);
                if (port == null) {
                    // port can be null
                    // #removePort modifies `ports` outside synchronized block
                    continue;
                }
                final int probeCount = this.portProbeCount.get(portNumber)
                        .getAndIncrement();
                if (probeCount < LinkDiscovery.MAX_PROBE_COUNT) {
                    this.log.debug("sending fast probe to port");

                    OFPacketOut pkt = this.createLLDPPacketOut(port);
                    this.sendMsg(pkt);
                    if (useBDDP) {
                        OFPacketOut bpkt = this.createBDDPPacketOut(port);
                        this.sendMsg(bpkt);
                    }
                } else {
                    // Update fast and slow ports
                    fastIterator.remove();
                    this.slowPorts.add(portNumber);
                    this.portProbeCount.remove(portNumber);

                    // Remove link from topology
                    final OFPortDesc srcPort = port;

                    ConnectPoint cp = new ConnectPoint(
                            DeviceId.deviceId(uri(sw.getId())),
                            PortNumber.portNumber(srcPort.getPortNo().getPortNumber()));
                    linkProvider.linksVanished(cp);
                }
            }

            // send a probe for the next slow port
            if (!this.slowPorts.isEmpty()) {
                this.slowIterator = this.slowPorts.iterator();
                while (this.slowIterator.hasNext()) {
                    final int portNumber = this.slowIterator.next();
                    this.log.debug("sending slow probe to port {}", portNumber);
                    OFPortDesc port = findPort(portNumber);

                    OFPacketOut pkt = this.createLLDPPacketOut(port);
                    this.sendMsg(pkt);
                    if (useBDDP) {
                        OFPacketOut bpkt = this.createBDDPPacketOut(port);
                        this.sendMsg(bpkt);
                    }

                }
            }
        }

        // reschedule timer
        timeout = Timer.getTimer().newTimeout(this, this.probeRate,
                TimeUnit.MILLISECONDS);
    }

    public void removeAllPorts() {
        for (OFPortDesc port : ports.values()) {
            removePort(port);
        }
    }

    public void stop() {
        timeout.cancel();
        removeAllPorts();
    }

}
