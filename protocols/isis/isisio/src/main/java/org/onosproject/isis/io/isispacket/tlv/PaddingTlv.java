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
 * Representation of padding TLV.
 */
public class PaddingTlv extends TlvHeader implements IsisTlv {
    private List<Byte> paddings = new ArrayList<>();

    /**
     * Creates an instance of padding TLV.
     *
     * @param tlvHeader TLV header
     */
    public PaddingTlv(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        while (channelBuffer.readableBytes() > 0) {
            this.paddings.add(channelBuffer.readByte());
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
     * Returns TLV body of padding TLV.
     *
     * @return byteArray TLV body of padding TLV
     */
    private byte[] tlvBodyAsBytes() {
        byte[] areaArea = new byte[this.tlvLength()];
        return areaArea;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("paddings", paddings)
                .toString();
    }
}