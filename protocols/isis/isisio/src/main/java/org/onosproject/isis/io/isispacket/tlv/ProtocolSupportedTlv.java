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

import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents Protocol supported TLV.
 */
public class ProtocolSupportedTlv extends TlvHeader implements IsisTlv {

    private List<Byte> protocolSupported = new ArrayList();

    /**
     * Sets TLV type and TLV length of protocol supported TLV.
     *
     * @param tlvHeader tlvHeader.
     */
    public ProtocolSupportedTlv(TlvHeader tlvHeader) {

        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());

    }

    /**
     * Gets the Protocol Supported by the TLV.
     *
     * @return Protocol Supported
     */
    public List<Byte> protocolSupported() {

        return this.protocolSupported;

    }

    @Override
    public void readFrom(ByteBuf byteBuf) {

        while (byteBuf.readableBytes() > 0) {
            this.protocolSupported.add(byteBuf.readByte());
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
     * Gets TLV body of protocol supported TLV.
     *
     * @return byteArray TLV body of protocol supported TLV
     */
    public byte[] tlvBodyAsBytes() {

        List<Byte> bytes = new ArrayList();
        for (byte byt : this.protocolSupported) {
            bytes.add(byt);
        }
        byte[] byteArray = new byte[bytes.size()];
        int i = 0;
        for (byte byt : bytes) {
            byteArray[i++] = byt;
        }
        return byteArray;
    }
}