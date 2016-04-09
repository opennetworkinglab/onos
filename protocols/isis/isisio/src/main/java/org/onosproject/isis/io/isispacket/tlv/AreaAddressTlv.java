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
import io.netty.buffer.ByteBuf;
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the area address TLV.
 */
public class AreaAddressTlv extends TlvHeader implements IsisTlv {

    private List<String> areaAddress = new ArrayList();

    /**
     * Sets TLV type and TLV length of area address TLV.
     *
     * @param tlvHeader tlvHeader.
     */
    public AreaAddressTlv(TlvHeader tlvHeader) {

        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());

    }

    /**
     * Gets the area address of area address TLV.
     *
     * @return area address
     */
    public List<String> areaAddress() {
        return this.areaAddress;
    }

    @Override
    public void readFrom(ByteBuf byteBuf) {
        while (byteBuf.readableBytes() > 0) {
            int addressLength = byteBuf.readByte();
            byte[] addressBytes = new byte[IsisUtil.THREE_BYTES];
            byteBuf.readBytes(addressBytes, 0, IsisUtil.THREE_BYTES);
            String areaAddress = IsisUtil.areaAddres(addressBytes);
            this.areaAddress.add(areaAddress);
        }
    }

    @Override
    public byte[] asBytes() {
        byte[] bytes = null;

        byte[] tlvHeader = tlvHeaderAsByteArray();
        byte[] tlvBody = tlvBodyAsBytes();
        bytes = Bytes.concat(tlvHeader, tlvBody);

        return bytes;
    }

    /**
     * Gets TLV body of area address TLV.
     *
     * @return byteArray TLV body of area address TLV
     */
    public byte[] tlvBodyAsBytes() {

        List<Byte> bytes = new ArrayList();
        for (String areaAddress : this.areaAddress) {
            bytes.add((byte) (areaAddress.length() / 2));
            bytes.addAll(IsisUtil.areaAddresToBytes(areaAddress));
        }
        byte[] byteArray = new byte[bytes.size()];
        int i = 0;
        for (byte byt : bytes) {
            byteArray[i++] = byt;
        }
        return byteArray;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .toString();
    }
}