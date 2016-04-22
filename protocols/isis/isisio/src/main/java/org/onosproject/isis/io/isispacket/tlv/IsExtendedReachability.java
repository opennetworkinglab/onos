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
 * Representation of IS extended reachability TLV.
 */
public class IsExtendedReachability extends TlvHeader implements IsisTlv {

    private String neighborId;
    private int metric;
    private List<TrafficEngineeringSubTlv> trafEnginSubTlv = new ArrayList<>();

    /**
     * Creates an instance of IP external reachability TLV.
     *
     * @param tlvHeader TLV header
     */
    public IsExtendedReachability(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    /**
     * Returns neighbor ID.
     *
     * @return neighbor ID
     */
    public String neighborId() {
        return neighborId;
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
     * Adds the traffic engineering sub TLV to IS extended reachability TLV.
     *
     * @param trafEnginSubTlv traffic engineering sub TLV
     */
    public void addSubTlv(TrafficEngineeringSubTlv trafEnginSubTlv) {
        this.trafEnginSubTlv.add(trafEnginSubTlv);
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        byte[] tempByteArray = new byte[IsisUtil.ID_SIX_BYTES];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_SIX_BYTES);
        this.setNeighborId(IsisUtil.systemId(tempByteArray));
        this.setMetric(channelBuffer.readUnsignedMedium());
        while (channelBuffer.readableBytes() > 0) {
            TlvHeader tlvHeader = new TlvHeader();
            tlvHeader.setTlvType(channelBuffer.readByte());
            tlvHeader.setTlvLength(channelBuffer.readByte());
            SubTlvType tlvValue = SubTlvType.get(tlvHeader.tlvType());
            if (tlvValue != null) {
                this.addSubTlv(SubTlvFinder.findSubTlv(tlvHeader,
                                                       channelBuffer.readBytes(tlvHeader.tlvLength())));
            } else {
                channelBuffer.readBytes(tlvHeader.tlvLength());
            }
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
     * Returns TLV body of IS extended reachability TLV.
     *
     * @return byteArray TLV body of IS extended reachability TLV.
     */
    private byte[] tlvBodyAsBytes() {
        List<Byte> byteList = new ArrayList<>();
        byteList.addAll(IsisUtil.sourceAndLanIdToBytes(this.neighborId()));
        byteList.addAll(Bytes.asList(IsisUtil.convertToThreeBytes(this.metric())));
        if (this.trafEnginSubTlv.size() > 0) {
            for (TrafficEngineeringSubTlv trafficEngineeringSubTlv : this.trafEnginSubTlv) {
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
                .add("trafEnginSubTlv", trafEnginSubTlv)
                .toString();
    }

}
