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



package org.onlab.packet;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.*;

/**
 * Implements TCP packet format.
 */

public class TCP extends BasePacket {

    private static final short TCP_HEADER_LENGTH = 20;

    protected int sourcePort;
    protected int destinationPort;
    protected int sequence;
    protected int acknowledge;
    protected byte dataOffset;
    protected short flags;
    protected short windowSize;
    protected short checksum;
    protected short urgentPointer;
    protected byte[] options;

    /**
     * Gets TCP source port.
     *
     * @return TCP source port
     */
    public int getSourcePort() {
        return this.sourcePort;
    }

    /**
     * Sets TCP source port.
     *
     * @param sourcePort the sourcePort to set (unsigned 16 bits integer)
     * @return this
     */
    public TCP setSourcePort(final int sourcePort) {
        this.sourcePort = sourcePort;
        return this;
    }

    /**
     * Gets TCP destination port.
     *
     * @return the destinationPort
     */
    public int getDestinationPort() {
        return this.destinationPort;
    }

    /**
     * Sets TCP destination port.
     *
     * @param destinationPort the destinationPort to set (unsigned 16 bits integer)
     * @return this
     */
    public TCP setDestinationPort(final int destinationPort) {
        this.destinationPort = destinationPort;
        return this;
    }

    /**
     * Gets checksum.
     *
     * @return the checksum
     */
    public short getChecksum() {
        return this.checksum;
    }

    /**
     * Sets checksum.
     *
     * @param checksum the checksum to set
     * @return this
     */
    public TCP setChecksum(final short checksum) {
        this.checksum = checksum;
        return this;
    }

    /**
     * Gets sequence number.
     *
     * @return the sequence number
     */
    public int getSequence() {
        return this.sequence;
    }

    /**
     * Sets sequence number.
     *
     * @param seq the sequence number to set
     * @return this
     */
    public TCP setSequence(final int seq) {
        this.sequence = seq;
        return this;
    }

    /**
     * Gets acknowledge number.
     *
     * @return the acknowledge number
     */
    public int getAcknowledge() {
        return this.acknowledge;
    }

    /**
     * Sets acknowledge number.
     *
     * @param ack the acknowledge number to set
     * @return this
     */
    public TCP setAcknowledge(final int ack) {
        this.acknowledge = ack;
        return this;
    }

    /**
     * Gets offset.
     *
     * @return the offset
     */
    public byte getDataOffset() {
        return this.dataOffset;
    }

    /**
     * Sets offset.
     *
     * @param offset the offset to set
     * @return this
     */
    public TCP setDataOffset(final byte offset) {
        this.dataOffset = offset;
        return this;
    }

    /**
     * Gets TCP flags.
     *
     * @return the TCP flags
     */
    public short getFlags() {
        return this.flags;
    }

    /**
     * Sets TCP flags.
     *
     * @param flags the TCP flags to set
     * @return this
     */
    public TCP setFlags(final short flags) {
        this.flags = flags;
        return this;
    }

    /**
     * Gets TCP window size.
     *
     * @return the TCP window size
     */
    public short getWindowSize() {
        return this.windowSize;
    }

    /**
     * Sets TCP window size.
     *
     * @param windowSize the TCP window size to set
     * @return this
     */
    public TCP setWindowSize(final short windowSize) {
        this.windowSize = windowSize;
        return this;
    }

    @Override
    public void resetChecksum() {
        this.checksum = 0;
        super.resetChecksum();
    }

    /**
     * Gets urgent pointer.
     *
     * @return the urgent pointer
     */
    public short getUrgentPointer() {
        return this.urgentPointer;
    }

    /**
     * Sets urgent pointer.
     *
     * @param urgentPointer the urgent pointer to set
     * @return this
     */
    public TCP setUrgentPointer(final short urgentPointer) {
        this.urgentPointer = urgentPointer;
        return this;
    }

    /**
     * Gets TCP options.
     *
     * @return the TCP options
     */
    public byte[] getOptions() {
        return this.options;
    }

    /**
     * Sets TCP options.
     *
     * @param options the options to set
     * @return this
     */
    public TCP setOptions(final byte[] options) {
        this.options = options;
        this.dataOffset = (byte) (20 + options.length + 3 >> 2);
        return this;
    }

    /**
     * Serializes the packet. Will compute and set the following fields if they
     * are set to specific values at the time serialize is called: -checksum : 0
     * -length : 0
     */
    @Override
    public byte[] serialize() {
        int length;
        if (this.dataOffset == 0) {
            this.dataOffset = 5; // default header length
        }
        length = this.dataOffset << 2;
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
            length += payloadData.length;
        }

        final byte[] data = new byte[length];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putShort((short) (this.sourcePort & 0xffff));
        bb.putShort((short) (this.destinationPort & 0xffff));
        bb.putInt(this.sequence);
        bb.putInt(this.acknowledge);
        bb.putShort((short) (this.flags | this.dataOffset << 12));
        bb.putShort(this.windowSize);
        bb.putShort(this.checksum);
        bb.putShort(this.urgentPointer);
        if (this.dataOffset > 5) {
            int padding;
            bb.put(this.options);
            padding = (this.dataOffset << 2) - 20 - this.options.length;
            for (int i = 0; i < padding; i++) {
                bb.put((byte) 0);
            }
        }
        if (payloadData != null) {
            bb.put(payloadData);
        }

