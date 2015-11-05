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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.InterfaceIpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The PIM Interface is a wrapper around a ConnectPoint and used to provide
 * hello options values when "talking" with PIM other PIM routers.
 */
public class PIMInterface {
    private static Logger log = LoggerFactory.getLogger("PIMInterfaces");

    // Interface from the interface subsystem
    private Interface theInterface;

    // The list of PIM neighbors adjacent to this interface
    private Map<IpAddress, PIMNeighbor> neighbors = new HashMap<>();

    // The designatedRouter for this LAN
    private PIMNeighbor designatedRouter;

    // The priority we use on this ConnectPoint.
    private int priority = PIMHelloOption.DEFAULT_PRIORITY;

    // The holdtime we are sending out.
    private int holdtime = PIMHelloOption.DEFAULT_HOLDTIME;

    // Then generation ID we are sending out. 0 means we need to generate a new random ID
    private int genid = PIMHelloOption.DEFAULT_GENID;

    // Our default prune delay
    private int prunedelay = PIMHelloOption.DEFAULT_PRUNEDELAY;

    /**
     * Create a PIMInterface.
     *
     * @param intf the network interface configuration
     */
    public PIMInterface(Interface intf) {

        log.debug("Adding an interface: " + intf.toString() + "\n");
        this.theInterface = intf;

        // Send a hello to let our neighbors know we are alive
        sendHello();
    }

    /**
     * Get the PIM Interface.
     *
     * @return the PIM Interface
     */
    public Interface getInterface() {
        return theInterface;
    }

    /**
     * Getter for our IP address.
     *
     * @return our IP address.
     */
    public IpAddress getIpAddress() {
        if (theInterface.ipAddresses().isEmpty()) {
            return null;
        }

        // We will just assume the first interface on the list
        IpAddress ipaddr = null;
        for (InterfaceIpAddress ifipaddr : theInterface.ipAddresses()) {
            ipaddr = ifipaddr.ipAddress();
            break;
        }
        return ipaddr;
    }

    /**
     * Get our priority.
     *
     * @return our priority.
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * Get the designated router on this connection.
     *
     * @return the PIMNeighbor representing the DR
     */
    public PIMNeighbor getDesignatedRouter() {
        return designatedRouter;
    }

    /**
     * Are we the DR on this CP?
     *
     * @return true if we are, false if not
     */
    public boolean areWeDr() {
        return (designatedRouter != null &&
                designatedRouter.getPrimaryAddr().equals(this.getIpAddress()));
    }

    /**
     * Return a collection of PIM Neighbors.
     *
     * @return the collection of PIM Neighbors
     */
    public Collection<PIMNeighbor> getNeighbors() {
        return this.neighbors.values();
    }

    /**
     * Find the neighbor with the given IP address on this CP.
     *
     * @param ipaddr the IP address of the neighbor we are interested in
     * @return the pim neighbor if it exists
     */
    public PIMNeighbor findNeighbor(IpAddress ipaddr) {
        PIMNeighbor nbr = neighbors.get(ipaddr);
        return nbr;
    }

    /**
     * Add a new PIM neighbor to this list.
     *
     * @param nbr the neighbor to be added.
     */
    public void addNeighbor(PIMNeighbor nbr) {
        if (neighbors.containsKey(nbr.getPrimaryAddr())) {

            log.debug("We are adding a neighbor that already exists: {}", nbr.toString());
            neighbors.remove(nbr.getPrimaryAddr());
        }
        neighbors.put(nbr.getPrimaryAddr(), nbr);
    }

    /**
     * Remove the neighbor from our neighbor list.
     *
     * @param ipaddr the IP address of the neighbor to remove
     */
    public void removeNeighbor(IpAddress ipaddr) {

        if (neighbors.containsKey(ipaddr)) {
            neighbors.remove(ipaddr);
        }
        this.electDR();
    }

    /**
     * Remove the given neighbor from the neighbor list.
     *
     * @param nbr the nbr to be removed.
     */
    public void removeNeighbor(PIMNeighbor nbr) {

        neighbors.remove(nbr.getPrimaryAddr(), nbr);
        this.electDR();
    }

