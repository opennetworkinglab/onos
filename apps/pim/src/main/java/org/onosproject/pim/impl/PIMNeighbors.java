
package org.onosproject.pim.impl;

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.PIM;
import org.onlab.packet.pim.PIMHello;
import org.onosproject.net.ConnectPoint;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * PIMNeighbors is a collection of all neighbors we have received
 * PIM hello messages from.  The main structure is a HashMap indexed
 * by ConnectPoint with another HashMap indexed on the PIM neighbors
 * IPAddress, it contains all PIM neighbors attached on that ConnectPoint.
 */
public final class PIMNeighbors {

    private static Logger log = LoggerFactory.getLogger("PIMNeighbors");

    /**
     * This is the global container for all PIM neighbors indexed by ConnectPoints.
     *
     * NOTE: We'll have a problem if the same neighbor can show up on two interfaces
     * but that should never happen.
     */
    private static HashMap<ConnectPoint, PIMNeighbors> connectPointNeighbors = new HashMap<>();

    // The connect point these neighbors are connected to.
    private ConnectPoint connectPoint;

    // Pointer to the current designated router on this ConnectPoint.
    private PIMNeighbor designatedRouter;

    // The list of neighbors we have learned on this ConnectPoint.
    private HashMap<IpAddress, PIMNeighbor> neighbors = new HashMap<>();

    /*
     * TODO: turn ourIpAddress, ourPriority and OurHoldTime into config options.
     */
    // The IP address we are using to source our PIM hello messages on this connect Point.
    private IpAddress ourIpAddress;

    // The priority we use on this ConnectPoint.
    private int ourPriority = 1;

    // The holdtime we are sending out.
    private int ourHoldtime = 105;

    // Then generation ID we are sending out. 0 means we need to generate a new random ID
    private int ourGenid = 0;

    // Hello Timer for sending hello messages per ConnectPoint with neighbors.
    private volatile Timeout helloTimer;

    // The period of which we will be sending out PIM hello messages.
    private final int defaultPimHelloInterval = 30; // seconds

    /**
     * Create PIMNeighbors object per ConnectPoint.
     *
     * @param cp the ConnectPoint.
     * @return PIMNeighbors structure
     */
    public static PIMNeighbors getConnectPointNeighbors(ConnectPoint cp) {
        return connectPointNeighbors.get(cp);
    }

    /**
     * Process incoming hello message, we will need the Macaddress and IP address of the sender.
     *
     * @param ethPkt the ethernet header
     * @param receivedFrom the connect point we recieved this message from
     */
    public static void processHello(Ethernet ethPkt, ConnectPoint receivedFrom) {
        checkNotNull(ethPkt);
        checkNotNull(ethPkt);

        MacAddress srcmac = ethPkt.getSourceMAC();
        IPv4 ip = (IPv4) ethPkt.getPayload();
        Ip4Address srcip = Ip4Address.valueOf(ip.getSourceAddress());

        PIM pim = (PIM) ip.getPayload();
        checkNotNull(pim);

        PIMHello hello = (PIMHello) pim.getPayload();
        checkNotNull(hello);

        PIMNeighbor nbr = PIMNeighbors.findOrCreate(srcip, srcmac, receivedFrom);
        if (nbr == null) {
            log.error("Could not create a neighbor for: {1}", srcip.toString());
            return;
        }

        nbr.setConnectPoint(receivedFrom);
        nbr.refresh(hello);
    }

    /**
     * Create a PIM Neighbor.
     *
     * @param cp The ConnectPoint this neighbor was found on
     */
    public PIMNeighbors(ConnectPoint cp) {
        this.connectPoint = cp;

        // TODO: use network config to assign address.
        this.ourIpAddress = IpAddress.valueOf("10.2.2.2");
        this.addIpAddress(this.ourIpAddress);
    }

    /**
     * Create a PIM neighbor.
     *
     * @param cp the ConnectPoint this neighbor was found on
     * @param ourIp the IP address of this neighbor
     */
    public PIMNeighbors(ConnectPoint cp, IpAddress ourIp) {
        this.connectPoint = cp;
        this.addIpAddress(ourIp);
    }

    /**
     * Start the hello timer when we have been given an IP address.
     *
     * @param ourIp our IP address.
     */
    public void addIpAddress(IpAddress ourIp) {
        this.startHelloTimer();

        // Kick off the first pim hello packet
        this.sendHelloPacket();
    }

