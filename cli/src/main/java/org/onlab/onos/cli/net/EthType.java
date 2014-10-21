package org.onlab.onos.cli.net;

import org.onlab.packet.Ethernet;

/**
 * Allowed values for Ethernet types.  Used by the CLI completer for
 * connectivity based intent L2 parameters.
 */
public enum EthType {
    /** ARP. */
    ARP(Ethernet.TYPE_ARP),
    /** RARP. */
    RARP(Ethernet.TYPE_RARP),
    /** IPV4. */
    IPV4(Ethernet.TYPE_IPV4),
    /** LLDP. */
    LLDP(Ethernet.TYPE_LLDP),
    /** BSN. */
    BSN(Ethernet.TYPE_BSN);

    private short value;

    /**
     * Constructs an EthType with the given value.
     *
     * @param value value to use when this EthType is seen.
     */
    private EthType(short value) {
        this.value = value;
    }

    /**
     * Gets the value to use for this EthType.
     *
     * @return short value to use for this EthType
     */
    public short value() {
        return this.value;
    }
}
