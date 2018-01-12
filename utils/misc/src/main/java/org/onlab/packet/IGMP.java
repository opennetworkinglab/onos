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

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.PacketUtils.checkInput;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implements IGMP control packet format.
 */
public abstract class IGMP extends BasePacket {
    private static final Logger log = getLogger(IGMP.class);

    public static final byte TYPE_IGMPV3_MEMBERSHIP_QUERY = 0x11;
    public static final byte TYPE_IGMPV1_MEMBERSHIP_REPORT = 0x12;
    public static final byte TYPE_IGMPV2_MEMBERSHIP_REPORT = 0x16;
    public static final byte TYPE_IGMPV2_LEAVE_GROUP = 0x17;
    public static final byte TYPE_IGMPV3_MEMBERSHIP_REPORT = 0x22;

    List<IGMPGroup> groups = new ArrayList<>();

    // Fields contained in the IGMP header
    protected byte igmpType;
    protected byte resField = 0;
    protected short checksum = 0;

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
    public abstract void setMaxRespCode(byte respCode);

    /**
     * Get the list of IGMPGroups.  The group objects will be either IGMPQuery or IGMPMembership
     * depending on the IGMP message type.  For IGMP Query, the groups list should only be
     * one group.
     *
     * @return The list of IGMP groups.
     */
    public List<IGMPGroup> getGroups() {
        return ImmutableList.copyOf(groups);
    }

    /**
     * Add a multicast group to this IGMP message.
     *
     * @param group the IGMPGroup will be IGMPQuery or IGMPMembership depending on the message type.
     * @return true if group was valid and added, false otherwise.
     */
    public abstract boolean addGroup(IGMPGroup group);

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

        if (this instanceof IGMPv3) {
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
        } else if (this instanceof IGMPv2) {
            if (this.groups.isEmpty()) {
                bb.putInt(0);
            } else {
                bb.putInt(groups.get(0).getGaddr().getIp4Address().toInt());
            }
        } else {
            throw new UnsupportedOperationException();
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
     * Deserializer function for IPv4 packets.
     *
     * @return deserializer function
     */
    public static Deserializer<IGMP> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, IGMPv2.HEADER_LENGTH);

            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            byte igmpType = bb.get();
            boolean isV2;
            if (igmpType == TYPE_IGMPV2_MEMBERSHIP_REPORT  || igmpType == TYPE_IGMPV2_LEAVE_GROUP ||
                    length == IGMPv2.HEADER_LENGTH) {
                isV2 = true;
            } else {
                isV2 = false;
            }

            IGMP igmp = isV2 ? new IGMPv2() : new IGMPv3();

            igmp.igmpType = igmpType;
            igmp.resField = bb.get();
            igmp.checksum = bb.getShort();

            if (isV2) {
                igmp.addGroup(new IGMPQuery(IpAddress.valueOf(bb.getInt()), 0));
                if (igmp.validChecksum()) {
                    return igmp;
                }
                throw new DeserializationException("invalid checksum");
            }

            // second check for IGMPv3
            checkInput(data, offset, length, IGMPv3.MINIMUM_HEADER_LEN);

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

    /**
     * Validates the message's checksum.
     *
     * @return true if valid, false if not
     */
    protected abstract boolean validChecksum();

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

    public static class IGMPv3 extends IGMP {
        public static final int MINIMUM_HEADER_LEN = 12;

        @Override
        public void setMaxRespCode(byte respCode) {
            if (igmpType != IGMP.TYPE_IGMPV3_MEMBERSHIP_QUERY) {
                log.debug("Requesting the max response code for an incorrect field: ");
            }
            this.resField = respCode;
        }

        @Override
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

        @Override
        protected boolean validChecksum() {
            return true; //FIXME
        }
    }

    public static class IGMPv2 extends IGMP {
        public static final int HEADER_LENGTH = 8;

        @Override
        public void setMaxRespCode(byte respCode) {
            this.resField = respCode;
        }

        @Override
        public boolean addGroup(IGMPGroup group) {
            if (groups.isEmpty()) {
                groups = ImmutableList.of(group);
                return true;
            }
            return false;
        }

        @Override
        protected boolean validChecksum() {
            int accumulation = (((int) this.igmpType) & 0xff) << 8;
            accumulation += ((int) this.resField) & 0xff;
            if (!groups.isEmpty()) {
                int ipaddr = groups.get(0).getGaddr().getIp4Address().toInt();
                accumulation += (ipaddr >> 16) & 0xffff;
                accumulation += ipaddr & 0xffff;
            }
            accumulation = (accumulation >> 16 & 0xffff)
                    + (accumulation & 0xffff);
            short checksum = (short) (~accumulation & 0xffff);
            return checksum == this.checksum;
        }
    }
}
