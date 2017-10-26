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
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

import static org.slf4j.LoggerFactory.getLogger;

/*
 * Entry for RIP version 2 - RFC 2453
 */
public class RIPV2Entry extends BasePacket {
    public static final int ENTRY_LEN = 20;
    public static final short AFI_IP = 2;
    public static final byte INFINITY_METRIC = 16;
    public static final byte NEXTHOP_METRIC =  -128; // actually it is 0xFF

    private final Logger log = getLogger(getClass());
    protected short addressFamilyId;
    protected short routeTag;
    protected Ip4Address ipAddress;
    protected Ip4Address subnetMask;
    protected Ip4Address nextHop;
    protected int metric;

    @Override
    public byte[] serialize() {
        ByteBuffer byteBuffer;
        byteBuffer = ByteBuffer.allocate(ENTRY_LEN);
        byteBuffer.putShort(addressFamilyId);
        byteBuffer.putShort(routeTag);
        byteBuffer.putInt(ipAddress.toInt());
        byteBuffer.putInt(subnetMask.toInt());
        byteBuffer.putInt(nextHop.toInt());
        byteBuffer.putInt(metric);
        return byteBuffer.array();
    }

    /**
     * Deserializer function for RIPv2 entry.
     *
     * @return deserializer function
     */
    public static Deserializer<RIPV2Entry> deserializer() {
        return (data, offset, length) -> {
            RIPV2Entry ripEntry = new RIPV2Entry();

            checkNotNull(data);

            if (offset < 0 || length < 0 ||
                length > data.length || offset >= data.length ||
                offset + length > data.length) {
               throw new DeserializationException("Illegal offset or length");
            }
            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            if (bb.remaining() < ENTRY_LEN) {
               throw new DeserializationException(
                          "Buffer underflow while reading RIP entry");
            }
            ripEntry.addressFamilyId = bb.getShort();
            // skip the authentication entry
            if (ripEntry.addressFamilyId == 0xffff) {
                return ripEntry;
            }
            ripEntry.routeTag = bb.getShort();
            ripEntry.ipAddress = Ip4Address.valueOf(bb.getInt());
            ripEntry.subnetMask = Ip4Address.valueOf(bb.getInt());
            ripEntry.nextHop = Ip4Address.valueOf(bb.getInt());
            ripEntry.metric = bb.getInt();
            return ripEntry;
        };
    }
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nextHop.toInt(), subnetMask.toInt(),
                            ipAddress.toInt(), addressFamilyId, metric, routeTag);
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
        if (!(obj instanceof RIPV2Entry)) {
            return false;
        }
        final RIPV2Entry that = (RIPV2Entry) obj;


        return super.equals(that) &&
                Objects.equals(routeTag, that.routeTag) &&
                Objects.equals(metric, that.metric) &&
                Objects.equals(addressFamilyId, that.addressFamilyId) &&
                Objects.equals(ipAddress, that.ipAddress) &&
                Objects.equals(nextHop, that.nextHop) &&
                Objects.equals(subnetMask, that.subnetMask);
    }


    /**
     * @return the Address Family Identifier
     */
    public short getAddressFamilyId() {
        return this.addressFamilyId;
    }

    /**
     * @param addressFamilyIdentifier the address family identifier to set
     * @return this
     */
    public RIPV2Entry setAddressFamilyId(final short addressFamilyIdentifier) {
        this.addressFamilyId = addressFamilyIdentifier;
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
    public RIPV2Entry setRouteTag(final short routetag) {
        this.routeTag = routetag;
        return this;
    }

    /**
     * @return the ip address
     */
    public Ip4Address getipAddress() {
        return this.ipAddress;
    }

    /**
     * @param ipaddress the Ip Address  to set
     * @return this
     */
    public RIPV2Entry setIpAddress(final Ip4Address ipaddress) {
        this.ipAddress = ipaddress;
        return this;
    }
    /**
     * @return the subnet mask
     */
    public Ip4Address getSubnetMask() {
        return this.subnetMask;
    }

    /**
     * @param subnetmask the subnet mask  to set
     * @return this
     */
    public RIPV2Entry setSubnetMask(final Ip4Address subnetmask) {
        this.subnetMask = subnetmask;
        return this;
    }

    /**
     * @return the next hop
     */
    public Ip4Address getNextHop() {
        return this.nextHop;
    }

    /**
     * @param nexthop the ip address if the next hop  to set
     * @return this
     */
    public RIPV2Entry setNextHop(final Ip4Address nexthop) {
        this.nextHop = nexthop;
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
    public RIPV2Entry setMetric(final int metric) {
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
        return "RIPV2Entry [address family Id=" + this.addressFamilyId + ", route tag=" + this.routeTag
                + ", Address=" + this.ipAddress
                + ", Subnet mask=" + this.subnetMask
                + ", Mext hop=" + this.nextHop
                + ", metric = " + this.metric + "]";
    }
}
