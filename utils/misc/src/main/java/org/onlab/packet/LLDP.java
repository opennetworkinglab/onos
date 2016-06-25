/*
 * Copyright 2014-present Open Networking Laboratory
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

/**
 *
 */
package org.onlab.packet;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.*;

/**
 * Representation of an LLDP Packet.
 */
public class LLDP extends BasePacket {
    public static final byte CHASSIS_TLV_TYPE = 1;
    public static final short CHASSIS_TLV_SIZE = 7;
    public static final byte CHASSIS_TLV_SUBTYPE = 4;

    public static final byte PORT_TLV_TYPE = 2;
    public static final short PORT_TLV_SIZE = 5;
    public static final byte PORT_TLV_SUBTYPE = 2;

    public static final byte TTL_TLV_TYPE = 3;
    public static final short TTL_TLV_SIZE = 2;

    protected LLDPTLV chassisId;
    protected LLDPTLV portId;
    protected LLDPTLV ttl;
    protected List<LLDPTLV> optionalTLVList;
    protected short ethType;

    public LLDP() {
        this.optionalTLVList = new LinkedList<>();
        this.ethType = Ethernet.TYPE_LLDP;
    }

    /**
     * @return the chassisId
     */
    public LLDPTLV getChassisId() {
        return this.chassisId;
    }

    /**
     * @param chassis the chassisId to set
     * @return this
     */
    public LLDP setChassisId(final LLDPTLV chassis) {
        this.chassisId = chassis;
        return this;
    }

    /**
     * @return the portId
     */
    public LLDPTLV getPortId() {
        return this.portId;
    }

    /**
     * @param portId the portId to set
     * @return this
     */
    public LLDP setPortId(final LLDPTLV portId) {
        this.portId = portId;
        return this;
    }

    /**
     * @return the ttl
     */
    public LLDPTLV getTtl() {
        return this.ttl;
    }

    /**
     * @param ttl the ttl to set
     * @return this
     */
    public LLDP setTtl(final LLDPTLV ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     * @return the optionalTLVList
     */
    public List<LLDPTLV> getOptionalTLVList() {
        return this.optionalTLVList;
    }

    /**
     * @param optionalTLVList the optionalTLVList to set
     * @return this
     */
    public LLDP setOptionalTLVList(final List<LLDPTLV> optionalTLVList) {
        this.optionalTLVList = optionalTLVList;
        return this;
    }

    @Override
    public byte[] serialize() {
        int length = 2 + this.chassisId.getLength() + 2
                + this.portId.getLength() + 2 + this.ttl.getLength() + 2;
        for (final LLDPTLV tlv : this.optionalTLVList) {
            length += 2 + tlv.getLength();
        }

        final byte[] data = new byte[length];
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.chassisId.serialize());
        bb.put(this.portId.serialize());
        bb.put(this.ttl.serialize());
        for (final LLDPTLV tlv : this.optionalTLVList) {
            bb.put(tlv.serialize());
        }
        bb.putShort((short) 0); // End of LLDPDU

        /*
         * if (this.parent != null && this.parent instanceof Ethernet) {
         * ((Ethernet) this.parent).setEtherType(this.ethType); }
         */

        return data;
    }

    @Override
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        LLDPTLV tlv;
        do {
            try {
                tlv = new LLDPOrganizationalTLV().deserialize(bb);
            } catch (DeserializationException e) {
                break;
            }

            // if there was a failure to deserialize stop processing TLVs
            if (tlv == null) {
                break;
            }
            switch (tlv.getType()) {
                case 0x0:
                    // can throw this one away, its just an end delimiter
                    break;
                case 0x1:
                    this.chassisId = tlv;
                    break;
                case 0x2:
                    this.portId = tlv;
                    break;
                case 0x3:
                    this.ttl = tlv;
                    break;

                default:
                    this.optionalTLVList.add(tlv);
                    break;
            }
        } while (tlv.getType() != 0 && bb.hasRemaining());
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 883;
        int result = super.hashCode();
        result = prime * result
                + (this.chassisId == null ? 0 : this.chassisId.hashCode());
        result = prime * result + this.optionalTLVList.hashCode();
        result = prime * result
                + (this.portId == null ? 0 : this.portId.hashCode());
        result = prime * result + (this.ttl == null ? 0 : this.ttl.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof LLDP)) {
            return false;
        }
        final LLDP other = (LLDP) obj;
        if (this.chassisId == null) {
            if (other.chassisId != null) {
                return false;
            }
        } else if (!this.chassisId.equals(other.chassisId)) {
            return false;
        }
        if (!this.optionalTLVList.equals(other.optionalTLVList)) {
            return false;
        }
        if (this.portId == null) {
            if (other.portId != null) {
                return false;
            }
        } else if (!this.portId.equals(other.portId)) {
            return false;
        }
        if (this.ttl == null) {
            if (other.ttl != null) {
                return false;
            }
        } else if (!this.ttl.equals(other.ttl)) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer function for LLDP packets.
     *
     * @return deserializer function
     */
    public static Deserializer<LLDP> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, 0);

            LLDP lldp = new LLDP();

            int currentIndex = 0;

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            LLDPTLV tlv;
            do {
                // Each new TLV must be a minimum of 2 bytes
                // (containing the type and length fields).
                currentIndex += 2;
                checkHeaderLength(length, currentIndex);

                tlv = new LLDPOrganizationalTLV().deserialize(bb);

                // if there was a failure to deserialize stop processing TLVs
                if (tlv == null) {
                    break;
                }
                switch (tlv.getType()) {
                    case 0x0:
                        // can throw this one away, it's just an end delimiter
                        break;
                    case 0x1:
                        lldp.chassisId = tlv;
                        break;
                    case 0x2:
                        lldp.portId = tlv;
                        break;
                    case 0x3:
                        lldp.ttl = tlv;
                        break;
                    default:
                        lldp.optionalTLVList.add(tlv);
                        break;
                }

                currentIndex += tlv.getLength();
            } while (tlv.getType() != 0);

            return lldp;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("chassisId", Arrays.toString(chassisId.getValue()))
                .add("portId", Arrays.toString(portId.getValue()))
                .add("ttl", Arrays.toString(ttl.getValue()))
                .add("ethType", Short.toString(ethType))
                .toString();

        // TODO: need to handle optionalTLVList
    }
}