        if (this.parent != null && this.parent instanceof IPv4) {
            ((IPv4) this.parent).setProtocol(IPv4.PROTOCOL_TCP);
        }

        // compute checksum if needed
        if (this.checksum == 0) {
            bb.rewind();
            int accumulation = 0;

            // compute pseudo header mac
            if (this.parent != null) {
                if (this.parent instanceof IPv4) {
                    final IPv4 ipv4 = (IPv4) this.parent;
                    accumulation += (ipv4.getSourceAddress() >> 16 & 0xffff)
                            + (ipv4.getSourceAddress() & 0xffff);
                    accumulation += (ipv4.getDestinationAddress() >> 16 & 0xffff)
                            + (ipv4.getDestinationAddress() & 0xffff);
                    accumulation += ipv4.getProtocol() & 0xff;
                    accumulation += length & 0xffff;
                } else if (this.parent instanceof IPv6) {
                    final IPv6 ipv6 = (IPv6) this.parent;
                    final int bbLength =
                            Ip6Address.BYTE_LENGTH * 2 // IPv6 src, dst
                                    + 2  // nextHeader (with padding)
                                    + 4; // length
                    final ByteBuffer bbChecksum = ByteBuffer.allocate(bbLength);
                    bbChecksum.put(ipv6.getSourceAddress());
                    bbChecksum.put(ipv6.getDestinationAddress());
                    bbChecksum.put((byte) 0); // padding
                    bbChecksum.put(ipv6.getNextHeader());
                    bbChecksum.putInt(length);
                    bbChecksum.rewind();
                    for (int i = 0; i < bbLength / 2; ++i) {
                        accumulation += 0xffff & bbChecksum.getShort();
                    }
                }
            }

            for (int i = 0; i < length / 2; ++i) {
                accumulation += 0xffff & bb.getShort();
            }
            // pad to an even number of shorts
            if (length % 2 > 0) {
                accumulation += (bb.get() & 0xff) << 8;
            }

            accumulation = (accumulation >> 16 & 0xffff)
                    + (accumulation & 0xffff);
            this.checksum = (short) (~accumulation & 0xffff);
            bb.putShort(16, this.checksum);
        }
        return data;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 5807;
        int result = super.hashCode();
        result = prime * result + this.checksum;
        result = prime * result + this.destinationPort;
        result = prime * result + this.sourcePort;
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
        if (!(obj instanceof TCP)) {
            return false;
        }
        final TCP other = (TCP) obj;
        // May want to compare fields based on the flags set
        return this.checksum == other.checksum
                && this.destinationPort == other.destinationPort
                && this.sourcePort == other.sourcePort
                && this.sequence == other.sequence
                && this.acknowledge == other.acknowledge
                && this.dataOffset == other.dataOffset
                && this.flags == other.flags
                && this.windowSize == other.windowSize
                && this.urgentPointer == other.urgentPointer
                && (this.dataOffset == 5 || Arrays.equals(this.options, other.options));
    }

    /**
     * Deserializer function for TCP packets.
     *
     * @return deserializer function
     */
    public static Deserializer<TCP> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, TCP_HEADER_LENGTH);

            TCP tcp = new TCP();

            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            tcp.sourcePort = (bb.getShort() & 0xffff);
            tcp.destinationPort = (bb.getShort() & 0xffff);
            tcp.sequence = bb.getInt();
            tcp.acknowledge = bb.getInt();
            tcp.flags = bb.getShort();
            tcp.dataOffset = (byte) (tcp.flags >> 12 & 0xf);
            tcp.flags = (short) (tcp.flags & 0x1ff);
            tcp.windowSize = bb.getShort();
            tcp.checksum = bb.getShort();
            tcp.urgentPointer = bb.getShort();
            if (tcp.dataOffset > 5) {
                int optLength = (tcp.dataOffset << 2) - 20;
                checkHeaderLength(length, TCP_HEADER_LENGTH + optLength);
                tcp.options = new byte[optLength];
                bb.get(tcp.options, 0, optLength);
            }

            tcp.payload = Data.deserializer()
                    .deserialize(data, bb.position(), bb.limit() - bb.position());
            tcp.payload.setParent(tcp);
            return tcp;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("sourcePort", Integer.toString(sourcePort))
                .add("destinationPort", Integer.toString(destinationPort))
                .add("sequence", Integer.toString(sequence))
                .add("acknowledge", Integer.toString(acknowledge))
                .add("dataOffset", Byte.toString(dataOffset))
                .add("flags", Short.toString(flags))
                .add("windowSize", Short.toString(windowSize))
                .add("checksum", Short.toString(checksum))
                .add("urgentPointer", Short.toString(urgentPointer))
                .add("options", Arrays.toString(options))
                .toString();
    }
}
