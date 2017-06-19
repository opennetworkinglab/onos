/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.onlab.packet.BasePacket;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default DHCP option.
 */
public class DhcpOption extends BasePacket {
    public static final int OPT_CODE_LEN = 1;
    public static final int DEFAULT_LEN = 2;
    private final Logger log = getLogger(getClass());
    protected byte code;
    protected byte length;
    protected byte[] data;

    @Override
    public byte[] serialize() {
        ByteBuffer byteBuffer;
        if (data != null) {
            byteBuffer = ByteBuffer.allocate(DEFAULT_LEN + data.length);
            byteBuffer.put(code);
            byteBuffer.put(length);
            byteBuffer.put(data);
        } else {
            byteBuffer = ByteBuffer.allocate(OPT_CODE_LEN);
            byteBuffer.put(code);
        }
        return byteBuffer.array();
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        try {
            return deserializer().deserialize(data, offset, length);
        } catch (DeserializationException e) {
            log.warn("Can't deserialize DhcpOption {}", e);
            return null;
        }
    }

    /**
     * Deserializer function for DHCP option.
     *
     * @return deserializer function
     */
    public static Deserializer<DhcpOption> deserializer() {
        return (data, offset, length) -> {
            DhcpOption dhcpOption = new DhcpOption();
            ByteBuffer byteBuffer = ByteBuffer.wrap(data, offset, length);
            dhcpOption.code = byteBuffer.get();
            if (byteBuffer.hasRemaining()) {
                dhcpOption.length = byteBuffer.get();
                dhcpOption.data = new byte[dhcpOption.length];
                byteBuffer.get(dhcpOption.data);
            } else {
                dhcpOption.length = 0;
                dhcpOption.data = null;
            }
            return dhcpOption;
        };
    }

    /**
     * @return the code
     */
    public byte getCode() {
        return this.code;
    }

    /**
     * @param code the code to set
     * @return this
     */
    public DhcpOption setCode(final byte code) {
        this.code = code;
        return this;
    }

    /**
     * @return the length
     */
    public byte getLength() {
        return this.length;
    }

    /**
     * @param length the length to set
     * @return this
     */
    public DhcpOption setLength(final byte length) {
        this.length = length;
        return this;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return this.data;
    }

    /**
     * @param data the data to set
     * @return this
     */
    public DhcpOption setData(final byte[] data) {
        this.data = data;
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(code, length, data);
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
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DhcpOption)) {
            return false;
        }
        final DhcpOption other = (DhcpOption) obj;
        return Objects.equals(this.code, other.code) &&
                Objects.equals(this.length, other.length) &&
                Arrays.equals(this.data, other.data);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "DhcpOption [code=" + this.code + ", length=" + this.length
                + ", data=" + Arrays.toString(this.data) + "]";
    }
}
