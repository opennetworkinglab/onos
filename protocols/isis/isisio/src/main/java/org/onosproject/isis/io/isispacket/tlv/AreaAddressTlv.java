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
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of area address TLV.
 */
public class AreaAddressTlv extends TlvHeader implements IsisTlv {

    private List<String> areaAddress = new ArrayList<>();

    /**
     * Creates an instance of area address TLV.
     *
     * @param tlvHeader TLV header
     */
    public AreaAddressTlv(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    /**
     * Adds the area address to the area address TLV.
     *
     * @param areaAddress area address
     */
    public void addAddress(String areaAddress) {
        this.areaAddress.add(areaAddress);
    }

    /**
     * Returns the area address of area address TLV.
     *
     * @return areaAddress area address
     */
    public List<String> areaAddress() {
        return this.areaAddress;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        while (channelBuffer.readableBytes() > 0) {
            int addressLength = channelBuffer.readByte();
            byte[] addressbytes = new byte[addressLength];
            channelBuffer.readBytes(addressbytes, 0, addressLength);
            String areaAddress = IsisUtil.areaAddres(addressbytes);
            this.areaAddress.add(areaAddress);
        }
    }

    @Override
    public byte[] asBytes() {
        byte[] bytes = null;
        byte[] tlvHeader = tlvHeaderAsByteArray();
        byte[] tlvBody = tlvBodyAsBytes();
        tlvHeader[1] = (byte) tlvBody.length;
        bytes = Bytes.concat(tlvHeader, tlvBody);
        return bytes;
    }

    /**
     * Returns TLV body of area address TLV.
     *
     * @return byteArray TLV body of area address TLV
     */
    private byte[] tlvBodyAsBytes() {
        List<Byte> bytes = new ArrayList<>();
        for (String areaAddress : this.areaAddress) {
            bytes.add((byte) (areaAddress.length() / 2));
            bytes.addAll(IsisUtil.areaAddressToBytes(areaAddress));
        }
        return Bytes.toArray(bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("areaAddress", areaAddress)
                .toString();
    }
}