    /**
     * Elect a new DR on this ConnectPoint.
     *
     * @return the PIM Neighbor that wins
     */
    public PIMNeighbor electDR() {

        for (PIMNeighbor nbr : this.neighbors.values()) {
            if (this.designatedRouter == null) {
                this.designatedRouter = nbr;
                continue;
            }

            if (nbr.getPriority() > this.designatedRouter.getPriority()) {
                this.designatedRouter = nbr;
                continue;
            }

            // We could sort in ascending order
            if (this.designatedRouter.getPrimaryAddr().compareTo(nbr.getPrimaryAddr()) > 0) {
                this.designatedRouter = nbr;
                continue;
            }
        }

        return this.designatedRouter;
    }

    /**
     * Elect a new DR given the new neighbor.
     *
     * @param nbr the new neighbor to use in DR election.
     * @return the PIM Neighbor that wins DR election
     */
    public PIMNeighbor electDR(PIMNeighbor nbr) {

        // Make sure I have
        if (this.designatedRouter == null ||
                this.designatedRouter.getPriority() < nbr.getPriority() ||
                this.designatedRouter.getPrimaryAddr().compareTo(nbr.getPrimaryAddr()) > 0) {
            this.designatedRouter = nbr;
        }
        return this.designatedRouter;
    }

    /**
     * Find or create a pim neighbor with a given ip address and connect point.
     *
     * @param ipaddr of the pim neighbor
     * @param mac The mac address of our sending neighbor
     * @return an existing or new PIM neighbor
     */
    public PIMNeighbor findOrCreate(IpAddress ipaddr, MacAddress mac) {
        PIMNeighbor nbr = this.findNeighbor(ipaddr);
        if (nbr == null) {
            nbr = new PIMNeighbor(ipaddr, mac, this);
            this.addNeighbor(nbr);
            this.electDR(nbr);
        }
        return nbr;
    }

    /**
     * Process a hello packet received on this Interface.
     *
     * @param ethPkt the ethernet packet containing the hello message
     * @param cp the ConnectPoint of this interface
     */
    public void processHello(Ethernet ethPkt, ConnectPoint cp) {
        checkNotNull(ethPkt);
        checkNotNull(cp);

        MacAddress srcmac = ethPkt.getSourceMAC();
        IPv4 ip = (IPv4) ethPkt.getPayload();
        Ip4Address srcip = Ip4Address.valueOf(ip.getSourceAddress());

        PIM pim = (PIM) ip.getPayload();
        checkNotNull(pim);

        PIMHello hello = (PIMHello) pim.getPayload();
        checkNotNull(hello);

        PIMNeighbor nbr = this.findOrCreate(srcip, srcmac);
        if (nbr == null) {
            log.error("Could not create a neighbor for: {1}", srcip.toString());
            return;
        }

        ConnectPoint icp = theInterface.connectPoint();
        checkNotNull(icp);
        if (!cp.equals(icp)) {
            log.error("PIM Hello message received from {} on incorrect interface {}",
                    nbr.getPrimaryAddr(), this.toString());
            return;
        }
        nbr.refresh(hello);
    }

    /**
     * Send a hello packet from this interface.
     */
    public void sendHello() {
        PIM pim = new PIM();
        PIMHello hello = new PIMHello();

        // Create a PIM Hello
        pim = new PIM();
        pim.setVersion((byte) 2);
        pim.setPIMType((byte) PIM.TYPE_HELLO);
        pim.setChecksum((short) 0);

        hello = new PIMHello();
        hello.createDefaultOptions();
        pim.setPayload(hello);
        hello.setParent(pim);

        log.debug("Sending hello: \n");
        PIMPacketHandler.getInstance().sendPacket(pim, this);
    }

    /**
     * prints the connectPointNeighbors list with each neighbor list.
     *
     * @return string of neighbors.
     */
    public String printNeighbors() {
        String out = "PIM Neighbors Table: \n";
        for (PIMNeighbor nbr : this.neighbors.values()) {
            out += "\t" + nbr.toString();
        }
        return out;
    }

    @Override
    public String toString() {
        IpAddress ipaddr = this.getIpAddress();
        String out = "PIM Neighbors: ";
        if (ipaddr != null) {
            out += "IP: " + ipaddr.toString();
        } else {
            out += "IP: *Null*";
        }
        out += "\tPR: " + String.valueOf(this.priority) + "\n";
        return out;
    }

}

