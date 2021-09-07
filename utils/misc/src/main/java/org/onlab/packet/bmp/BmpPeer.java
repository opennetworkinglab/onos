/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onlab.packet.bmp;

import com.google.common.base.MoreObjects;
import org.onlab.packet.BasePacket;
import org.onlab.packet.Deserializer;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import static org.onlab.packet.PacketUtils.checkInput;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The per-peer header follows the common header for most BMP messages.
 * The rest of the data in a BMP message is dependent on the Message
 * Type field in the common header.
 * <p>
 * Peer Type (1 byte): Identifies the type of peer.  Currently, three
 * types of peers are identified:
 * <p>
 * *  Peer Type = 0: Global Instance Peer
 * *  Peer Type = 1: RD Instance Peer
 * *  Peer Type = 2: Local Instance Peer
 * <p>
 * o  Peer Flags (1 byte): These flags provide more information about
 * the peer.  The flags are defined as follows:
 * <p>
 * 0 1 2 3 4 5 6 7
 * +-+-+-+-+-+-+-+-+
 * |V|L|A| Reserved|
 * +-+-+-+-+-+-+-+-+
 * <p>
 * *  The V flag indicates that the Peer address is an IPv6 address.
 * For IPv4 peers, this is set to 0.
 * <p>
 * The L flag, if set to 1, indicates that the message reflects
 * the post-policy Adj-RIB-In (i.e., its path attributes reflect
 * the application of inbound policy).  It is set to 0 if the
 * message reflects the pre-policy Adj-RIB-In.  Locally sourced
 * routes also carry an L flag of 1.  See Section 5 for further
 * detail.  This flag has no significance when used with route
 * mirroring messages.
 * <p>
 * *  The A flag, if set to 1, indicates that the message is
 * formatted using the legacy 2-byte AS_PATH format.  If set to 0,
 * the message is formatted using the 4-byte AS_PATH format
 * [RFC6793].  A BMP speaker MAY choose to propagate the AS_PATH
 * information as received from its peer, or it MAY choose to
 * reformat all AS_PATH information into a 4-byte format
 * regardless of how it was received from the peer.  In the latter
 * case, AS4_PATH or AS4_AGGREGATOR path attributes SHOULD NOT be
 * sent in the BMP UPDATE message.  This flag has no significance
 * when used with route mirroring messages.
 * <p>
 * The remaining bits are reserved for future use.  They MUST be
 * transmitted as 0 and their values MUST be ignored on receipt.
 * <p>
 * Peer Distinguisher (8 bytes): Routers today can have multiple
 * instances (example: Layer 3 Virtual Private Networks (L3VPNs)
 * [RFC4364]).  This field is present to distinguish peers that
 * belong to one address domain from the other.
 * <p>
 * If the peer is a "Global Instance Peer", this field is zero-
 * filled.  If the peer is a "RD Instance Peer", it is set to the
 * route distinguisher of the particular instance the peer belongs
 * to.  If the peer is a "Local Instance Peer", it is set to a
 * unique, locally defined value.  In all cases, the effect is that
 * the combination of the Peer Type and Peer Distinguisher is
 * sufficient to disambiguate peers for which other identifying
 * information might overlap.
 * <p>
 * Peer Address: The remote IP address associated with the TCP
 * session over which the encapsulated PDU was received.  It is 4
 * bytes long if an IPv4 address is carried in this field (with the
 * 12 most significant bytes zero-filled) and 16 bytes long if an
 * IPv6 address is carried in this field.
 * <p>
 * Peer AS: The Autonomous System number of the peer from which the
 * encapsulated PDU was received.  If a 16-bit AS number is stored in
 * this field [RFC6793], it should be padded with zeroes in the 16
 * most significant bits.
 * <p>
 * Timestamp: The time when the encapsulated routes were received
 * (one may also think of this as the time when they were installed
 * in the Adj-RIB-In), expressed in seconds and microseconds since
 * midnight (zero hour), January 1, 1970 (UTC).  If zero, the time is
 * unavailable.  Precision of the timestamp is implementation-
 * dependent.
 * <p>
 * 4.3.  Initiation Message
 * <p>
 * The initiation message provides a means for the monitored router to
 * inform the monitoring station of its vendor, software version, and so
 * on.  An initiation message MUST be sent as the first message after
 * the TCP session comes up.  An initiation message MAY be sent at any
 * point thereafter, if warranted by a change on the monitored router.
 * <p>
 * The initiation message consists of the common BMP header followed by
 * two or more Information TLVs containing information
 * about the monitored router.  The sysDescr and sysName Information
 * TLVs MUST be sent, any others are optional.  The string TLV MAY be
 * included multiple times.
 */
public class BmpPeer extends BasePacket {

