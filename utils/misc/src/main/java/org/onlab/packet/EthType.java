/*
 * Copyright 2015-present Open Networking Foundation
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

/**
 * Representation of an Ethertype.
 */
public class EthType {

    /**
     * A list of known ethertypes. Adding a fully defined enum here will
     * associated the ethertype with a textual representation and a parsing
     * class.
     */
    public enum EtherType {

        ARP(0x806, "arp", org.onlab.packet.ARP.deserializer()),
        RARP(0x8035, "rarp", org.onlab.packet.ARP.deserializer()),
        IPV4(0x800, "ipv4", org.onlab.packet.IPv4.deserializer()),
        IPV6(0x86dd, "ipv6", org.onlab.packet.IPv6.deserializer()),
        LLDP(0x88cc, "lldp", org.onlab.packet.LLDP.deserializer()),
        VLAN(0x8100, "vlan", null),
        QINQ(0x88a8, "qinq", null),
        BDDP(0x8942, "bddp", org.onlab.packet.LLDP.deserializer()),
        MPLS_UNICAST(0x8847, "mpls_unicast", org.onlab.packet.MPLS.deserializer()),
        MPLS_MULTICAST(0x8848, "mpls_multicast", org.onlab.packet.MPLS.deserializer()),
        EAPOL(0x888e, "eapol", org.onlab.packet.EAPOL.deserializer()),
        PPPoED(0x8863, "pppoed", org.onlab.packet.PPPoED.deserializer()),
        SLOW(0x8809, "slow", org.onlab.packet.Slow.deserializer()),
        UNKNOWN(0, "unknown", null);


        private final EthType etherType;
        private final String type;
        private final Deserializer<?> deserializer;

        /**
         * Constructs a new ethertype.
         *
         * @param ethType The actual ethertype
         * @param type it's textual representation
         * @param deserializer a parser for this ethertype
         */
        EtherType(int ethType, String type, Deserializer<?> deserializer) {
            this.etherType = new EthType(ethType);
            this.type = type;
            this.deserializer = deserializer;
        }

        public EthType ethType() {
            return etherType;
        }

        @Override
        public String toString() {
            return type;
        }

        public Deserializer<?> deserializer() {
            return deserializer;
        }

        public static EtherType lookup(short etherType) {
            for (EtherType ethType : EtherType.values()) {
                if (ethType.ethType().toShort() == etherType) {
                    return ethType;
                }
            }
            return UNKNOWN;
        }

    }


    private final short etherType;

    /**
     * Builds the EthType.
     *
     * @param etherType an integer representing an ethtype
     */
    public EthType(int etherType) {
        this.etherType = (short) (etherType & 0xFFFF);
    }

    /**
     * Builds the EthType.
     *
     * @param etherType a short representing the ethtype
     */
    public EthType(short etherType) {
        this.etherType = etherType;
    }

    /**
     * Returns the short value for this ethtype.
     *
     * @return a short value
     */
    public short toShort() {
        return etherType;
    }

    /**
     * Looks up the ethertype by it's numerical representation
     * and returns it's textual format.
     *
     * @param etherType the short value of the ethertype
     * @return a textual representation
     */
    public EtherType lookup(short etherType) {
        for (EtherType ethType : EtherType.values()) {
            if (ethType.ethType().toShort() == etherType) {
                return ethType;
            }
        }
        return null;
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
        return (ethType == null ? String.format("0x%04x", etherType) :
                ethType.toString());
    }

}
