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
package org.onosproject.pim.impl;

import com.google.common.collect.ImmutableList;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.PIM;
import org.onlab.packet.pim.PIMAddrUnicast;
import org.onlab.packet.pim.PIMHello;
import org.onlab.packet.pim.PIMHelloOption;
import org.onlab.packet.pim.PIMJoinPrune;
import org.onlab.packet.pim.PIMJoinPruneGroup;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * PIM Interface represents an ONOS Interface with IP and MAC addresses for
 * a given ConnectPoint.
 */
public final class PimInterface {

    private final Logger log = getLogger(getClass());

    private static final int JOIN_PERIOD = 60;
    private static final double HOLD_TIME_MULTIPLIER = 3.5;

    private final PacketService packetService;

    private Interface onosInterface;
    private final TrafficTreatment outputTreatment;

    // Our hello opt holdtime
    private short holdtime = PIMHelloOption.DEFAULT_HOLDTIME;

    // Our hello opt prune delay
    private int pruneDelay = PIMHelloOption.DEFAULT_PRUNEDELAY;

    // Neighbor priority
    private int priority   = PIMHelloOption.DEFAULT_PRIORITY;

    private final int helloInterval;

    private long lastHello;

    // Our current genid
    private final int generationId;

    // The IP address of the DR
    private IpAddress drIpaddress;

    // A map of all our PIM neighbors keyed on our neighbors IP address
    private Map<IpAddress, PimNeighbor> pimNeighbors = new ConcurrentHashMap<>();

    private Map<McastRoute, RouteData> routes = new ConcurrentHashMap<>();

    /**
     * Create a PIMInterface from an ONOS Interface.
     *
     * @param intf the ONOS Interface.
     * @param holdTime hold time
     * @param priority priority
     * @param propagationDelay propagation delay
     * @param overrideInterval override interval
     * @param packetService reference to the packet service
     */
    private PimInterface(Interface intf,
                         int helloInterval,
                         short holdTime,
                         int priority,
                         short propagationDelay,
                         short overrideInterval,
                         PacketService packetService) {

        onosInterface = intf;
        outputTreatment = createOutputTreatment();
        this.helloInterval = helloInterval;
        this.holdtime = holdTime;
        this.packetService = packetService;
        IpAddress ourIp = getIpAddress();
        MacAddress mac = intf.mac();

        lastHello = 0;

        generationId = new Random().nextInt();

        // Create a PIM Neighbor to represent ourselves for DR election.
        PimNeighbor us = new PimNeighbor(ourIp, mac, holdTime, 0, priority, generationId);

        pimNeighbors.put(ourIp, us);
        drIpaddress = ourIp;
    }

    private TrafficTreatment createOutputTreatment() {
        return DefaultTrafficTreatment.builder()
                .setOutput(onosInterface.connectPoint().port())
                .build();
    }

    /**
     * Return the ONOS Interface.
     *
     * @return ONOS Interface.
     */
    public Interface getInterface() {
        return onosInterface;

    }

    /**
     * Set the ONOS Interface, it will override a previous value.
     *
     * @param intf ONOS Interface
     * @return PIM interface instance
     */
    public PimInterface setInterface(Interface intf) {
        onosInterface = intf;
        return this;
    }

    /**
     * Get the set of IP Addresses associated with this interface.
     *
     * @return a set of Ip Addresses on this interface
     */
    public List<InterfaceIpAddress> getIpAddresses() {
        return onosInterface.ipAddressesList();
    }

    /**
     * Return a single "best" IP address.
     *
     * @return the choosen IP address or null if none
     */
    public IpAddress getIpAddress() {
        if (onosInterface.ipAddressesList().isEmpty()) {
            return null;
        }

        IpAddress ipaddr = null;
        for (InterfaceIpAddress ifipaddr : onosInterface.ipAddressesList()) {
            ipaddr = ifipaddr.ipAddress();
            break;
        }
        return ipaddr;
    }

    /**
     * Get the holdtime.
     *
     * @return the holdtime
     */
    public short getHoldtime() {
        return holdtime;
    }

