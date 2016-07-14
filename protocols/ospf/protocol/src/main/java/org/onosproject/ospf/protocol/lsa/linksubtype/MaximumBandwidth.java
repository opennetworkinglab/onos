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
package org.onosproject.ospf.protocol.lsa.linksubtype;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.ospf.protocol.lsa.TlvHeader;
import org.onosproject.ospf.protocol.util.OspfUtil;

/**
 * Representation of maximum bandwidth TE value.
 */
public class MaximumBandwidth extends TlvHeader implements LinkSubType {
    private float maximumBandwidth;

    /**
     * Creates an instance of maximum bandwidth.
     *
     * @param header tlv header instance
     */
    public MaximumBandwidth(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Sets value of maximum bandwidth.
     *
     * @param maximumBandwidth value of maximum bandwidth
     */
    public void setMaximumBandwidth(float maximumBandwidth) {
        this.maximumBandwidth = maximumBandwidth;
    }

    /**
     * Gets value of maximum bandwidth.
     *
     * @return maximumBandwidth value of maximum bandwidth
     */
    public float getMaximumBandwidthValue() {
        return this.maximumBandwidth;
    }

    /**
     * Reads bytes from channel buffer.
     *
     * @param channelBuffer channel buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        byte[] tempByteArray = new byte[tlvLength()];
        channelBuffer.readBytes(tempByteArray, 0, tlvLength());
        int maxBandwidth = (OspfUtil.byteToInteger(tempByteArray));
        this.setMaximumBandwidth(Float.intBitsToFloat(maxBandwidth));
    }

    /**
     * Gets byte array of maximum bandwidth sub tlv.
     *
     * @return byte array of maximum bandwidth sub tlv
     */
    public byte[] asBytes() {
        byte[] linkSubType = null;
        byte[] linkSubTlvHeader = getTlvHeaderAsByteArray();
        byte[] linkSubTlvBody = getLinkSubTypeTlvBodyAsByteArray();
        linkSubType = Bytes.concat(linkSubTlvHeader, linkSubTlvBody);

        return linkSubType;
    }

    /**
     * Gets maximum bandwidth sub tlv byte array.
     *
     * @return byte array of maximum bandwidth sub tlv
     */
    public byte[] getLinkSubTypeTlvBodyAsByteArray() {
        byte[] linkSubTypeBody;
        linkSubTypeBody = OspfUtil.convertToFourBytes(Float.floatToIntBits(this.maximumBandwidth));
        return linkSubTypeBody;
    }

    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("maximumBandwidth", maximumBandwidth)
                .toString();
    }
}