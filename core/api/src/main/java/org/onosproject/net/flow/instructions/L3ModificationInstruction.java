/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * Abstraction of a single traffic treatment step.
 */
public abstract class L3ModificationInstruction implements Instruction {

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
        TTL_IN

        //TODO: remaining types
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
            return toStringHelper(subtype().toString())
                    .add("ip", ip).toString();
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
            return toStringHelper(subtype().toString())
                .add("flowLabel", Long.toHexString(flowLabel)).toString();
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
            return toStringHelper(subtype().toString())
                    .toString();
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
}
