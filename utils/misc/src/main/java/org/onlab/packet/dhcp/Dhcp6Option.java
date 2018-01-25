/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onlab.packet.dhcp;

import com.google.common.base.MoreObjects.ToStringHelper;
import org.onlab.packet.BasePacket;
import org.onlab.packet.Data;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;

import java.nio.ByteBuffer;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of an DHCPv6 Option.
 * Base on RFC-3315.
 */
public class Dhcp6Option extends BasePacket {
    public static final int DEFAULT_LEN = 4;
    protected static final int UNSIGNED_SHORT_MASK = 0xffff;
    private short code;
    private short length;
    // XXX: use "payload" from BasePacket for option data.

    /**
     * Default constructor.
     */
    public Dhcp6Option() {
    }

    /**
     * Constructs a DHCPv6 option based on information from other DHCPv6 option.
     *
     * @param dhcp6Option other DHCPv6 option
     */
    public Dhcp6Option(Dhcp6Option dhcp6Option) {
        this.code = dhcp6Option.code;
        this.length = dhcp6Option.length;
        this.payload = dhcp6Option.payload;
        this.payload.setParent(this);
    }

    /**
     * Sets the code of this option.
     *
     * @param code the code to set
     */
    public void setCode(short code) {
        this.code = code;
    }

    /**
     * Sets the data of this option.
     *
     * @param data the data to set
     */
    public void setData(byte[] data) {
        try {
            this.payload = Data.deserializer().deserialize(data, 0, data.length);
        } catch (DeserializationException e) {
            throw new IllegalArgumentException("Invalid data");
        }
    }

    /**
     * Sets length of this option.
     *
     * @param length the length to set
     */
    public void setLength(short length) {
        this.length = length;
    }

    /**
     * Gets the code of this option.
     *
     * @return the code
     */
    public short getCode() {
        return code;
    }

    /**
     * Gets the length of this option.
     *
     * @return the length of this option
     */
    public short getLength() {
        return length;
    }

    /**
     * Gets the data of this option.
     *
     * @return the data of this option
     */
    public byte[] getData() {
        return payload.serialize();
    }

    /**
     * Gets deserializer of DHCPv6 option.
     *
     * @return the deserializer of DHCPv6 option
     */
    public static Deserializer<Dhcp6Option> deserializer() {
        return (data, offset, len) -> {
            Dhcp6Option dhcp6Option = new Dhcp6Option();
            if (len < DEFAULT_LEN) {
                throw new DeserializationException("DHCPv6 option code length" +
                                                           "should be at least 4 bytes");
            }
            ByteBuffer bb = ByteBuffer.wrap(data, offset, len);
            dhcp6Option.code = bb.getShort();
            dhcp6Option.length = bb.getShort();
            int optionLen = UNSIGNED_SHORT_MASK & dhcp6Option.length;
            byte[] optData = new byte[optionLen];
            bb.get(optData);
            dhcp6Option.setData(optData);
            return dhcp6Option;
        };
    }

    @Override
    public byte[] serialize() {
        ByteBuffer bb = ByteBuffer.allocate(DEFAULT_LEN + getLength());
        bb.putShort(getCode());
        bb.putShort(getLength());
        bb.put(payload.serialize());
        return bb.array();
    }


    protected ToStringHelper getToStringHelper() {
        return toStringHelper(Dhcp6Option.class)
                .add("code", code)
                .add("length", length);
    }

    @Override
    public String toString() {
        return getToStringHelper()
                .add("data", payload.toString())
                .toString();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(code, length);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final Dhcp6Option other = (Dhcp6Option) obj;
        return Objects.equals(this.code, other.code)
                && Objects.equals(this.length, other.length);
    }
}
