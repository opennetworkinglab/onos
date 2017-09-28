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
import java.util.Arrays;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkHeaderLength;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * EAP (Extensible Authentication Protocol) packet.
 */
public class EAP extends BasePacket {
    private static final int HEADER_LENGTH = 4;

    public static final short MIN_LEN = 0x4;
    public static final short EAP_HDR_LEN_REQ_RESP = 5;
    public static final short EAP_HDR_LEN_SUC_FAIL = 4;

    // EAP Code
    public static final byte REQUEST  = 0x1;
    public static final byte RESPONSE = 0x2;
    public static final byte SUCCESS  = 0x3;
    public static final byte FAILURE  = 0x4;

    // EAP Attribute Type
    public static final byte ATTR_IDENTITY = 0x1;
    public static final byte ATTR_NOTIFICATION = 0x2;
    public static final byte ATTR_NAK = 0x3;
    public static final byte ATTR_MD5 = 0x4;
    public static final byte ATTR_OTP = 0x5;
    public static final byte ATTR_GTC = 0x6;
    public static final byte ATTR_TLS = 0xd;

    protected byte code;
    protected byte identifier;
    protected short length;
    protected byte type;
    protected byte[] data;

    /**
     * Gets the EAP code.
     *
     * @return EAP code
     */
    public byte getCode() {
        return this.code;
    }


    /**
     * Sets the EAP code.
     *
     * @param code EAP code
     * @return this
     */
    public EAP setCode(final byte code) {
        this.code = code;
        return this;
    }

    /**
     * Gets the EAP identifier.
     *
     * @return EAP identifier
     */
    public byte getIdentifier() {
        return this.identifier;
    }

    /**
     * Sets the EAP identifier.
     *
     * @param identifier EAP identifier
     * @return this
     */
    public EAP setIdentifier(final byte identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Gets the get packet length.
     *
     * @return packet length
     */
    public short getLength() {
        return this.length;
    }

    /**
     * Sets the packet length.
     *
     * @param length packet length
     * @return this
     */
    public EAP setLength(final short length) {
        this.length = length;
        return this;
    }

    /**
     * Gets the data type.
     *
     * @return data type
     */
    public byte getDataType() {
        return this.type;
    }

    /**
     * Sets the data type.
     *
     * @param type data type
     * @return this
     */
    public EAP setDataType(final byte type) {
        this.type = type;
        return this;
    }

    /**
     * Gets the EAP data.
     *
     * @return EAP data
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * Sets the EAP data.
     *
     * @param data EAP data to be set
     * @return this
     */
    public EAP setData(final byte[] data) {
        this.data = data;
        return this;
    }

    /**
     * Default EAP constructor that sets the EAP code to 0.
     */
    public EAP() {
        this.code = 0;
    }

    /**
     * EAP constructor that initially sets all fields.
     *
     * @param code EAP code
     * @param identifier EAP identifier
     * @param type packet type
     * @param data EAP data
     */
    public EAP(byte code, byte identifier, byte type, byte[] data) {
        this.code = code;
        this.identifier = identifier;
        if (this.code == REQUEST || this.code == RESPONSE) {
            this.length = (short) (5 + (data == null ? 0 : data.length));
            this.type = type;
        } else {
            this.length = (short) (4 + (data == null ? 0 : data.length));
        }
        this.data = data;
    }

    /**
     * Deserializer for EAP packets.
     *
     * @return deserializer
     */
    public static Deserializer<EAP> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            EAP eap = new EAP();
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            eap.code = bb.get();
            eap.identifier = bb.get();
            eap.length = bb.getShort();

            checkHeaderLength(length, eap.length);

            int dataLength;
            if (eap.code == REQUEST || eap.code == RESPONSE) {
                eap.type = bb.get();
                dataLength = eap.length - 5;
            } else {
                dataLength = eap.length - 4;
            }

            eap.data = new byte[dataLength];
            bb.get(eap.data);
            return eap;
        };
    }

    @Override
    public byte[] serialize() {
        final byte[] data = new byte[this.length];

        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.code);
        bb.put(this.identifier);
        bb.putShort(this.length);
        if (this.code == REQUEST || this.code == RESPONSE) {
            bb.put(this.type);
        }
        if (this.data != null) {
            bb.put(this.data);
        }
        return data;
    }


    @Override
    public int hashCode() {
        final int prime = 3889;
        int result = super.hashCode();
        result = prime * result + this.code;
        result = prime * result + this.identifier;
        result = prime * result + this.length;
        result = prime * result + this.type;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EAP)) {
            return false;
        }
        EAP that = (EAP) o;

        if (this.code != that.code) {
            return false;
        }
        if (this.identifier != that.identifier) {
            return false;
        }
        if (this.length != that.length) {
            return false;
        }
        if (this.type != that.type) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("code", Byte.toString(code))
                .add("identifier", Byte.toString(identifier))
                .add("length", Short.toString(length))
                .add("type", Byte.toString(type))
                .add("data", Arrays.toString(data))
                .toString();
    }
}
