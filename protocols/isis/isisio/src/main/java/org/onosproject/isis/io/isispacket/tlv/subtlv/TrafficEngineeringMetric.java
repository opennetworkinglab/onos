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
 * Representation of traffic engineering metric TE value.
 */
public class TrafficEngineeringMetric extends TlvHeader implements TrafficEngineeringSubTlv {
    private long trafficEngineeringMetric;

    /**
     * Creates an instance of traffic engineering metric .
     *
     * @param header tlv header instance
     */
    public TrafficEngineeringMetric(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Sets TE metric value.
     *
     * @param trafficEngineeringMetric value of trafficEngineeringMetric
     */
    public void setTrafficEngineeringMetric(long trafficEngineeringMetric) {
        this.trafficEngineeringMetric = trafficEngineeringMetric;
    }

    /**
     * Returns TE metric value.
     *
     * @return value of traffic engineering metric
     */
    public long getTrafficEngineeringMetricValue() {
        return this.trafficEngineeringMetric;
    }

    /**
     * Reads bytes from channel buffer .
     *
     * @param channelBuffer channel buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        byte[] tempByteArray = new byte[tlvLength()];
        channelBuffer.readBytes(tempByteArray, 0, tlvLength());
        this.setTrafficEngineeringMetric(IsisUtil.byteToLong(tempByteArray));
    }

    /**
     * Returns instance as byte array.
     *
     * @return instance as byte array
     */
    public byte[] asBytes() {
        byte[] linkSubType = null;

        byte[] linkSubTlvHeader = tlvHeaderAsByteArray();
        byte[] linkSubTlvBody = tlvBodyAsBytes();
        linkSubType = Bytes.concat(linkSubTlvHeader, linkSubTlvBody);

        return linkSubType;
    }

    /**
     * Returns trafficEngineeringMetric as byte array .
     *
     * @return byte array of trafficEngineeringMetric
     */
    public byte[] tlvBodyAsBytes() {

        byte[] linkSubTypeBody;
        linkSubTypeBody = IsisUtil.convertToFourBytes(this.trafficEngineeringMetric);

        return linkSubTypeBody;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("trafficEngineeringMetric", trafficEngineeringMetric)
                .toString();
    }
}