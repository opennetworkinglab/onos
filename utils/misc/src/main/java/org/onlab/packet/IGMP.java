/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.PacketUtils.checkInput;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implements IGMP control packet format.
 */
public class IGMP extends BasePacket {
    private static final Logger log = getLogger(IGMP.class);

    public static final byte TYPE_IGMPV3_MEMBERSHIP_QUERY = 0x11;
    public static final byte TYPE_IGMPV1_MEMBERSHIP_REPORT = 0x12;
    public static final byte TYPE_IGMPV2_MEMBERSHIP_REPORT = 0x16;
    public static final byte TYPE_IGMPV2_LEAVE_GROUP = 0x17;
    public static final byte TYPE_IGMPV3_MEMBERSHIP_REPORT = 0x22;
    public static final Map<Byte, Deserializer<? extends IPacket>> PROTOCOL_DESERIALIZER_MAP = new HashMap<>();

    public static final int MINIMUM_HEADER_LEN = 12;

    List<IGMPGroup> groups = new ArrayList<>();

    // Fields contained in the IGMP header
    private byte igmpType;
    private byte resField = 0;
    private short checksum = 0;

    private byte[] unsupportTypeData;

    public IGMP() {
    }

    /**
     * Get the IGMP message type.
     *
     * @return the IGMP message type
     */
    public byte getIgmpType() {
        return igmpType;
    }

    /**
     * Set the IGMP message type.
     *
     * @param msgType IGMP message type
     */
    public void setIgmpType(byte msgType) {
        igmpType = msgType;
    }

    /**
     * Get the checksum of this message.
     *
     * @return the checksum
     */
    public short getChecksum() {
        return checksum;
    }

    /**
     * get the Max Resp Code.
     *
     * @return The Maximum Time allowed before before sending a responding report.
     */
    public byte getMaxRespField() {
        return resField;
    }

    /**
     * Set the Max Resp Code.
     *
     * @param respCode the Maximum Response Code.
     */
    public void setMaxRespCode(byte respCode) {
        if (igmpType != IGMP.TYPE_IGMPV3_MEMBERSHIP_QUERY) {
            log.debug("Requesting the max response code for an incorrect field: ");
        }
        this.resField = respCode;
    }

    /**
     * Get the list of IGMPGroups.  The group objects will be either IGMPQuery or IGMPMembership
     * depending on the IGMP message type.  For IGMP Query, the groups list should only be
     * one group.
     *
     * @return The list of IGMP groups.
     */
    public List<IGMPGroup> getGroups() {
        return groups;
    }

    /**
     * Add a multicast group to this IGMP message.
     *
     * @param group the IGMPGroup will be IGMPQuery or IGMPMembership depending on the message type.
     * @return true if group was valid and added, false otherwise.
     */
    public boolean addGroup(IGMPGroup group) {
        checkNotNull(group);
        switch (this.igmpType) {
            case TYPE_IGMPV3_MEMBERSHIP_QUERY:
                if (group instanceof IGMPMembership) {
                    return false;
                }

                if (group.sources.size() > 1) {
                    return false;
                }
                break;

            case TYPE_IGMPV3_MEMBERSHIP_REPORT:
                if (group instanceof IGMPQuery) {
                    return false;
                }
                break;

            default:
                log.debug("Warning no IGMP message type has been set");
        }

        this.groups.add(group);
        return true;
    }

    /**
     * Serialize this IGMP packet.  This will take care
     * of serializing IGMPv3 Queries and IGMPv3 Membership
     * Reports.
     *
     * @return the serialized IGMP message
     */
    @java.lang.SuppressWarnings("squid:S128") // suppress switch fall through warning
    @Override
    public byte[] serialize() {
        byte[] data = new byte[8915];

        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.getIgmpType());

        // reserved or max resp code depending on type.
        bb.put(this.resField);

        // Must calculate checksum
        bb.putShort((short) 0);



        switch (this.igmpType) {

            case IGMP.TYPE_IGMPV3_MEMBERSHIP_REPORT:
                // reserved
                bb.putShort((short) 0);
                // Number of groups
                bb.putShort((short) groups.size());
                // Fall through

            case IGMP.TYPE_IGMPV3_MEMBERSHIP_QUERY:

                for (IGMPGroup grp : groups) {
                    grp.serialize(bb);
                }
                break;

            default:
                bb.put(this.unsupportTypeData);
                break;
        }

