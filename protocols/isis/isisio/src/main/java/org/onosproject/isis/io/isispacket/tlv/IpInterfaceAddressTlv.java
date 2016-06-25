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
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of IP interface address TLV.
 */
public class IpInterfaceAddressTlv extends TlvHeader implements IsisTlv {

    private List<Ip4Address> interfaceAddress = new ArrayList<>();

    /**
     * Creates an instance of IP interface address TLV.
     *
     * @param tlvHeader TLV header
     */
    public IpInterfaceAddressTlv(TlvHeader tlvHeader) {

        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());

    }

    /**
     * Adds the interface address to IP interface address TLV.
     *
     * @param interfaceAddress interface address
     */
    public void addInterfaceAddres(Ip4Address interfaceAddress) {
        this.interfaceAddress.add(interfaceAddress);
    }

    /**
     * Returns the interface address of IP interface address TLV.
     *
     * @return interface address
     */
    public List<Ip4Address> interfaceAddress() {
        return interfaceAddress;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        while (channelBuffer.readableBytes() >= 4) {
            byte[] addressbytes = new byte[IsisUtil.FOUR_BYTES];
            channelBuffer.readBytes(addressbytes, 0, IsisUtil.FOUR_BYTES);
            this.interfaceAddress.add(Ip4Address.valueOf(addressbytes));
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
     * Returns TLV body of IP interface address TLV.
     *
     * @return byteArray TLV body of IP interface address TLV
     */
    private byte[] tlvBodyAsBytes() {
        List<Byte> bytes = new ArrayList<>();
        for (Ip4Address ip4Address : this.interfaceAddress) {
            bytes.addAll(Bytes.asList(ip4Address.toOctets()));
        }
        return Bytes.toArray(bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("interfaceAddress", interfaceAddress)
                .toString();
    }
}