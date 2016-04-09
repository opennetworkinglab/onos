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
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents IP interface address TLV.
 */
public class IpInterfaceAddressTlv extends TlvHeader implements IsisTlv {
    private List<Ip4Address> interfaceAddress = new ArrayList();

    /**
     * Sets TLV type and TLV length of IP interface address TLV.
     *
     * @param tlvHeader tlvHeader.
     */
    public IpInterfaceAddressTlv(TlvHeader tlvHeader) {

        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());

    }

    /**
     * Gets interface address of interface address TLV.
     *
     * @return interfaceAddress interface address
     */
    public List<Ip4Address> interfaceAddress() {
        return interfaceAddress;
    }

    @Override
    public void readFrom(ByteBuf byteBuf) {
        while (byteBuf.readableBytes() >= 4) {
            byte[] addressbytes = new byte[IsisUtil.FOUR_BYTES];
            byteBuf.readBytes(addressbytes, 0, IsisUtil.FOUR_BYTES);
            this.interfaceAddress.add(Ip4Address.valueOf(addressbytes));
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
     * Gets TLV body of interface address TLV.
     *
     * @return byteArray TLV body of interface address TLV.
     */
    public byte[] tlvBodyAsBytes() {

        List<Byte> bytes = new ArrayList();
        for (Ip4Address ip4Address : this.interfaceAddress) {
            bytes.addAll(Bytes.asList(ip4Address.toOctets()));
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