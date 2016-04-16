/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.isis.io.isispacket.tlv;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of TLV header.
 */
public class TlvHeader implements IsisTlv {
    private int tlvType;
    private int tlvLength;

    /**
     * Returns TLV length of TLV.
     *
     * @return TLV length
     */
    public int tlvLength() {
        return tlvLength;
    }

    /**
     * Sets TLV length for TLV.
     *
     * @param tlvLength TLV length
     */
    public void setTlvLength(int tlvLength) {
        this.tlvLength = tlvLength;
    }

    /**
     * Returns TLV type of TLV.
     *
     * @return TLV type
     */
    public int tlvType() {
        return tlvType;
    }

    /**
     * Sets TLV type for TLV.
     *
     * @param tlvType TLV type
     */
    public void setTlvType(int tlvType) {
        this.tlvType = tlvType;
    }

    /**
     * Sets TLV values from channel buffer.
     *
     * @param channelBuffer channel Buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        //implemented in sub classes
    }


    /**
     * Returns TLV as byte array.
     *
     * @return byteArray TLV body of area address TLV
     */
    public byte[] asBytes() {
        return null;
    }

    /**
     * Returns TLV header of TLV as bytes.
     *
     * @return TLV header as bytes
     */
    public byte[] tlvHeaderAsByteArray() {
        List<Byte> headerLst = new ArrayList<>();
        headerLst.add((byte) this.tlvType);
        headerLst.add((byte) this.tlvLength);
        return Bytes.toArray(headerLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("tlvType", tlvType)
                .add("tlvLength", tlvLength)
                .toString();
    }
}