    /**
     * Get the prune delay.
     *
     * @return The prune delay
     */
    public int getPruneDelay() {
        return pruneDelay;
    }

    /**
     * Get our hello priority.
     *
     * @return our priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Get our generation ID.
     *
     * @return our generation ID
     */
    public int getGenerationId() {
        return generationId;
    }

    /**
     * Gets the neighbors seen on this interface.
     *
     * @return PIM neighbors
     */
    public Collection<PimNeighbor> getNeighbors() {
        return ImmutableList.copyOf(pimNeighbors.values());
    }

    public Collection<McastRoute> getRoutes() {
        return routes.keySet();
    }

    /**
     * Checks whether any of our neighbors have expired, and cleans up their
     * state if they have.
     */
    public void checkNeighborTimeouts() {
        Set<PimNeighbor> expired = pimNeighbors.values().stream()
                // Don't time ourselves out!
                .filter(neighbor -> !neighbor.ipAddress().equals(getIpAddress()))
                .filter(neighbor -> neighbor.isExpired())
                .collect(Collectors.toSet());

        for (PimNeighbor neighbor : expired) {
            log.info("Timing out neighbor {}", neighbor);
            pimNeighbors.remove(neighbor.ipAddress(), neighbor);
        }
    }

    /**
     * Multicast a hello message out our interface.  This hello message is sent
     * periodically during the normal PIM Neighbor refresh time, as well as a
     * result of a newly created interface.
     */
    public void sendHello() {
        if (lastHello + TimeUnit.SECONDS.toMillis(helloInterval) >
                System.currentTimeMillis()) {
            return;
        }

        lastHello = System.currentTimeMillis();

        // Create the base PIM Packet and mark it a hello packet
        PimPacket pimPacket = new PimPacket(PIM.TYPE_HELLO);

        // We need to set the source MAC and IPv4 addresses
        pimPacket.setSrcMacAddr(onosInterface.mac());
        pimPacket.setSrcIpAddress(Ip4Address.valueOf(getIpAddress().toOctets()));

        // Create the hello message with options
        PIMHello hello = new PIMHello();
        hello.createDefaultOptions();
        hello.addOption(PIMHelloOption.createHoldTime(holdtime));
        hello.addOption(PIMHelloOption.createPriority(priority));
        hello.addOption(PIMHelloOption.createGenID(generationId));

        // Now set the hello option payload
        pimPacket.setPimPayload(hello);

        packetService.emit(new DefaultOutboundPacket(
                onosInterface.connectPoint().deviceId(),
                outputTreatment,
                ByteBuffer.wrap(pimPacket.getEthernet().serialize())));
    }

    /**
     * Process an incoming PIM Hello message.  There are a few things going on in
     * this method:
     * <ul>
     *     <li>We <em>may</em> have to create a new neighbor if one does not already exist</li>
     *     <li>We <em>may</em> need to re-elect a new DR if new information is received</li>
     *     <li>We <em>may</em> need to send an existing neighbor all joins if the genid changed</li>
     *     <li>We will refresh the neighbor's timestamp</li>
     * </ul>
     *
     * @param ethPkt the Ethernet packet header
     */
    public void processHello(Ethernet ethPkt) {
        if (log.isTraceEnabled()) {
            log.trace("Received a PIM hello packet");
        }

        // We'll need to save our neighbors MAC address
        MacAddress nbrmac = ethPkt.getSourceMAC();

        // And we'll need to save neighbors IP Address.
        IPv4 iphdr = (IPv4) ethPkt.getPayload();
        IpAddress srcip = IpAddress.valueOf(iphdr.getSourceAddress());

        PIM pimhdr = (PIM) iphdr.getPayload();
        if (pimhdr.getPimMsgType() != PIM.TYPE_HELLO) {
            log.error("process Hello has received a non hello packet type: " + pimhdr.getPimMsgType());
            return;
        }

        // get the DR values for later calculation
        PimNeighbor dr = pimNeighbors.get(drIpaddress);
        checkNotNull(dr);

        IpAddress drip = drIpaddress;
        int drpri = dr.priority();

        // Assume we do not need to run a DR election
        boolean reElectDr = false;
        boolean genidChanged = false;

        PIMHello hello = (PIMHello) pimhdr.getPayload();

        // Determine if we already have a PIMNeighbor
        PimNeighbor nbr = pimNeighbors.getOrDefault(srcip, null);
        PimNeighbor newNbr = PimNeighbor.createPimNeighbor(srcip, nbrmac, hello.getOptions().values());

        if (nbr == null) {
            pimNeighbors.putIfAbsent(srcip, newNbr);
            nbr = newNbr;
        } else if (!nbr.equals(newNbr)) {
            if (newNbr.holdtime() == 0) {
                // Neighbor has shut down. Remove them and clean up
                pimNeighbors.remove(srcip, nbr);
                return;
            } else {
                // Neighbor has changed one of their options.
                pimNeighbors.put(srcip, newNbr);
                nbr = newNbr;
            }
        }

        // Refresh this neighbor's timestamp
        nbr.refreshTimestamp();

        /*
         * the election method will first determine if an election
         * needs to be run, if so it will run the election.  The
         * IP address of the DR will be returned.  If the IP address
         * of the DR is different from what we already have we know a
         * new DR has been elected.
         */
        IpAddress electedIp = election(nbr, drip, drpri);
        if (!drip.equals(electedIp)) {
            // we have a new DR.
            drIpaddress = electedIp;
        }
    }

