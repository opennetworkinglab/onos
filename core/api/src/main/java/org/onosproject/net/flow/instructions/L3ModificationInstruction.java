/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.flow.instructions;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.util.Objects;

/**
 * Abstraction of a single traffic treatment step.
 */
public abstract class L3ModificationInstruction implements Instruction {

    private static final String SEPARATOR = ":";

    /**
     * Represents the type of traffic treatment.
     */
    public enum L3SubType {
        /**
         * IPv4 src modification.
         */
        IPV4_SRC,

        /**
         * IPv4 dst modification.
         */
        IPV4_DST,

        /**
         * IPv6 src modification.
         */
        IPV6_SRC,

        /**
         * IPv6 dst modification.
         */
        IPV6_DST,

        /**
         * IPv6 flow label modification.
         */
        IPV6_FLABEL,

        /**
         * Decrement TTL.
         */
        DEC_TTL,

        /**
         * Copy TTL out.
         */
        TTL_OUT,

        /**
         * Copy TTL in.
         */
        TTL_IN,

        /**
         * ARP IP src modification.
         */
        ARP_SPA,

        /**
         * ARP Ether src modification.
         */
        ARP_SHA,

        /**
         * ARP IP dst modification.
         */
        ARP_TPA,

        /**
         * ARP Ether dst modification.
         */
        ARP_THA,

        /**
         * Arp operation modification.
         */
        ARP_OP,

        /**
         * IP DSCP operation modification.
         */
        IP_DSCP
    }

    /**
     * Returns the subtype of the modification instruction.
     * @return type of instruction
     */
    public abstract L3SubType subtype();

    @Override
    public final Type type() {
        return Type.L3MODIFICATION;
    }

    /**
     * Represents a L3 src/dst modification instruction.
     */
    public static final class ModIPInstruction extends L3ModificationInstruction {

        private final L3SubType subtype;
        private final IpAddress ip;

        ModIPInstruction(L3SubType subType, IpAddress addr) {

            this.subtype = subType;
            this.ip = addr;
        }

        @Override
        public L3SubType subtype() {
            return this.subtype;
        }

        public IpAddress ip() {
            return this.ip;
        }

        @Override
        public String toString() {
            return subtype().toString() + SEPARATOR + ip;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), ip);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModIPInstruction) {
                ModIPInstruction that = (ModIPInstruction) obj;
                return  Objects.equals(ip, that.ip) &&
                        Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }

    /**
     * Represents a L3 ARP IP src/dst modification instruction.
     */
    public static final class ModArpIPInstruction extends L3ModificationInstruction {

        private final L3SubType subtype;
        private final IpAddress ip;

        ModArpIPInstruction(L3SubType subType, IpAddress addr) {

            this.subtype = subType;
            this.ip = addr;
        }

        @Override
        public L3SubType subtype() {
            return this.subtype;
        }

        public IpAddress ip() {
            return this.ip;
        }

        @Override
        public String toString() {
            return subtype().toString() + SEPARATOR + ip;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), ip);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModArpIPInstruction) {
                ModArpIPInstruction that = (ModArpIPInstruction) obj;
                return  Objects.equals(ip, that.ip) &&
                        Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }

    /**
     * Represents a L3 ARP Ether src/dst modification instruction.
     */
    public static final class ModArpEthInstruction extends L3ModificationInstruction {

        private final L3SubType subtype;
        private final MacAddress mac;

        ModArpEthInstruction(L3SubType subType, MacAddress addr) {

            this.subtype = subType;
            this.mac = addr;
        }

        @Override
        public L3SubType subtype() {
            return this.subtype;
        }

        public MacAddress mac() {
            return this.mac;
        }

        @Override
        public String toString() {
            return subtype().toString() + SEPARATOR + mac;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), mac);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModArpEthInstruction) {
                ModArpEthInstruction that = (ModArpEthInstruction) obj;
                return  Objects.equals(mac, that.mac) &&
                        Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }

    /**
     * Represents a L3 ARP operation modification instruction.
     */
    public static final class ModArpOpInstruction extends L3ModificationInstruction {

        private final L3SubType subtype;
        private final short op;

        ModArpOpInstruction(L3SubType subType, short op) {

            this.subtype = subType;
            this.op = op;
        }

        @Override
        public L3SubType subtype() {
            return this.subtype;
        }

        public long op() {
            return this.op;
        }

        @Override
        public String toString() {
            return subtype().toString() + SEPARATOR + op;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), op);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModArpOpInstruction) {
                ModArpOpInstruction that = (ModArpOpInstruction) obj;
                return  Objects.equals(op, that.op) &&
                        Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }

    /**
     * Represents a L3 IPv6 Flow Label (RFC 6437) modification instruction
     * (20 bits unsigned integer).
     */
    public static final class ModIPv6FlowLabelInstruction
        extends L3ModificationInstruction {
        private static final int MASK = 0xfffff;
        private final int flowLabel;            // IPv6 flow label: 20 bits

        /**
         * Creates a new flow mod instruction.
         *
         * @param flowLabel the IPv6 flow label to set in the treatment (20 bits)
         */
        ModIPv6FlowLabelInstruction(int flowLabel) {
            this.flowLabel = flowLabel & MASK;
        }

        @Override
        public L3SubType subtype() {
            return L3SubType.IPV6_FLABEL;
        }

        /**
         * Gets the IPv6 flow label to set in the treatment.
         *
         * @return the IPv6 flow label to set in the treatment (20 bits)
         */
        public int flowLabel() {
            return this.flowLabel;
        }

        @Override
        public String toString() {
            return subtype().toString() + SEPARATOR + Long.toHexString(flowLabel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), flowLabel);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModIPv6FlowLabelInstruction) {
                ModIPv6FlowLabelInstruction that =
                    (ModIPv6FlowLabelInstruction) obj;
                return  Objects.equals(flowLabel, that.flowLabel);
            }
            return false;
        }
    }

    /**
     * Represents a L3 TTL modification instruction.
     */
    public static final class ModTtlInstruction extends L3ModificationInstruction {

        private final L3SubType subtype;

        ModTtlInstruction(L3SubType subtype) {
            this.subtype = subtype;
        }

        @Override
        public L3SubType subtype() {
            return this.subtype;
        }

        @Override
        public String toString() {
            return subtype().toString();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModTtlInstruction) {
                ModTtlInstruction that = (ModTtlInstruction) obj;
                return  Objects.equals(this.subtype(), that.subtype());
            }
            return false;
        }
    }

    /**
     * Represents a L3 DSCP modification instruction.
     */
    public static final class ModDscpInstruction extends L3ModificationInstruction {

        private final L3SubType subtype;
        private final byte dscp;

        ModDscpInstruction(L3SubType subtype, byte dscp) {
            this.subtype = subtype;
            this.dscp = dscp;
        }

        @Override
        public L3SubType subtype() {
            return this.subtype;
        }

        @Override
        public String toString() {
            return subtype().toString() + SEPARATOR + dscp();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type(), subtype(), dscp());
        }

        public byte dscp() {
            return this.dscp;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ModDscpInstruction) {
                ModDscpInstruction that = (ModDscpInstruction) obj;
                return  Objects.equals(this.subtype(), that.subtype()) &&
                        Objects.equals(this.dscp(), that.dscp());
            }
            return false;
        }
    }
}
