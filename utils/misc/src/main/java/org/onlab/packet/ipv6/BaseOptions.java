/*
 * Copyright 2014-2015 Open Networking Laboratory
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

package org.onlab.packet.ipv6;

import org.onlab.packet.BasePacket;
import org.onlab.packet.Data;
import org.onlab.packet.IPacket;
import org.onlab.packet.IPv6;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Base class for hop-by-hop options and destination options.
 */
public class BaseOptions extends BasePacket implements IExtensionHeader {
    public static final byte FIXED_HEADER_LENGTH = 2; // bytes
    public static final byte FIXED_OPTIONS_LENGTH = 6; // bytes
    public static final byte LENGTH_UNIT = 8; // bytes per unit

    protected byte nextHeader;
    protected byte headerExtLength;
    protected byte[] options;
    protected byte type;

    @Override
    public byte getNextHeader() {
        return this.nextHeader;
    }

    @Override
    public BaseOptions setNextHeader(final byte nextHeader) {
        this.nextHeader = nextHeader;
        return this;
    }

    /**
     * Gets the extension length of this header.
     *
     * @return header length
     */
    public byte getHeaderExtLength() {
        return this.headerExtLength;
    }

    /**
     * Sets the extension length of this header.
     *
     * @param headerExtLength the header length to set
     * @return this
     */
    public BaseOptions setHeaderExtLength(final byte headerExtLength) {
        this.headerExtLength = headerExtLength;
        return this;
    }

    /**
     * Gets the options.
     *
     * @return the options
     */
    public byte[] getOptions() {
        return this.options;
    }

    /**
     * Sets the options.
     *
     * @param options the options to set
     * @return this
     */
    public BaseOptions setOptions(final byte[] options) {
        this.options =
                Arrays.copyOfRange(options, 0, options.length);
        return this;
    }

    /**
     * Gets the type of this option.
     *
     * @return the type
     */
    protected byte getType() {
        return this.type;
    }

    /**
     * Sets the type of this option.
     * Must be either IPv6.PROTOCOL_HOPOPT or IPv6.PROTOCOL_DSTOPT
     *
     * @param type the type to set
     * @return this
     */
    protected BaseOptions setType(final byte type) {
        this.type = type;
        return this;
    }

    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }

        int headerLength = FIXED_HEADER_LENGTH + options.length;
        int payloadLength = 0;
        if (payloadData != null) {
            payloadLength = payloadData.length;
        }

        final byte[] data = new byte[headerLength + payloadLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(this.nextHeader);
        bb.put(this.headerExtLength);
        bb.put(this.options, 0, options.length);

        if (payloadData != null) {
            bb.put(payloadData);
        }

        if (this.parent != null && this.parent instanceof IExtensionHeader) {
            ((IExtensionHeader) this.parent).setNextHeader(this.type);
        }
        return data;
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        this.nextHeader = bb.get();
        this.headerExtLength = bb.get();
        int optionLength =
                FIXED_OPTIONS_LENGTH + LENGTH_UNIT * this.headerExtLength;
        this.options = new byte[optionLength];
        bb.get(this.options, 0, optionLength);

        IPacket payload;
        if (IPv6.PROTOCOL_CLASS_MAP.containsKey(this.nextHeader)) {
            final Class<? extends IPacket> clazz = IPv6.PROTOCOL_CLASS_MAP
                    .get(this.nextHeader);
            try {
                payload = clazz.newInstance();
            } catch (final Exception e) {
                throw new RuntimeException(
                        "Error parsing payload for BaseOptions packet", e);
            }
        } else {
            payload = new Data();
        }
        this.payload = payload.deserialize(data, bb.position(),
                bb.limit() - bb.position());
        this.payload.setParent(this);

        return this;
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
        result = prime * result + this.nextHeader;
        result = prime * result + this.headerExtLength;
        for (byte b : this.options) {
            result = prime * result + b;
        }
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
        if (!(obj instanceof BaseOptions)) {
            return false;
        }
        final BaseOptions other = (BaseOptions) obj;
        if (this.nextHeader != other.nextHeader) {
            return false;
        }
        if (this.headerExtLength != other.headerExtLength) {
            return false;
        }
        if (!Arrays.equals(this.options, other.options)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }
}