    // Run an election if we need to.  Return the elected IP address.
    private IpAddress election(PimNeighbor nbr, IpAddress drIp, int drPriority) {

        IpAddress nbrIp = nbr.ipAddress();
        if (nbr.priority() > drPriority) {
            return nbrIp;
        }

        if (nbrIp.compareTo(drIp) > 0) {
            return nbrIp;
        }
        return drIp;
    }

    /**
     * Process an incoming PIM JoinPrune message.
     *
     * @param ethPkt the Ethernet packet header.
     */
    public void processJoinPrune(Ethernet ethPkt) {

        IPv4 ip = (IPv4) ethPkt.getPayload();
        checkNotNull(ip);

        PIM pim = (PIM) ip.getPayload();
        checkNotNull(pim);

        PIMJoinPrune jpHdr = (PIMJoinPrune) pim.getPayload();
        checkNotNull(jpHdr);

        /*
         * The Join/Prune messages are grouped by Group address. We'll walk each group address
         * where we will possibly have to walk a list of source address for the joins and prunes.
         */
        Collection<PIMJoinPruneGroup> jpgs = jpHdr.getJoinPrunes();
        for (PIMJoinPruneGroup jpg : jpgs) {
            IpPrefix gpfx = jpg.getGroup();

            // Walk the joins first.
            for (IpPrefix spfx : jpg.getJoins().values()) {

                // We may need


            }

            for (IpPrefix spfx : jpg.getPrunes().values()) {

                // TODO: this is where we many need to remove multi-cast state and possibly intents.

            }
        }

    }

    public void addRoute(McastRoute route, IpAddress nextHop, MacAddress nextHopMac) {
        RouteData data = new RouteData(nextHop, nextHopMac);
        routes.put(route, data);

        sendJoinPrune(route, data, true);
    }

    public void removeRoute(McastRoute route) {
        RouteData data = routes.remove(route);

        if (data != null) {
            sendJoinPrune(route, data, false);
        }
    }

    public void sendJoins() {
        routes.entrySet().forEach(entry -> {
            if (entry.getValue().timestamp + TimeUnit.SECONDS.toMillis(JOIN_PERIOD) >
                    System.currentTimeMillis()) {
                return;
            }

            sendJoinPrune(entry.getKey(), entry.getValue(), true);
        });
    }

