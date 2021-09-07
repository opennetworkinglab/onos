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

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.onlab.packet.PacketUtils.checkInput;

/**
 * The following common header appears in all BMP messages.  The rest of
 * the data in a BMP message is dependent on the Message Type field in
 * the common header.
 * <p>
 * Version (1 byte): Indicates the BMP version.  This is set to '3'
 * for all messages defined in this specification. ('1' and '2' were
 * used by draft versions of this document.)  Version 0 is reserved
 * and MUST NOT be sent.
 * <p>
 * Message Length (4 bytes): Length of the message in bytes
 * (including headers, data, and encapsulated messages, if any).
 * <p>
 * Message Type (1 byte): This identifies the type of the BMP
 * message.  A BMP implementation MUST ignore unrecognized message
 * types upon receipt.
 * <p>
 * Type = 0: Route Monitoring
 * Type = 1: Statistics Report
 * Type = 2: Peer Down Notification
 * Type = 3: Peer Up Notification
 * Type = 4: Initiation Message
 * Type = 5: Termination Message
 * Type = 6: Route Mirroring Message
 */
public class Bmp extends BasePacket {

    /*

      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+
     |    Version    |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                        Message Length                         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |   Msg. Type   |
     +---------------+

     */


    public static final short DEFAULT_HEADER_LENGTH = 6;
    public static final int DEFAULT_PACKET_MINIMUM_LENGTH = 4;

    protected byte version;
    protected byte type;
    protected int length;


    /**
     * Sets version field.
     *
     * @param version message version field
     */
    public void setVersion(byte version) {
        this.version = version;
    }

    /**
     * Sets message type.
     *
     * @param type message type
     */
    public void setType(byte type) {
        this.type = type;
    }

    /**
     * Sets message length.
     *
     * @param length message length
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Returns message length.
     *
     * @return message length
     */
    public int getLength() {
        return this.length;
    }

    /**
     * Returns message version.
     *
     * @return message version
     */
    public byte getVersion() {
        return this.version;
    }

    /**
     * Returns message type.
     *
     * @return message type
     */
    public byte getType() {
        return this.type;
    }


    @Override
    public byte[] serialize() {
        final byte[] data = new byte[DEFAULT_HEADER_LENGTH];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(this.version);
        bb.putInt(this.length);
        bb.put(this.type);

        return data;
    }


    /**
     * Deserializer function for Bmp Packets.
     *
     * @return deserializer function
     */
    public static Deserializer<Bmp> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, DEFAULT_HEADER_LENGTH);

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            Bmp bmp = new Bmp();

            bmp.version = bb.get();
            bmp.length = bb.getInt();
            bmp.type = bb.get();

            return bmp;
        };
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("version", version)
                .add("type", type)
                .add("length", length)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), version, type, length);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Bmp)) {
            return false;
        }
        final Bmp other = (Bmp) obj;
        if (this.version != other.version) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        if (this.length != other.length) {
            return false;
        }
        return true;
    }
}

