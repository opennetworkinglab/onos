/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgpio.types;

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Provides MultiProtocolExtnCapabilityTlv.
 */
public class MultiProtocolExtnCapabilityTlv implements BgpValueType {

    /*
        0       7       15      23      31
        +-------+-------+-------+-------+
        |  AFI          | Res   |  SAFI |
        +-------+-------+-------+-------+

        Multiprotocol Extensions CAPABILITY TLV format
        REFERENCE : RFC 4760
     */
    private static final Logger log = LoggerFactory
            .getLogger(MultiProtocolExtnCapabilityTlv.class);

    public static final byte TYPE = 1;
    public static final byte LENGTH = 4;

    private final short afi;
    private final byte res;
    private final byte safi;

    /**
     * Constructor to initialize variables.
     * @param afi Address Family Identifiers
     * @param res reserved field
     * @param safi Subsequent Address Family Identifier
     */
    public MultiProtocolExtnCapabilityTlv(short afi, byte res, byte safi) {
        this.afi = afi;
        this.res = res;
        this.safi = safi;
    }

    /**
     * Returns object of MultiProtocolExtnCapabilityTlv.
     * @param afi Address Family Identifiers
     * @param res reserved field
     * @param safi Subsequent Address Family Identifier
     * @return object of MultiProtocolExtnCapabilityTlv
     */
    public static MultiProtocolExtnCapabilityTlv of(short afi, byte res,
                                                    byte safi) {
        return new MultiProtocolExtnCapabilityTlv(afi, res, safi);
    }

    /**
     * Returns afi Address Family Identifiers value.
     * @return afi Address Family Identifiers value
     */
    public short getAfi() {
        return afi;
    }

    /**
     * Returns res reserved field value.
     * @return res reserved field value
     */
    public byte getRes() {
        return res;
    }

    /**
     * Returns safi Subsequent Address Family Identifier value.
     * @return safi Subsequent Address Family Identifier value
     */
    public byte getSafi() {
        return safi;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(afi, res, safi);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MultiProtocolExtnCapabilityTlv) {
            MultiProtocolExtnCapabilityTlv other = (MultiProtocolExtnCapabilityTlv) obj;
            return Objects.equals(this.afi, other.afi)
                    && Objects.equals(this.res, other.res)
                    && Objects.equals(this.safi, other.safi);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeByte(TYPE);
        cb.writeByte(LENGTH);

        // write afi
        cb.writeShort(afi);

        // write res
        cb.writeByte(res);

        // write safi
        cb.writeByte(safi);

        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads from channel buffer and returns object of MultiprotocolCapabilityTlv.
     * @param cb of type channel buffer
     * @return object of MultiProtocolExtnCapabilityTlv
     */
    public static BgpValueType read(ChannelBuffer cb) {
        short afi = cb.readShort();
        byte res = cb.readByte();
        byte safi = cb.readByte();
        return new MultiProtocolExtnCapabilityTlv(afi, res, safi);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("AFI", afi)
                .add("Reserved", res)
                .add("SAFI", safi).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
