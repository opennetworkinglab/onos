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
 * Representation of  protocol supported TLV.
 */
public class ProtocolSupportedTlv extends TlvHeader implements IsisTlv {

    private List<Byte> protocolSupported = new ArrayList<>();

    /**
     * Creates an instance of protocol supported TLV.
     *
     * @param tlvHeader TLV header
     */
    public ProtocolSupportedTlv(TlvHeader tlvHeader) {

        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());

    }

    /**
     * Adds the protocol supported to protocol supported TLV.
     *
     * @param protocolValue protocol supported
     */
    public void addProtocolSupported(byte protocolValue) {
        protocolSupported.add(protocolValue);
    }

    /**
     * Returns protocols supported of protocol supported TLV.
     *
     * @return protocol supported
     */
    public List<Byte> protocolSupported() {
        return this.protocolSupported;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        while (channelBuffer.readableBytes() > 0) {
            this.protocolSupported.add(channelBuffer.readByte());
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
     * Gets TLV body of protocol supported TLV.
     *
     * @return byteArray TLV body of protocol supported TLV
     */
    private byte[] tlvBodyAsBytes() {
        List<Byte> bytes = new ArrayList<>();
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("protocolSupported", protocolSupported)
                .toString();
    }
}