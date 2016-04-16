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
import org.onosproject.isis.io.isispacket.tlv.subtlv.TrafficEngineeringSubTlv;
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of IP external reachability TLV.
 */
public class IpExternalReachabilityTlv extends TlvHeader implements IsisTlv {

    private String sysIdAndPseudoNumber;
    private int defaultMetric;
    private byte subTlvLength;
    private List<TrafficEngineeringSubTlv> trafEnginSubTlv = new ArrayList<>();

    /**
     * Sets TLV type and TLV length for IP external reachability TLV.
     *
     * @param tlvHeader tlvHeader
     */
    public IpExternalReachabilityTlv(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    /**
     * Gets the system ID and pseudo number of IP external reachability TLV.
     *
     * @return sysIdAndPseudoNumber system ID and pseudo number
     */
    public String sysIdAndPseudoNumber() {
        return sysIdAndPseudoNumber;
    }

    /**
     * Gets the system ID and pseudo number for IP external reachability TLV.
     *
     * @param sysIdAndPseudoNumber system ID and pseudo number
     */
    public void setSysIdAndPseudoNumber(String sysIdAndPseudoNumber) {
        this.sysIdAndPseudoNumber = sysIdAndPseudoNumber;
    }

    /**
     * Adds the traffic engineering sub TLV to IP external reachability TLV.
     *
     * @param trafEnginSubTlv traffic engineering sub TLV
     */
    public void addSubTlv(TrafficEngineeringSubTlv trafEnginSubTlv) {
        this.trafEnginSubTlv.add(trafEnginSubTlv);
    }

    /**
     * Gets the sub TLV length of IP external reachability TLV.
     *
     * @return sub TLV length
     */
    public byte subTlvLength() {
        return subTlvLength;
    }

    /**
     * Sets the sub TLV length for IP external reachability TLV.
     *
     * @param  subTlvLength sub TLV length
     */
    public void setSubTlvLength(byte subTlvLength) {
        this.subTlvLength = subTlvLength;
    }

    /**
     * Gets default metric of IP external reachability TLV.
     *
     * @return default metric
     */
    public int defaultMetric() {
        return defaultMetric;
    }

    /**
     * Sets default metric for IP external reachability TLV.
     *
     * @param defaultMetric default metric
     */
    public void setDefaultMetric(int defaultMetric) {
        this.defaultMetric = defaultMetric;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        byte[] tempByteArray = new byte[IsisUtil.ID_PLUS_ONE_BYTE];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_PLUS_ONE_BYTE);
        this.setSysIdAndPseudoNumber(IsisUtil.systemIdPlus(tempByteArray));
        this.setDefaultMetric(channelBuffer.readUnsignedMedium());
        this.setSubTlvLength((byte) channelBuffer.readByte());
        while (channelBuffer.readableBytes() > 0) {
            TlvHeader tlvHeader = new TlvHeader();
            tlvHeader.setTlvType(channelBuffer.readByte());
            tlvHeader.setTlvLength(channelBuffer.readByte());
            this.addSubTlv(SubTlvFinder.findSubTlv(tlvHeader,
                                                   channelBuffer.readBytes(tlvHeader.tlvLength())));
        }
    }

    @Override
    public byte[] asBytes() {
        byte[] bytes = null;
        byte[] tlvHeader = tlvHeaderAsByteArray();
        byte[] tlvBody = tlvBodyAsBytes();
        //systemID + pseudo number+length of subtlv=11l
        tlvBody[10] = (byte) (tlvBody.length - 11);
        tlvHeader[1] = (byte) tlvBody.length;
        bytes = Bytes.concat(tlvHeader, tlvBody);
        return bytes;
    }

    /**
     * Gets TLV body of IP external reachability TLV.
     *
     * @return byteArray TLV body of IP external reachability TLV.
     */
    public byte[] tlvBodyAsBytes() {
        List<Byte> bodyLst = new ArrayList<>();
        bodyLst.addAll(IsisUtil.sourceAndLanIdToBytes(this.sysIdAndPseudoNumber()));
        bodyLst.addAll(Bytes.asList(IsisUtil.convertToThreeBytes(this.defaultMetric())));
        bodyLst.add(this.subTlvLength());
        for (TrafficEngineeringSubTlv trafficEngineeringSubTlv : this.trafEnginSubTlv) {
            bodyLst.addAll(SubTlvToBytes.tlvToBytes(trafficEngineeringSubTlv));
        }
        return Bytes.toArray(bodyLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("sysIdAndPseudoNumber", sysIdAndPseudoNumber)
                .add("defaultMetric", defaultMetric)
                .add("subTlvLength", subTlvLength)
                .toString();
    }
}