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
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.PIM;
import org.onlab.packet.pim.PIMHello;
import org.onlab.packet.pim.PIMHelloOption;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.host.InterfaceIpAddress;
import org.slf4j.Logger;

import java.util.Set;

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

    /**
     * Create a PIMInterface from an ONOS Interface.
     *
     * @param intf the ONOS Interface.
     */
    public PIMInterface(Interface intf) {
        onosInterface = intf;
    }

    /**
     * Return the ONOS Interface.
     *
     * @return ONOS Interface.
     */
    public Interface getInterface() {
        return this.onosInterface;
    }

    /**
     * Set the ONOS Interface, it will override a previous value.
     *
     * @param intf ONOS Interface.
     */
    public PIMInterface setInterface(Interface intf) {
        this.onosInterface = intf;
        return this;
    }

    /**
     * Get the set of IP Addresses associated with this interface.
     *
     * @return a set of Ip Addresses on this interface
     */
    public Set<InterfaceIpAddress> getIpAddresses() {
        return this.onosInterface.ipAddresses();
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
        return this.holdtime;
    }

    /**
     * Get the prune delay.
     *
     * @return The prune delay
     */
    public int getPruneDelay() {
        return this.pruneDelay;
    }

    /**
     * Get our hello priority.
     *
     * @return our priority
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * Get our generation ID.
     *
     * @return our generation ID
     */
    public int getGenid() {
        return this.genid;
    }

    /**
     * Process an incoming PIM Hello message.
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

        // TODO: Maybe a good idea to check the checksum.  Let's jump into the hello options.
        PIMHello hello = (PIMHello) pimhdr.getPayload();

        // TODO: this is about where we find or create our PIMNeighbor

        boolean reElectDr = false;

        // Start walking through all the hello options to handle accordingly.
        for (PIMHelloOption opt : hello.getOptions().values()) {

            // TODO: This is where we handle the options and modify the neighbor accordingly.
            // We'll need to create the PIMNeighbor class next.  Depending on what happens
            // we may need to re-elect a DR
        }

        if (reElectDr) {
            // TODO: create an election() method and call it here with a PIMNeighbor
        }
    }

    /**
     * Process an incoming PIM JoinPrune message.
     *
     * @param ethPkt the Ethernet packet header.
     */
    public void processJoinPrune(Ethernet ethPkt) {

    }
}