    /**
     * Getter for our IP address.
     *
     * @return our IP address.
     */
    public IpAddress getOurIpAddress() {
        return this.ourIpAddress;
    }

    /**
     * Get our priority.
     *
     * @return our priority.
     */
    public int getOurPriority() {
        return this.ourPriority;
    }

    /**
     * Get the neighbor list for this specific connectPoint.
     *
     * @return PIM neighbors on this ConnectPoint
     */
    public HashMap<IpAddress, PIMNeighbor> getOurNeighborsList() {
        return this.neighbors;
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
    public boolean weAreTheDr() {
        return (designatedRouter != null &&
                designatedRouter.getPrimaryAddr().equals(ourIpAddress));
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

            // TODO: Hmmm, how should this be handled?
            log.debug("We are adding a neighbor that already exists: {}", nbr.toString());
            neighbors.remove(nbr.getPrimaryAddr(), nbr);
        }
        nbr.setNeighbors(this);
        neighbors.put(nbr.getPrimaryAddr(), nbr);
    }

    /**
     * Remove the neighbor from our neighbor list.
     *
     * @param ipaddr the IP address of the neighbor to remove
     */
    public void removeNeighbor(IpAddress ipaddr) {

        boolean reelect = (designatedRouter == null || designatedRouter.getPrimaryAddr().equals(ipaddr));
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

        boolean reelect = (designatedRouter == null || nbr.isDr());
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
     * @param cp the connect point the neighbor was learned from
     * @return an existing or new PIM neighbor
     */
    public static PIMNeighbor findOrCreate(IpAddress ipaddr, MacAddress mac, ConnectPoint cp) {
        PIMNeighbors neighbors = connectPointNeighbors.get(cp);
        if (neighbors == null) {
            neighbors = new PIMNeighbors(cp);
            connectPointNeighbors.put(cp, neighbors);
        }

        PIMNeighbor nbr = neighbors.findNeighbor(ipaddr);
        if (nbr == null) {
            nbr = new PIMNeighbor(ipaddr, mac, cp);
            neighbors.addNeighbor(nbr);
            neighbors.electDR(nbr);
        }
        return nbr;
    }

    // Returns the connect point neighbors hash map
    public static HashMap<ConnectPoint, PIMNeighbors> getConnectPointNeighbors() {
        return connectPointNeighbors;
    }

    /* ---------------------------------- PIM Hello Timer ----------------------------------- */

    /**
     * Start a new hello timer for this ConnectPoint.
     */
    private void startHelloTimer() {
        this.helloTimer = PIMTimer.getTimer().newTimeout(
                new HelloTimer(this),
                this.defaultPimHelloInterval,
                TimeUnit.SECONDS);

        log.trace("Started Hello Timer: " + this.ourIpAddress.toString());
    }

    /**
     * This inner class handles transmitting a PIM hello message on this ConnectPoint.
     */
    private final class HelloTimer implements TimerTask {
        PIMNeighbors neighbors;

        HelloTimer(PIMNeighbors neighbors) {
            this.neighbors = neighbors;
        }

        @Override
        public void run(Timeout timeout) throws Exception {

            // Send off a hello packet
            sendHelloPacket();

            // restart the hello timer
            neighbors.startHelloTimer();
        }
    }

    private void sendHelloPacket() {
        PIMHello hello = new PIMHello();

        // TODO: we will need to implement the network config service to assign ip addresses & options
        /*
        hello.createDefaultOptions();

        Ethernet eth = hello.createPIMHello(this.ourIpAddress);
        hello.sendPacket(this.connectPoint);
        */
    }

    /**
     * prints the connectPointNeighbors list with each neighbor list.
     *
     * @return string of neighbors.
     */
    public static String printPimNeighbors() {
        String out = "PIM Neighbors Table: \n";

        for (PIMNeighbors pn: connectPointNeighbors.values()) {

            out += "CP:\n " + pn.toString();
            for (PIMNeighbor nbr : pn.neighbors.values()) {
                out += "\t" + nbr.toString();
            }
        }
        return out;
    }

    @Override
    public String toString() {
        String out = "PIM Neighbors: ";
        if (this.ourIpAddress != null) {
            out += "IP: " + this.ourIpAddress.toString();
        } else {
            out += "IP: *Null*";
        }
        out += "\tPR: " + String.valueOf(this.ourPriority) + "\n";
        return out;
    }
}