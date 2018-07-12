/*
 *
 *  * Copyright 2015 AT&T Foundry
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.onlab.packet;

import java.nio.ByteBuffer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkHeaderLength;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * EAPOL (Extensible Authentication Protocol over LAN) header.
 */
public class EAPOL extends BasePacket {

    // private byte version = 0x01;
    private byte version = 0x03;
    private byte eapolType;
    private short packetLength;

    private static final int HEADER_LENGTH = 4;

    // EAPOL Packet Type
    public static final byte EAPOL_PACKET = 0x0;
    public static final byte EAPOL_START = 0x1;
    public static final byte EAPOL_LOGOFF = 0x2;
    public static final byte EAPOL_KEY = 0x3;
    public static final byte EAPOL_ASF = 0x4;
    public static final byte EAPOL_MKA = 0X5;

    public static final MacAddress PAE_GROUP_ADDR = MacAddress.valueOf(new byte[]{
            (byte) 0x01, (byte) 0x80, (byte) 0xc2, (byte) 0x00, (byte) 0x00, (byte) 0x03
    });

    /**
     * Gets the version.
     *
     * @return version
     */
    public byte getVersion() {
        return this.version;
    }

    /**
     * Sets the version.
     *
     * @param version EAPOL version
     * @return this
     */
    public EAPOL setVersion(final byte version) {
        this.version = version;
        return this;
    }

    /**
     * Gets the type.
     *
     * @return EAPOL type
     */
    public byte getEapolType() {
        return this.eapolType;
    }

    /**
     * Sets the EAPOL type.
     *
     * @param eapolType EAPOL type
     * @return this
     */
    public EAPOL setEapolType(final byte eapolType) {
        this.eapolType = eapolType;
        return this;
    }

    /**
     * Gets the packet length.
     *
     * @return packet length
     */
    public short getPacketLength() {
        return this.packetLength;
    }

    /**
     * Sets the packet length.
     *
     * @param packetLen packet length
     * @return this
     */
    public EAPOL setPacketLength(final short packetLen) {
        this.packetLength = packetLen;
        return this;
    }

    /**
     * Serializes the packet, based on the code/type using the payload
     * to compute its length.
     *
     * @return this
     */
    @Override
    public byte[] serialize() {
        byte[] payloadData = null;

        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }

        // prepare the buffer to hold the version (1), packet type (1),
        // packet length (2) and the eap payload.
        // if there is no payload, packet length is 0
        byte[] data = new byte[4 + this.packetLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.version);
        bb.put(this.eapolType);
        bb.putShort(this.packetLength);

        // put the EAP payload
        if (payloadData != null) {
            bb.put(payloadData);
        }

        return data;
    }

    @Override
    public int hashCode() {
        final int prime = 3889;
        int result = super.hashCode();
        result = prime * result + this.version;
        result = prime * result + this.eapolType;
        result = prime * result + this.packetLength;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EAPOL)) {
            return false;
        }
        EAPOL that = (EAPOL) o;

        if (this.version != that.version) {
            return false;
        }
        if (this.eapolType != that.eapolType) {
            return false;
        }
        if (this.packetLength != that.packetLength) {
            return false;
        }
        return true;
    }

    /**
     * Deserializer for EAPOL packets.
     *
     * @return deserializer
     */
    public static Deserializer<EAPOL> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            EAPOL eapol = new EAPOL();
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            eapol.setVersion(bb.get());
            eapol.setEapolType(bb.get());
            eapol.setPacketLength(bb.getShort());

            if (eapol.packetLength > 0) {
                checkHeaderLength(length, HEADER_LENGTH + eapol.packetLength);
                if (eapol.getEapolType() == EAPOL_MKA) {
                    eapol.payload = EAPOLMkpdu.deserializer().deserialize(data,
                            bb.position(), bb.limit() - bb.position());
                } else {
                    // deserialize the EAP Payload
                    eapol.payload = EAP.deserializer().deserialize(data,
                            bb.position(), bb.limit() - bb.position());
                }
                eapol.payload.setParent(eapol);
            }
            return eapol;
        };
    }


    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("version", Byte.toString(version))
                .add("eapolType", Byte.toString(eapolType))
                .add("packetLength", Short.toString(packetLength))
                .toString();
    }
}