    private void sendJoinPrune(McastRoute route, RouteData data, boolean join) {
        PIMJoinPrune jp = new PIMJoinPrune();

        jp.addJoinPrune(route.source().toIpPrefix(), route.group().toIpPrefix(), join);
        jp.setHoldTime(join ? (short) Math.floor(JOIN_PERIOD * HOLD_TIME_MULTIPLIER) : 0);
        jp.setUpstreamAddr(new PIMAddrUnicast(data.ipAddress.toString()));

        PIM pim = new PIM();
        pim.setPIMType(PIM.TYPE_JOIN_PRUNE_REQUEST);
        pim.setPayload(jp);

        IPv4 ipv4 = new IPv4();
        ipv4.setDestinationAddress(PIM.PIM_ADDRESS.getIp4Address().toInt());
        ipv4.setSourceAddress(getIpAddress().getIp4Address().toInt());
        ipv4.setProtocol(IPv4.PROTOCOL_PIM);
        ipv4.setTtl((byte) 1);
        ipv4.setDiffServ((byte) 0xc0);
        ipv4.setPayload(pim);

        Ethernet eth = new Ethernet();
        eth.setSourceMACAddress(onosInterface.mac());
        eth.setDestinationMACAddress(MacAddress.valueOf("01:00:5E:00:00:0d"));
        eth.setEtherType(Ethernet.TYPE_IPV4);
        eth.setPayload(ipv4);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(onosInterface.connectPoint().port())
                .build();

        packetService.emit(new DefaultOutboundPacket(onosInterface.connectPoint().deviceId(),
                treatment, ByteBuffer.wrap(eth.serialize())));

        data.timestamp = System.currentTimeMillis();
    }

    /**
     * Returns a builder for a PIM interface.
     *
     * @return PIM interface builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a PIM interface.
     */
    public static class Builder {
        private Interface intf;
        private PacketService packetService;
        private int helloInterval = PimInterfaceManager.DEFAULT_HELLO_INTERVAL;
        private short holdtime = PIMHelloOption.DEFAULT_HOLDTIME;
        private int priority   = PIMHelloOption.DEFAULT_PRIORITY;
        private short propagationDelay = PIMHelloOption.DEFAULT_PRUNEDELAY;
        private short overrideInterval = PIMHelloOption.DEFAULT_OVERRIDEINTERVAL;

        /**
         * Uses the specified ONOS interface.
         *
         * @param intf ONOS interface
         * @return this PIM interface builder
         */
        public Builder withInterface(Interface intf) {
            this.intf = checkNotNull(intf);
            return this;
        }

        /**
         * Sets the reference to the packet service.
         *
         * @param packetService packet service
         * @return this PIM interface builder
         */
        public Builder withPacketService(PacketService packetService) {
            this.packetService = checkNotNull(packetService);
            return this;
        }

        /**
         * Users the specified hello interval.
         *
         * @param helloInterval hello interval in seconds
         * @return this PIM interface builder
         */
        public Builder withHelloInterval(int helloInterval) {
            this.helloInterval = helloInterval;
            return this;
        }

        /**
         * Uses the specified hold time.
         *
         * @param holdTime hold time in seconds
         * @return this PIM interface builder
         */
        public Builder withHoldTime(short holdTime) {
            this.holdtime = holdTime;
            return this;
        }

        /**
         * Uses the specified DR priority.
         *
         * @param priority DR priority
         * @return this PIM interface builder
         */
        public Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Uses the specified propagation delay.
         *
         * @param propagationDelay propagation delay in ms
         * @return this PIM interface builder
         */
        public Builder withPropagationDelay(short propagationDelay) {
            this.propagationDelay = propagationDelay;
            return this;
        }

        /**
         * Uses the specified override interval.
         *
         * @param overrideInterval override interval in ms
         * @return this PIM interface builder
         */
        public Builder withOverrideInterval(short overrideInterval) {
            this.overrideInterval = overrideInterval;
            return this;
        }

        /**
         * Builds the PIM interface.
         *
         * @return PIM interface
         */
        public PimInterface build() {
            checkArgument(intf != null, "Must provide an interface");
            checkArgument(packetService != null, "Must provide a packet service");

            return new PimInterface(intf, helloInterval, holdtime, priority,
                    propagationDelay, overrideInterval, packetService);
        }

    }

    private static class RouteData {
        final IpAddress ipAddress;
        final MacAddress macAddress;
        long timestamp;

        public RouteData(IpAddress ip, MacAddress mac) {
            this.ipAddress = ip;
            this.macAddress = mac;
            timestamp = System.currentTimeMillis();
        }
    }
}