        int size = bb.position();

        // compute checksum if needed
        if (this.checksum == 0) {
            bb.rewind();
            int accumulation = 0;
            for (int i = 0; i < size * 2; ++i) {
                accumulation += 0xffff & bb.getShort();
            }
            accumulation = (accumulation >> 16 & 0xffff)
                    + (accumulation & 0xffff);
            this.checksum = (short) (~accumulation & 0xffff);
            bb.putShort(2, this.checksum);
        }


        bb.position(0);
        byte[] rdata = new byte[size];
        bb.get(rdata, 0, size);
        return rdata;
    }

    /**
     * Deserialize an IGMP message.
     *
     * @param data bytes to deserialize
     * @param offset offset to start deserializing from
     * @param length length of the data to deserialize
     * @return populated IGMP object
     */
    @Override
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {

        IGMP igmp = new IGMP();
        try {
            igmp = IGMP.deserializer().deserialize(data, offset, length);
        } catch (DeserializationException e) {
            log.error(e.getStackTrace().toString());
            return this;
        }
        this.igmpType = igmp.igmpType;
        this.resField = igmp.resField;
        this.checksum = igmp.checksum;
        this.groups = igmp.groups;
        return this;
    }

    /**
     * Deserializer function for IPv4 packets.
     *
     * @return deserializer function
     */
    public static Deserializer<IGMP> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, MINIMUM_HEADER_LEN);

            IGMP igmp = new IGMP();

            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            igmp.igmpType = bb.get();
            igmp.resField = bb.get();
            igmp.checksum = bb.getShort();

            String msg;

            switch (igmp.igmpType) {

                case TYPE_IGMPV3_MEMBERSHIP_QUERY:
                    IGMPQuery qgroup = new IGMPQuery();
                    qgroup.deserialize(bb);
                    igmp.groups.add(qgroup);
                    break;

                case TYPE_IGMPV3_MEMBERSHIP_REPORT:
                    bb.getShort();  // Ignore resvd
                    int ngrps = bb.getShort();

                    for (; ngrps > 0; ngrps--) {
                        IGMPMembership mgroup = new IGMPMembership();
                        mgroup.deserialize(bb);
                        igmp.groups.add(mgroup);
                    }
                    break;

                /*
                 * NOTE: according to the IGMPv3 spec. These previous IGMP type fields
                 * must be supported.  At this time we are going to <b>assume</b> we run
                 * in a modern network where all devices are IGMPv3 capable.
                 */
                case TYPE_IGMPV1_MEMBERSHIP_REPORT:
                case TYPE_IGMPV2_MEMBERSHIP_REPORT:
                case TYPE_IGMPV2_LEAVE_GROUP:
                    igmp.unsupportTypeData = bb.array();  // Is this the entire array?
                    msg = "IGMP message type: " + igmp.igmpType + " is not supported";
                    igmp.log.debug(msg);
                    break;

                default:
                    msg = "IGMP message type: " + igmp.igmpType + " is not recognized";
                    igmp.unsupportTypeData = bb.array();
                    igmp.log.debug(msg);
                    break;
            }
            return igmp;
        };
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
        if (!(obj instanceof IGMP)) {
            return false;
        }
        final IGMP other = (IGMP) obj;
        if (this.igmpType != other.igmpType) {
            return false;
        }
        if (this.resField != other.resField) {
            return false;
        }
        if (this.checksum != other.checksum) {
            return false;
        }
        if (this.groups.size() != other.groups.size()) {
            return false;
        }
        // TODO: equals should be true regardless of order.
        if (!groups.equals(other.groups)) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 2521;
        int result = super.hashCode();
        result = prime * result + this.igmpType;
        result = prime * result + this.groups.size();
        result = prime * result + this.resField;
        result = prime * result + this.checksum;
        result = prime * result + this.groups.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("igmpType", Byte.toString(igmpType))
                .add("resField", Byte.toString(resField))
                .add("checksum", Short.toString(checksum))
                .add("unsupportTypeData", Arrays.toString(unsupportTypeData))
                .toString();
        // TODO: need to handle groups
    }
}
