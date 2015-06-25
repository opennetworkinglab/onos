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
package org.onlab.packet;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Representation of an Ethertype.
 */
public class EthType {

    public static final short ARP = EtherType.ARP.ethType.toShort();
    public static final short RARP = EtherType.RARP.ethType.toShort();
    public static final short VLAN = EtherType.VLAN.ethType.toShort();
    public static final short IPV4 = EtherType.IPV4.ethType.toShort();
    public static final short IPV6 = EtherType.IPV6.ethType.toShort();
    public static final short LLDP = EtherType.LLDP.ethType.toShort();
    public static final short BDDP = EtherType.BDDP.ethType.toShort();
    public static final short MPLS_MULTICAST = EtherType.MPLS_UNICAST.ethType.toShort();
    public static final short MPLS_UNICAST = EtherType.MPLS_UNICAST.ethType.toShort();

    private short etherType;

    /*
     * Reverse-lookup map for getting a EtherType enum
     */
    private static final Map<Short, EtherType> LOOKUP = Maps.newHashMap();

    static {
        for (EtherType eth : EtherType.values()) {
            LOOKUP.put(eth.ethType().toShort(), eth);
        }
    }

    public EthType(int etherType) {
        this.etherType = (short) (etherType & 0xFFFF);
    }

    public EthType(short etherType) {
        this.etherType = etherType;
    }

    public short toShort() {
        return etherType;
    }

    public static EtherType lookup(short etherType) {
        return LOOKUP.get(etherType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EthType ethType = (EthType) o;

        if (etherType != ethType.etherType) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return (int) etherType;
    }

    public String toString() {
        EtherType ethType = lookup(this.etherType);
        return (ethType == null ? "unknown" : ethType.toString());
    }

    public static enum EtherType {

        ARP(0x806, "arp", ARP.class),
        RARP(0x8035, "rarp", null),
        IPV4(0x800, "ipv4", IPv4.class),
        IPV6(0x86dd, "ipv6", IPv6.class),
        LLDP(0x88cc, "lldp", LLDP.class),
        VLAN(0x8100, "vlan", null),
        BDDP(0x8942, "bddp", LLDP.class),
        MPLS_UNICAST(0x8847, "mpls_unicast", null),
        MPLS_MULTICAST(0x8848, "mpls_unicast", null);


        private final Class clazz;
        private EthType ethType;
        private String type;

        EtherType(int ethType, String type, Class clazz) {
            this.ethType = new EthType(ethType);
            this.type = type;
            this.clazz = clazz;
        }

        public EthType ethType() {
            return ethType;
        }

        @Override
        public String toString() {
            return type;
        }

        public Class clazz() {
            return clazz;
        }


    }
}
