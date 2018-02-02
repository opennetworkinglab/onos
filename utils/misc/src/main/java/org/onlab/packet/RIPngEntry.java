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

package org.onlab.packet;

import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default DHCP option.
 */
public class RIPngEntry extends BasePacket {
    public static final int OPT_CODE_LEN = 1;
    public static final int ENTRY_LEN = 20;
    public static final byte INFINITY_METRIC = 16;
    public static final int NEXTHOP_METRIC =  255; // actually it is 0xFF

    private final Logger log = getLogger(getClass());
    protected byte[] prefix; // 16 bytes
    protected short routeTag;
    protected int prefixLen;
    protected int metric;

    @Override
    public byte[] serialize() {
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocate(ENTRY_LEN);
        byteBuffer.put(prefix);
        byteBuffer.putShort(routeTag);
        byteBuffer.put((byte) prefixLen);
        byteBuffer.put((byte) metric);
        return byteBuffer.array();
    }

    /**
     * Deserializer function for RIPng entry.
     *
     * @return deserializer function
     */
    public static Deserializer<RIPngEntry> deserializer() {
        return (data, offset, length) -> {
            RIPngEntry ripngEntry = new RIPngEntry();

            checkNotNull(data);

            if (offset < 0 || length < 0 ||
                length > data.length || offset >= data.length ||
                offset + length > data.length) {
               throw new DeserializationException("Illegal offset or length");
            }
            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            if (bb.remaining() < ENTRY_LEN) {
               throw new DeserializationException(
                          "Buffer underflow while reading RIPng entry");
            }
            ripngEntry.prefix = new byte[IpAddress.INET6_BYTE_LENGTH];
            bb.get(ripngEntry.prefix);
            ripngEntry.routeTag = bb.getShort();
            ripngEntry.prefixLen = 0xFF & bb.get();
            ripngEntry.metric = 0xFF & bb.get();
            return ripngEntry;
        };
    }
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), metric, prefixLen, Arrays.hashCode(prefix), routeTag);
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
        if (!(obj instanceof RIPngEntry)) {
            return false;
        }
        final RIPngEntry that = (RIPngEntry) obj;

        return super.equals(that) &&
                Objects.equals(metric, that.metric) &&
                Objects.equals(prefixLen, that.prefixLen) &&
                Arrays.equals(prefix, that.prefix) &&
                Objects.equals(routeTag, that.routeTag);
    }
    /**
     * @return the IPv6 prefix
     */
    public byte[] getPrefix() {
        return this.prefix;
    }

    /**
     * @param prefix the IPv6 prefix to set
     * @return this
     */
    public RIPngEntry setPrefix(final byte[] prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * @return the route tag
     */
    public short getRouteTag() {
        return this.routeTag;
    }

    /**
     * @param routetag the route tag to set
     * @return this
     */
    public RIPngEntry setRouteTag(final short routetag) {
        this.routeTag = routetag;
        return this;
    }

    /**
     * @return the prefix length
     */
    public int getPrefixLen() {
        return this.prefixLen;
    }

    /**
     * @param prefixlen the prefix length to set
     * @return this
     */
    public RIPngEntry setPrefixLen(final int prefixlen) {
        this.prefixLen = prefixlen;
        return this;
    }

    /**
     * @return the metric
     */
    public int getMetric() {
        return this.metric;
    }

    /**
     * @param metric the route metric to set
     * @return this
     */
    public RIPngEntry setMetric(final int metric) {
        this.metric = metric;
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "RIPngEntry [prefix=" + Arrays.toString(this.prefix) + ", route tag=" + this.routeTag
                + ", prefix length=" + this.prefixLen
                + ", metric = " + this.metric + "]";
    }
}
