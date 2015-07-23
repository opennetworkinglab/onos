package org.onlab.packet;

/**
 * Implement sending link status update to neighbor controllers.
 *
 * Need to support multiple message types, OSPF, BGP, etc
 */
public class LSAdvertisment {

    private int subnet;
    private int mask;
    private int routerId;

    public LSAdvertisment(int subnet, int mask, int routerId) {
        this.subnet = subnet;
        this.mask = mask;
        this.routerId = routerId;
    }

    public LSAdvertisment(LSAdvertisment lsa) {
        this.subnet = lsa.subnet;
        this.mask = lsa.mask;
        this.routerId = lsa.routerId;
    }

    public int getSubnet() {
        return this.subnet;
    }

    public int getMask() {
        return this.mask;
    }

    public int getRouterId() {
        return this.routerId;
    }
}
