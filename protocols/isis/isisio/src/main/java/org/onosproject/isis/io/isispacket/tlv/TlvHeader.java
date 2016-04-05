/*
 * Copyright 2016 Open Networking Laboratory
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
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents TLV header.
 */
public class TlvHeader implements IsisTlv {
    private int tlvType;
    private int tlvLength;

    /**
     * Gets the TLV length of the TLV.
     *
     * @return tlvLength TLV length
     */
    public int tlvLength() {
        return tlvLength;
    }

    /**
     * Sets the TLV length for the mTLV.
     *
     * @param tlvLength TLV length
     */
    public void setTlvLength(int tlvLength) {
        this.tlvLength = tlvLength;
    }

    /**
     * Gets the TLV type of the TLV.
     *
     * @return tlvType TLV type
     */
    public int tlvType() {
        return tlvType;
    }

    /**
     * Sets TLV type for the TLV.
     *
     * @param tlvType TLV type
     */
    public void setTlvType(int tlvType) {
        this.tlvType = tlvType;
    }

    /**
     * Sets the TLV values of TLV from b yte buffer.
     *
     * @param byteBuf byteBuf.
     */
    public void readFrom(ByteBuf byteBuf) {
        //implemented in sub classes
    }


    /**
     * Gets the TLV of the TLV as bytes.
     *
     * @return null
     */
    public byte[] asBytes() {
        //implemented the subclasses
        return null;
    }

    /**
     * Gets the TLV header of the TLV.
     *
     * @return headerLst TLV of the TLV
     */
    public byte[] tlvHeaderAsByteArray() {
        List<Byte> headerLst = new ArrayList();
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