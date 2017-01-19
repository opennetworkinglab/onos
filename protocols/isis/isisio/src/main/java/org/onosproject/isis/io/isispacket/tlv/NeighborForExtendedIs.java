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
import org.onosproject.isis.io.isispacket.tlv.subtlv.SubTlvFinder;
import org.onosproject.isis.io.isispacket.tlv.subtlv.SubTlvToBytes;
import org.onosproject.isis.io.isispacket.tlv.subtlv.SubTlvType;
import org.onosproject.isis.io.isispacket.tlv.subtlv.TrafficEngineeringSubTlv;
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of IS extended reachability neighbors.
 */
public class NeighborForExtendedIs {

    private String neighborId;
    private int metric;
    private List<TrafficEngineeringSubTlv> teSubTlv = new ArrayList<>();

    /**
     * Returns neighbor ID.
     *
     * @return neighbor ID
     */
    public String neighborId() {
        return neighborId;
    }

    /**
     * Returns list of sub tlvs.
     *
     * @return teSubTlv list of sub tlvs
     */
    public List<TrafficEngineeringSubTlv> teSubTlv() {
        return teSubTlv;
    }

    /**
     * Sets neighbor ID.
     *
     * @param neighborId neighbor ID
     */
    public void setNeighborId(String neighborId) {
        this.neighborId = neighborId;
    }

    /**
     * Returns metric.
     *
     * @return metric
     */
    public int metric() {
        return metric;
    }

    /**
     * Sets metric.
     *
     * @param metric metric
     */
    public void setMetric(int metric) {
        this.metric = metric;
    }

    /**
     * Adds the TE for extended IS instance to IS extended reachability TLV.
     *
     * @param trafficEngineeringSubTlv TE for extended IS instance
     */
    public void addSubTlv(TrafficEngineeringSubTlv trafficEngineeringSubTlv) {
        this.teSubTlv.add(trafficEngineeringSubTlv);
    }

    /**
     * Reads from the channel buffer and populate this instance.
     *
     * @param channelBuffer channel buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        byte[] tempByteArray = new byte[IsisUtil.ID_PLUS_ONE_BYTE];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_PLUS_ONE_BYTE);
        this.setNeighborId(IsisUtil.systemIdPlus(tempByteArray));
        this.setMetric(channelBuffer.readUnsignedMedium());
        int nTlvPresent = channelBuffer.readByte();
        if (nTlvPresent > 0) {
            while (channelBuffer.readableBytes() > IsisUtil.TWO_BYTES) {
                TlvHeader tlvHeader = new TlvHeader();
                tlvHeader.setTlvType(channelBuffer.readByte());
                tlvHeader.setTlvLength(channelBuffer.readByte());
                SubTlvType tlvValue = SubTlvType.get(tlvHeader.tlvType());
                int tlvLength = tlvHeader.tlvLength();
                if (tlvValue != null) {
                    if (channelBuffer.readableBytes() >= tlvLength) {
                        TrafficEngineeringSubTlv subTlv =
                                SubTlvFinder.findSubTlv(tlvHeader, channelBuffer.readBytes(tlvHeader.tlvLength()));
                        if (subTlv != null) {
                            this.addSubTlv(subTlv);
                        }
                    }
                } else {
                    if (channelBuffer.readableBytes() >= tlvLength) {
                        channelBuffer.readBytes(tlvLength);
                    }
                }
            }
        }
    }

    /**
     * Returns neighbor details of IS extended reachability TLV.
     *
     * @return neighbor details of IS extended reachability TLV.
     */
    public byte[] neighborBodyAsbytes() {
        List<Byte> byteList = new ArrayList<>();
        byteList.addAll(IsisUtil.sourceAndLanIdToBytes(this.neighborId()));
        byteList.addAll(Bytes.asList(IsisUtil.convertToThreeBytes(this.metric())));
        if (!this.teSubTlv.isEmpty()) {
            for (TrafficEngineeringSubTlv trafficEngineeringSubTlv : this.teSubTlv) {
                byteList.addAll(SubTlvToBytes.tlvToBytes(trafficEngineeringSubTlv));
            }
        }
        return Bytes.toArray(byteList);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("neighborId", neighborId)
                .add("metric", metric)
                .add("teSubTlv", teSubTlv)
                .toString();
    }
}