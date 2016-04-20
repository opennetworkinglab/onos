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
package org.onosproject.isis.io.isispacket.tlv.subtlv;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;
import org.onosproject.isis.io.util.IsisUtil;


/**
 * Representation of maximum reservable bandwidth TE value.
 */
public class MaximumReservableBandwidth extends TlvHeader implements TrafficEngineeringSubTlv {
    private float maximumReservableBandwidth;

    /**
     * Creates an instance of maximum reservable bandwidth.
     *
     * @param header TLV header
     */
    public MaximumReservableBandwidth(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Sets value of maximum reversible bandwidth.
     *
     * @param maximumBandwidth maximum reversible bandwidth
     */
    public void setMaximumBandwidth(float maximumBandwidth) {
        this.maximumReservableBandwidth = maximumBandwidth;
    }

    /**
     * Returns value of maximum reversible bandwidth.
     *
     * @return maximumBandwidth maximum reversible bandwidth
     */
    public float getMaximumBandwidthValue() {
        return this.maximumReservableBandwidth;
    }

    /**
     * Reads bytes from channel buffer.
     *
     * @param channelBuffer channel buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        byte[] tempByteArray = new byte[tlvLength()];
        channelBuffer.readBytes(tempByteArray, 0, tlvLength());
        int maxBandwidth = (IsisUtil.byteToInteger(tempByteArray));
        this.setMaximumBandwidth(Float.intBitsToFloat(maxBandwidth));
    }

    /**
     * Returns byte array of maximum reservable bandwidth.
     *
     * @return byte array of maximum reservable bandwidth
     */
    public byte[] asBytes() {
        byte[] linkSubType = null;

        byte[] linkSubTlvHeader = tlvHeaderAsByteArray();
        byte[] linkSubTlvBody = tlvBodyAsBytes();
        linkSubType = Bytes.concat(linkSubTlvHeader, linkSubTlvBody);
        return linkSubType;

    }

    /**
     * Returns maximum reservable bandwidth sub tlv body as byte array.
     *
     * @return byte of maximum reservable bandwidth sub tlv body
     */
    public byte[] tlvBodyAsBytes() {
        byte[] linkSubTypeBody;
        linkSubTypeBody = IsisUtil.convertToFourBytes(Float.floatToIntBits(this.maximumReservableBandwidth));

        return linkSubTypeBody;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("maximumReservableBandwidth", maximumReservableBandwidth)
                .toString();
    }
}