    /*
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |   Peer Type   |  Peer Flags   |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |         Peer Distinguisher (present based on peer type)       |
     |                                                               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                 Peer Address (16 bytes)                       |
     ~                                                               ~
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                           Peer AS                             |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                         Peer BGP ID                           |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                    Timestamp (seconds)                        |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                  Timestamp (microseconds)                     |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    public static final short PEER_HEADER_MINIMUM_LENGTH = 42;

    public static final short PEER_DISTINGUISHER = 8;
    public static final short IPV4_ADDRSZ = 4;
    public static final short IPV6_ADDRSZ = 16;

    protected byte type;

    protected byte flags;

    protected byte[] peerDistinguisher;

    protected InetAddress peerAddress;

    protected int peerAs;

    protected int peerBgpId;

    protected int seconds;

    protected int microseconds;

    private static Logger log = getLogger(BmpPeer.class);


    /**
     * Returns Peer Type.
     *
     * @return the peer type
     */
    public byte getType() {
        return type;
    }

    /**
     * Returns Peer Flag.
     *
     * @return the peer flag
     */
    public byte getFlag() {
        return flags;
    }

    /**
     * Returns Peer Distinguisher.
     *
     * @return the peer distingusiher
     */
    public byte[] getPeerDistinguisher() {
        return peerDistinguisher;
    }

    /**
     * Returns Peer IP Address.
     *
     * @return the peer ip address
     */
    public InetAddress getIntAddress() {
        return peerAddress;
    }

    /**
     * Returns Peer Autonomous System number.
     *
     * @return the peer AS number
     */
    public int getPeerAs() {
        return peerAs;
    }

    /**
     * Returns Peer Bgp Id.
     *
     * @return the bgp id
     */
    public int getPeerBgpId() {
        return peerBgpId;
    }

    /**
     * Returns timestamp in sec.
     *
     * @return the timestamp in sec
     */
    public int getSeconds() {
        return seconds;
    }

    /**
     * Returns timestamp in micro second.
     *
     * @return the timestamp in micro second
     */
    public int getMicroseconds() {
        return microseconds;
    }


    @Override
    public byte[] serialize() {
        final byte[] data = new byte[PEER_HEADER_MINIMUM_LENGTH];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(this.type);
        bb.put(this.flags);
        bb.put(this.peerDistinguisher);
        bb.put(this.peerAddress.getAddress());
        bb.putInt(this.peerAs);
        bb.putInt(this.peerBgpId);
        bb.putInt(this.seconds);
        bb.putInt(this.microseconds);

        return data;
    }

    public static Deserializer<BmpPeer> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, PEER_HEADER_MINIMUM_LENGTH);

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            BmpPeer bmpPeer = new BmpPeer();

            bmpPeer.type = bb.get();
            bmpPeer.flags = bb.get();
            bmpPeer.peerDistinguisher = new byte[PEER_DISTINGUISHER];
            bb.get(bmpPeer.peerDistinguisher, 0, PEER_DISTINGUISHER);

            if ((bmpPeer.flags & 0x80) != 0x00) {
                bmpPeer.peerAddress = toInetAddress(IPV6_ADDRSZ, bb);
            } else {
                bb.position(bb.position() + (IPV6_ADDRSZ - IPV4_ADDRSZ));
                bmpPeer.peerAddress = toInetAddress(IPV4_ADDRSZ, bb);
            }

            bmpPeer.peerAs = bb.getInt();
            bmpPeer.peerBgpId = bb.getInt();
            bmpPeer.seconds = bb.getInt();
            bmpPeer.microseconds = bb.getInt();

            return bmpPeer;
        };
    }

    private static InetAddress toInetAddress(int length, ByteBuffer bb) {
        byte[] address = new byte[length];
        bb.get(address, 0, length);
        InetAddress ipAddress = null;
        try {
            ipAddress = InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            log.error("InetAddress conversion failed");
        }

        return ipAddress;
    }

    @Override
    public String toString() {

        return MoreObjects.toStringHelper(getClass())
                .add("flags", flags)
                .add("type", type)
                .add("peerDistinguisher", Arrays.toString(peerDistinguisher))
                .add("peerAddress", peerAddress.getHostAddress())
                .add("peerAs", peerAs)
                .add("peerBgpId", peerBgpId)
                .add("seconds", seconds)
                .add("microseconds", microseconds)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), flags, type, peerAddress,
                Arrays.hashCode(peerDistinguisher), peerAs, peerBgpId, seconds, microseconds);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof BmpPeer)) {
            return false;
        }
        final BmpPeer other = (BmpPeer) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.flags != other.flags) {
            return false;
        }
        if (!Arrays.equals(this.peerDistinguisher, other.peerDistinguisher)) {
            return false;
        }
        if (this.peerAddress != other.peerAddress) {
            return false;
        }
        if (this.peerAs != other.peerAs) {
            return false;
        }
        if (this.peerBgpId != other.peerBgpId) {
            return false;
        }
        if (this.seconds != other.seconds) {
            return false;
        }
        if (this.microseconds != other.microseconds) {
            return false;
        }
        return true;
    }

}
