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
 * Representation of IS reachability TLV.
 */
public class IsReachabilityTlv extends TlvHeader {

    private int reserved;
    private List<MetricsOfReachability> metricsOfReachabilities = new ArrayList<>();

    /**
     * Creates an instance of IS reachability TLV.
     *
     * @param tlvHeader TLV header
     */
    public IsReachabilityTlv(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    /**
     * Returns the reserved value of IS reachability TLV.
     *
     * @return reserved
     */
    public int reserved() {
        return reserved;
    }

    /**
     * Sets the reserved value for IS reachability TLV.
     *
     * @param reserved reserved
     */
    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    /**
     * Adds the metric of reachability to IS reachability TLV..
     *
     * @param metricsOfReachability metric of reachability
     */
    public void addMeticsOfReachability(MetricsOfReachability metricsOfReachability) {
        this.metricsOfReachabilities.add(metricsOfReachability);
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        this.setReserved(channelBuffer.readByte());
        while (channelBuffer.readableBytes() > 0) {
            MetricsOfReachability metricsOfReachability = new MetricsOfReachability();
            metricsOfReachability.readFrom(channelBuffer);
            this.metricsOfReachabilities.add(metricsOfReachability);
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
     * Returns TLV body of IS reachability TLV.
     *
     * @return byteArray TLV body of area address TLV
     */
    private byte[] tlvBodyAsBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) this.reserved());
        for (MetricsOfReachability metricsOfReachability : this.metricsOfReachabilities) {
            bytes.addAll(Bytes.asList(metricsOfReachability.asBytes()));
        }
        return Bytes.toArray(bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("metricsOfReachabilities", metricsOfReachabilities)
                .toString();
    }
}
