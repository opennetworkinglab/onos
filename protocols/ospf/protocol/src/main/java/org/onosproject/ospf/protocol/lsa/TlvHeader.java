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
package org.onosproject.ospf.protocol.lsa;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.onosproject.ospf.protocol.util.OspfUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a TLV header.
 */
public class TlvHeader {
    private int tlvType;
    private int tlvLength;

    /**
     * Gets TLV length.
     *
     * @return TLV length
     */
    public int tlvLength() {
        return tlvLength;
    }

    /**
     * Sets TLV length.
     *
     * @param tlvLength TLV length
     */
    public void setTlvLength(int tlvLength) {
        this.tlvLength = tlvLength;
    }

    /**
     * Gets TLV type.
     *
     * @return TLV type
     */
    public int tlvType() {
        return tlvType;
    }

    /**
     * Sets TLV type.
     *
     * @param tlvType TLV type
     */
    public void setTlvType(int tlvType) {
        this.tlvType = tlvType;
    }

    /**
     * Gets TLV header as bytes.
     *
     * @return TLV header as bytes
     */
    public byte[] getTlvHeaderAsByteArray() {
        List<Byte> headerLst = new ArrayList();
        headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.tlvType)));
        headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.tlvLength)));
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