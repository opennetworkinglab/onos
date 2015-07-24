package org.onlab.packet;

import static org.onlab.packet.PacketUtils.checkInput;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class OSPF extends BasePacket {
    public static final byte OSPF_HELLO = 0x01;
    public static final byte OSPF_DBD = 0x02;
    public static final byte OSPF_LSR = 0x03;
    public static final byte OSPF_LSU = 0x04;
    public static final byte OSPF_LSA = 0x05;

    public static final short OSPF_HEADER_LENGTH = 24;

    protected byte ospfVersionNum;
    protected byte ospfType;
    protected short ospfPktLen;
    protected int ospfRouterId;
    protected int ospfAreaId;
    protected short ospfChecksum;
    protected short ospfAuthType;
    protected long ospfAuth;
    protected int ospfNetMask;
    protected short ospfHelloInterval;
    protected short ospfSequenceNum;
    protected short ospfTtl;
    protected int ospfNumOfAdvertisments;
    protected ArrayList<LSAdvertisment> ospfAdvertismentList = new ArrayList<LSAdvertisment>();

    private static final Logger log = getLogger(OSPF.class);
    /**
     * Default constructor that sets the version to 2.
     */
    public OSPF() {
        super();
        this.ospfVersionNum = 0x02;
    }

    /**
     * @return the version
     */
    public byte getOspfVersionNum() {
        return this.ospfVersionNum;
    }

    /**
     * @param VersionNum
     *            the version to set
     * @return this
     */
    public OSPF setOspfVersion(final byte ospfVersionNum) {
        this.ospfVersionNum = ospfVersionNum;
        return this;
    }

    /**
     * @return the type
     */
    public byte getOspfType() {
        return this.ospfType;
    }

    /**
     * @param ospfType
     *            the type to set
     * @return this
     */
    public OSPF setOspfType(final byte ospfType) {
        this.ospfType = ospfType;
        return this;
    }

    /**
     * @return the packet length
     */
    public short getOspfPktLen() {
        return this.ospfPktLen;
    }

    /**
     * @param ospfPktLen
     *            the packet length to set
     * @return this
     */
    public OSPF setOspfPktLen(final short ospfPktLen) {
        this.ospfPktLen = ospfPktLen;
        return this;
    }

    /**
     * @return the router ID
     */
    public int getOspfRouterId() {
        return this.ospfRouterId;
    }

    /**
     * @param ospfRouterId
     *            the router ID to set
     * @return this
     */
    public OSPF setOspfRouterId(final int ospfRouterId) {
        this.ospfRouterId = ospfRouterId;
        return this;
    }

    /**
     * @return the area ID
     */
    public int getOspfAreaId() {
        return this.ospfAreaId;
    }

    /**
     * @param ospfAreaId
     *            the area ID to set
     * @return this
     */
    public OSPF setOspfAreaId(final int ospfAreaId) {
        this.ospfAreaId = ospfAreaId;
        return this;
    }

    /**
     * @return the checksum
     */
    public short getOspfChecksum() {
        return this.ospfChecksum;
    }

    /**
     * @param checksum
     *            the checksum to set
     * @return this
     */
    public OSPF setOspfChecksum(final short ospfChecksum) {
        this.ospfChecksum = ospfChecksum;
        return this;
    }

    // Extracted from OspfFrameDecode.java
    public short calculateOspfChecksum(byte[] packet, int pktLen) {
        // Sum consecutive 16-bit words.
        int sum = 0;
        int offset = 0;

        while (offset < pktLen - 1) {
            if (offset == 20) {
                offset += 8;
            }
            ByteBuffer wrapped = ByteBuffer.wrap(packet, offset, 2);
            sum += ((int) wrapped.getShort()) & 0x0000ffff;
            offset += 2;
        }

        if (offset == pktLen - 1) {
            sum += (packet[offset] >= 0 ? packet[offset] : packet[offset] ^ 0xffffff00) << 8;
        }

        // Add upper 16 bits to lower 16 bits.
        sum = (sum >>> 16) + (sum & 0xffff);

        // Add carry
        sum += sum >>> 16;

        // Ones complement and truncate.
        return (short) ~sum;
    }

    /**
     * @return the auth type
     */
    public short getOspfAuthType() {
        return this.ospfAuthType;
    }

    /**
     * @param ospfAuthType
     *            the auth type to set
     * @return this
     */
    public OSPF setOspfAuthType(final short ospfAuthType) {
        this.ospfAuthType = ospfAuthType;
        return this;
    }

    /**
     * @return the net mask
     */
    public int getOspfNetMask() {
        return this.ospfNetMask;
    }

    /**
     * @param ospfNetMask
     *            the net mask to set
     * @return this
     */
    public OSPF setOspfNetMask(final int ospfNetMask) {
        this.ospfNetMask = ospfNetMask;
        return this;
    }

    /**
     * @return the hello interval
     */
    public short getOspfHelloInterval() {
        return this.ospfHelloInterval;
    }

    /**
     * @param ospfHelloInterval
     *            the hello interval to set
     * @return this
     */
    public OSPF setOspfHelloInterval(final byte ospfHelloInterval) {
        this.ospfHelloInterval = ospfHelloInterval;
        return this;
    }

    /**
     * @return the sequence number
     */
    public short getOspfSequenceNum() {
        return this.ospfSequenceNum;
    }

    /**
     * @param ospfSequenceNum
     *            the sequence number to set
     * @return this
     */
    public OSPF setOspfSequenceNum(final byte ospfSequenceNum) {
        this.ospfSequenceNum = ospfSequenceNum;
        return this;
    }

    /**
     * @return the TTL
     */
    public short getOspfTtl() {
        return this.ospfTtl;
    }

    /**
     * @param ospfTtl
     *            the TTL to set
     * @return this
     */
    public OSPF setOspfTtl(final byte ospfTtl) {
        this.ospfTtl = ospfTtl;
        return this;
    }

    /**
     * @return the # of advertisements
     */
    public int getOspfNumOfAdvertisments() {
        return this.ospfNumOfAdvertisments;
    }

    /**
     * @param ospfNumOfAdvertisments
     *            the # of advertisements to set
     * @return this
     */
    public OSPF setOspfNumOfAdvertisments(final int ospfNumOfAdvertisments) {
        this.ospfNumOfAdvertisments = ospfNumOfAdvertisments;
        return this;
    }

    /**
     * Serializes the packet. Will compute and set the following fields if they
     * are set to specific values at the time serialize is called: -checksum : 0
     * -length : 0
     */
    @Override
    public byte[] serialize() {

        switch (this.ospfType) {
        case OSPF_HELLO:
            this.ospfPktLen = (short) (OSPF_HEADER_LENGTH + 2 * 4);

            break;

        case OSPF_DBD:

            break;

        case OSPF_LSR:

            break;

        case OSPF_LSU:
            this.ospfPktLen = (short) (OSPF_HEADER_LENGTH + 3 * 4 * this.ospfNumOfAdvertisments);

            break;

        case OSPF_LSA:

            break;

        default:
            log.debug("Unknown Type field in the packet");

        }

        final byte[] ospfData = new byte[this.ospfPktLen];
        final ByteBuffer bb = ByteBuffer.wrap(ospfData);

        bb.put(this.ospfVersionNum);
        bb.put(this.ospfType);
        bb.putShort(this.ospfPktLen);
        bb.put((byte) this.ospfRouterId);
        bb.put((byte) this.ospfAreaId);

        /* By default, the checksum is zero and if so, we will fix that once we warp the payload */
        bb.putShort(this.ospfChecksum);

        bb.putShort(this.ospfAuthType);
        bb.put((byte) this.ospfAuth);

        switch (this.ospfType) {
        case OSPF_HELLO:
            bb.put((byte) this.ospfNetMask);
            bb.putShort(this.ospfHelloInterval);

            break;

        case OSPF_DBD:

            break;

        case OSPF_LSR:

            break;

        case OSPF_LSU:
            /* Yes we will need to update the sequence number and TTL
             * as well as our advertisement content, but let's just
             * assume those have been done already and we can safely
             * build our ospfData.
             */
            bb.putShort(this.ospfSequenceNum);
            bb.putShort(this.ospfTtl);
            bb.put((byte) this.ospfNumOfAdvertisments);

            for (int i = 0; i < this.ospfNumOfAdvertisments; i++) {
                LSAdvertisment lsa = ospfAdvertismentList.get(i);
                bb.put((byte) lsa.getSubnet());
                bb.put((byte) lsa.getMask());
                bb.put((byte) lsa.getRouterId());
            }

            break;

        case OSPF_LSA:

            break;

        default:
            log.debug("Unknown Type field in the packet");

        }

        if (this.parent != null && this.parent instanceof IPv4) {
            ((IPv4) this.parent).setProtocol(IPv4.PROTOCOL_OSPF);
        }

        /* almost done, but don't forget we need update the checksum
         * the checksum should be put at 12-16th bytes of the header
         */
        bb.putShort(12, calculateOspfChecksum(ospfData, ospfPktLen));

        return ospfData;
    }

    /**
     * Deserializes this packet layer and all possible payloads.
     *
     * NOTE: This method has been deprecated and will be removed in a future
     * release. It is now recommended to use the Deserializer function provided
     * by the deserialize() method on each packet to deserialize them. The
     * Deserializer functions are robust to malformed input.
     *
     * @param data bytes to deserialize
     * @param offset
     *            offset to start deserializing from
     * @param length
     *            length of the data to deserialize
     * @return the deserialized data
     * @deprecated in Cardinal Release
     */
    @Override
    public IPacket deserialize(final byte[] data, final int offset,
            final int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

        /* Extract 24-byte OSPF header */
        this.ospfVersionNum = bb.get();
        this.ospfType = bb.get();
        this.ospfPktLen = bb.getShort();
        this.ospfRouterId = bb.getInt();
        this.ospfAreaId = bb.getInt();
        this.ospfChecksum = bb.get();
        this.ospfAuthType = bb.get();
        this.ospfAuth = bb.getLong();

        /* Extract OSPF payload */
        switch (this.ospfType) {
        case OSPF_HELLO:
            this.ospfNetMask = bb.getInt();
            this.ospfHelloInterval = bb.getShort();

            break;

        case OSPF_DBD:

            break;

        case OSPF_LSR:

            break;

        case OSPF_LSU:
            this.ospfSequenceNum = bb.getShort();
            this.ospfTtl = bb.getShort();
            this.ospfNumOfAdvertisments = bb.getInt();

            for (int i = 0; i < this.ospfNumOfAdvertisments; i++) {
                int subnet = bb.getInt();
                int mask = bb.getInt();
                int routerId = bb.getInt();
                this.ospfAdvertismentList.add(new LSAdvertisment(subnet, mask, routerId));
            }

            break;

        case OSPF_LSA:

            break;

        default:
            log.debug("Unknown Type field in the packet");

        }

        return this;
    }

    /**
     * Deserializer function for OSPF packets.
     *
     * @return deserializer function
     */
    public static Deserializer<OSPF> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, OSPF_HEADER_LENGTH);

            OSPF ospf = new OSPF();

            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            /* Extract 24-byte OSPF header
             *
             * OSPF header format
             * ===================================================================
             *
             *  0                   1                   2                   3
             *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
             *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
             *  |   Version #   |     Type      |         Packet length         |
             *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
             *  |                          Router ID                            |
             *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
             *  |                           Area ID                             |
             *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
             *  |           Checksum            |             AuType            |
             *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
             *  |                       Authentication                          |
             *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
             *  |                       Authentication                          |
             *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
             *
             * ===================================================================
             */
            ospf.ospfVersionNum = bb.get();
            ospf.ospfType = bb.get();
            ospf.ospfPktLen = bb.getShort();
            ospf.ospfRouterId = bb.getInt();
            ospf.ospfAreaId = bb.getInt();
            ospf.ospfChecksum = bb.getShort();
            ospf.ospfAuthType = bb.getShort();
            ospf.ospfAuth = bb.getLong();

            /* Extract OSPF payload */
            switch (ospf.ospfType) {
            case OSPF_HELLO:
                /* The Hello packet
                 *
                 * Hello packets are PWOSPF packet type 1. These packets are sent
                 * periodically on all interfaces in order to establish and maintain
                 * neighbor relationships. In addition, Hellos broadcast enabling
                 * dynamic discovery of neighboring routers.
                 *
                 * HELLO Packet format
                 * ===================================================================
                 *
                 *  0                   1                   2                   3
                 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                 *  |                        Network Mask                           |
                 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                 *  |         HelloInt              |           padding             |
                 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                 *
                 * ===================================================================
                 */
                ospf.ospfNetMask = bb.getInt();
                ospf.ospfHelloInterval = bb.getShort();

                break;

            case OSPF_DBD:

                break;

            case OSPF_LSR:

                break;

            case OSPF_LSU:
                /* The Link State Update packet
                 *
                 * LSU packets implement the flooding of link states and  are used to build and
                 * maintain the network topology database at each router.  Each link state
                 * update packet carries a collection of link state advertisements on hop
                 * further from its origin.  Several link state advertisements may be included
                 * in a single packet.
                 *
                 * ===================================================================
                 *
                 *  0                   1                   2                   3
                 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                 *  |     Sequence                |          TTL                    |
                 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                 *  |                      # advertisements                         |
                 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                 *  |                                                               |
                 *  +-                                                            +-+
                 *  |                  Link state advertisements                    |
                 *  +-                                                            +-+
                 *  |                              ...                              |
                 *
                 * ===================================================================
                 */
                ospf.ospfSequenceNum = bb.getShort();
                ospf.ospfTtl = bb.getShort();
                ospf.ospfNumOfAdvertisments = bb.getInt();

                /*
                 * Each link state update packet should contain 1 or more link state
                 * advertisements.  The advertisements are the reachable routes
                 * directly connected to the advertising router. Routes are in the
                 * form of the subnet, netmask and router neighbor for the attached
                 * link.
                 *
                 * ===================================================================
                 *
                 *  0                   1                   2                   3
                 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
                 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                 *  |                           Subnet                              |
                 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                 *  |                           Mask                                |
                 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                 *  |                         Router ID                             |
                 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                 *
                 * ===================================================================
                 */
                for (int i = 0; i < ospf.ospfNumOfAdvertisments; i++) {
                    int subnet = bb.getInt();
                    int mask = bb.getInt();
                    int routerId = bb.getInt();
                    ospf.ospfAdvertismentList.add(new LSAdvertisment(subnet, mask, routerId));
                }

                break;

            case OSPF_LSA:

                break;

            default:
                log.debug("Unknown Type field in the packet");

            }

            return ospf;
        };
    }
}
