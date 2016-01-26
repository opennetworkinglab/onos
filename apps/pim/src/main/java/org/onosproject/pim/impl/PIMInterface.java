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
import org.onlab.packet.MacAddress;
import org.onlab.packet.PIM;
import org.onlab.packet.pim.PIMHello;
import org.onlab.packet.pim.PIMHelloOption;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.host.InterfaceIpAddress;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * PIM Interface represents an ONOS Interface with IP and MAC addresses for
 * a given ConnectPoint.
 */
public class PIMInterface {

    private final Logger log = getLogger(getClass());

    private Interface onosInterface;

    // Our hello opt holdtime
    private short holdtime = PIMHelloOption.DEFAULT_HOLDTIME;

    // Our hello opt prune delay
    private int pruneDelay = PIMHelloOption.DEFAULT_PRUNEDELAY;

    // Neighbor priority
    private int priority   = PIMHelloOption.DEFAULT_PRIORITY;

    // Our current genid
    private int genid      = PIMHelloOption.DEFAULT_GENID;   // Needs to be assigned.

    // The IP address of the DR
    IpAddress drIpaddress;

    // A map of all our PIM neighbors keyed on our neighbors IP address
    private Map<IpAddress, PIMNeighbor> pimNeighbors = new HashMap<>();

    /**
     * Create a PIMInterface from an ONOS Interface.
     *
     * @param intf the ONOS Interface.
     */
    public PIMInterface(Interface intf) {
        onosInterface = intf;
        IpAddress ourIp = getIpAddress();
        MacAddress mac = intf.mac();

        // Create a PIM Neighbor to represent ourselves for DR election.
        PIMNeighbor us = new PIMNeighbor(ourIp, mac);

        // Priority and IP address are all we need to DR election.
        us.setPriority(priority);

        pimNeighbors.put(ourIp, us);
        drIpaddress = ourIp;
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
    public PIMInterface setInterface(Interface intf) {
        onosInterface = intf;
        return this;
    }

    /**
     * Get the set of IP Addresses associated with this interface.
     *
     * @return a set of Ip Addresses on this interface
     */
    public Set<InterfaceIpAddress> getIpAddresses() {
        return onosInterface.ipAddresses();
    }

    /**
     * Return a single "best" IP address.
     *
     * @return the choosen IP address or null if none
     */
    public IpAddress getIpAddress() {
        if (onosInterface.ipAddresses().isEmpty()) {
            return null;
        }

        IpAddress ipaddr = null;
        for (InterfaceIpAddress ifipaddr : onosInterface.ipAddresses()) {
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
    public int getGenid() {
        return genid;
    }

    /**
     * Multicast a hello message out our interface.  This hello message is sent
     * periodically during the normal PIM Neighbor refresh time, as well as a
     * result of a newly created interface.
     */
    public void sendHello() {

        // Create the base PIM Packet and mark it a hello packet
        PIMPacket pimPacket = new PIMPacket(PIM.TYPE_HELLO);

        // We need to set the source MAC and IPv4 addresses
        pimPacket.setSrcMacAddr(onosInterface.mac());
        pimPacket.setSrcIpAddress(Ip4Address.valueOf(getIpAddress().toOctets()));

        // Create the hello message with options
        PIMHello hello = new PIMHello();
        hello.createDefaultOptions();

        // Now set the hello option payload
        pimPacket.setPIMPayload(hello);

        // TODO: How to send the packet.?.
    }

    /**
     * Process an incoming PIM Hello message.  There are a few things going on in
     * this method:
     * <ul>
     *     <li>We <em>may</em> have to create a new neighbor if one does not already exist</li>
     *     <li>We <em>may</em> need to re-elect a new DR if new information is received</li>
     *     <li>We <em>may</em> need to send an existing neighbor all joins if the genid changed</li>
     *     <li>We will refresh the neighbors timestamp</li>
     * </ul>
     *
     * @param ethPkt the Ethernet packet header
     */
    public void processHello(Ethernet ethPkt) {

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
        PIMNeighbor dr = pimNeighbors.get(drIpaddress);
        checkNotNull(dr);

        IpAddress drip = drIpaddress;
        int drpri = dr.getPriority();

        // Assume we do not need to run a DR election
        boolean reElectDr = false;
        boolean genidChanged = false;

        PIMHello hello = (PIMHello) pimhdr.getPayload();

        // Determine if we already have a PIMNeighbor
        PIMNeighbor nbr = pimNeighbors.getOrDefault(srcip, null);
        if (nbr == null) {
            nbr = new PIMNeighbor(srcip, hello.getOptions());
            checkNotNull(nbr);
        } else {
            Integer previousGenid = nbr.getGenid();
            nbr.addOptions(hello.getOptions());
            if (previousGenid != nbr.getGenid()) {
                genidChanged = true;
            }
        }

        // Refresh this neighbors timestamp
        nbr.refreshTimestamp();

        /*
         * the election method will frist determine if an election
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
    private IpAddress election(PIMNeighbor nbr, IpAddress drip, int drpri) {

        IpAddress nbrip = nbr.getIpaddr();
        if (nbr.getPriority() > drpri) {
            return nbrip;
        }

        if (nbrip.compareTo(drip) > 0) {
            return nbrip;
        }
        return drip;
    }

    /**
     * Process an incoming PIM JoinPrune message.
     *
     * @param ethPkt the Ethernet packet header.
     */
    public void processJoinPrune(Ethernet ethPkt) {
        // TODO: add Join/Prune processing code.
    }
}
