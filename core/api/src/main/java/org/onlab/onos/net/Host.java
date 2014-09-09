package org.onlab.onos.net;

/**
 * Abstraction of an end-station host on the network, essentially a NIC.
 */
public interface Host extends Element {

    // MAC, IP(s), optional VLAN ID


    /**
     * Returns the most recent host location where the host attaches to the
     * network edge.
     *
     * @return host location
     */
    HostLocation location();

    // list of recent locations?

}
