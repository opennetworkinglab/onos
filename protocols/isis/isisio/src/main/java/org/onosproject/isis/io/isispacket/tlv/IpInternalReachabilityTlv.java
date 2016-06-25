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
 * Representation of IP internal reachability TLV.
 */
public class IpInternalReachabilityTlv extends TlvHeader implements IsisTlv {
    private List<MetricOfInternalReachability> metricOfInternalReachability = new ArrayList<>();

    /**
     * Creates an instance of IP internal reachability TLV.
     *
     * @param tlvHeader TLV header
     */
    public IpInternalReachabilityTlv(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    /**
     * Adds the metric of internal reachability to internal reachability TLV.
     *
     * @param metricValue metric of internal reachability
     */
    public void addInternalReachabilityMetric(MetricOfInternalReachability metricValue) {
        this.metricOfInternalReachability.add(metricValue);
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        while (channelBuffer.readableBytes() > 0) {
            MetricOfInternalReachability metricOfInternalReachability = new MetricOfInternalReachability();
            metricOfInternalReachability.readFrom(channelBuffer);
            this.metricOfInternalReachability.add(metricOfInternalReachability);
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
     * Returns TLV body of internal reachability TLV.
     *
     * @return byteArray TLV body of area address TLV
     */
    private byte[] tlvBodyAsBytes() {
        List<Byte> bytes = new ArrayList<>();
        for (MetricOfInternalReachability metricOfInternalReachability :
                this.metricOfInternalReachability) {
            bytes.addAll(Bytes.asList(metricOfInternalReachability.asBytes()));
        }
        return Bytes.toArray(bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("metricOfInternalReachability", metricOfInternalReachability)
                .toString();
    }
}