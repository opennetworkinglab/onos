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
 * Representation of IP extended reachability TLV.
 */
public class IpExtendedReachabilityTlv extends TlvHeader implements IsisTlv {

    private boolean down;
    private boolean subTlvPresence;
    private int prefixLength;
    private int metric;
    private byte subTlvLength;
    private String prefix;
    private List<TrafficEngineeringSubTlv> trafEnginSubTlv = new ArrayList<>();

    /**
     * Creates an instance of IP external reachability TLV.
     *
     * @param tlvHeader TLV header
     */
    public IpExtendedReachabilityTlv(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    /**
     * Returns list of traffic engineering sub tlvs.
     *
     * @return trafEnginSubTlv
     */
    public List<TrafficEngineeringSubTlv> teTlvs() {
        return this.trafEnginSubTlv;
    }

    /**
     * Returns the prefix of IP external reachability TLV.
     *
     * @return prefix
     */
    public String prefix() {
        return prefix;
    }

    /**
     * Sets the prefix of IP external reachability TLV.
     *
     * @param prefix prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns if down true else false of IP external reachability TLV.
     *
     * @return if down true else false
     */
    public boolean isDown() {
        return down;
    }

    /**
     * Sets if down true else false of IP external reachability TLV.
     *
     * @param upOrDown if down true else false
     */
    public void setDown(boolean upOrDown) {
        this.down = upOrDown;
    }

    /**
     * Returns true if sub TLV present else false of IP external reachability TLV.
     *
     * @return true if present else false
     */
    public boolean isSubTlvPresence() {
        return subTlvPresence;
    }

    /**
     * Sets true if sub TLV present else false of IP external reachability TLV.
     *
     * @param subTlvPresence true if present else false
     */
    public void setSubTlvPresence(boolean subTlvPresence) {
        this.subTlvPresence = subTlvPresence;
    }

    /**
     * Sets the prefix length of IP external reachability TLV.
     *
     * @return prefix length
     */
    public int prefixLength() {
        return prefixLength;
    }

    /**
     * Returns the prefix length of IP external reachability TLV.
     *
     * @param prefixLength the prefix length of IP external reachability TLV
     */
    public void setPrefixLength(int prefixLength) {
        this.prefixLength = prefixLength;
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
     * Returns the sub TLV length of IP external reachability TLV.
     *
     * @return sub TLV length
     */
    public byte subTlvLength() {
        return subTlvLength;
    }

    /**
     * Sets the sub TLV length for IP external reachability TLV.
     *
     * @param subTlvLength sub TLV length
     */
    public void setSubTlvLength(byte subTlvLength) {
        this.subTlvLength = subTlvLength;
    }

    /**
     * Returns metric of IP external reachability TLV.
     *
     * @return metric
     */
    public int metric() {
        return metric;
    }

    /**
     * Sets default metric for IP external reachability TLV.
     *
     * @param metric default metric
     */
    public void setMetric(int metric) {
        this.metric = metric;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        this.setMetric(channelBuffer.readInt());
        int controlInfo = channelBuffer.readByte();
        byte[] tempByteArray = null;

        String string = IsisUtil.toEightBitBinary(Integer.toBinaryString(controlInfo));
        if (string.charAt(0) == '0') {
            this.setDown(false);
        }
        if (string.charAt(1) == '1') {
            this.setSubTlvPresence(true);
        }
        this.setPrefixLength(Integer.parseInt(string.substring(2, string.length()), 2));
        if (this.prefixLength >= 0 && this.prefixLength <= 8) {
            channelBuffer.readByte();
        } else if (this.prefixLength >= 8 && this.prefixLength <= 16) {
            tempByteArray = new byte[IsisUtil.TWO_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, IsisUtil.TWO_BYTES);
            this.setPrefix(IsisUtil.prefixConversion(tempByteArray));
        } else if (this.prefixLength >= 17 && this.prefixLength <= 24) {
            tempByteArray = new byte[IsisUtil.THREE_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, IsisUtil.THREE_BYTES);
            this.setPrefix(IsisUtil.prefixConversion(tempByteArray));
        } else if (this.prefixLength >= 24 && this.prefixLength <= 32) {
            tempByteArray = new byte[IsisUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, IsisUtil.FOUR_BYTES);
            this.setPrefix(IsisUtil.prefixConversion(tempByteArray));
        }
        if (this.isSubTlvPresence()) {
            this.setSubTlvLength(channelBuffer.readByte());
            while (channelBuffer.readableBytes() > 0) {
                TlvHeader tlvHeader = new TlvHeader();
                tlvHeader.setTlvType(channelBuffer.readByte());
                tlvHeader.setTlvLength(channelBuffer.readByte());
                SubTlvType tlvValue = SubTlvType.get(tlvHeader.tlvType());
                if (tlvValue != null) {
                    TrafficEngineeringSubTlv subTlv =
                            SubTlvFinder.findSubTlv(tlvHeader, channelBuffer.readBytes(tlvHeader.tlvLength()));
                    if (subTlv != null) {
                        this.addSubTlv(subTlv);
                    }
                } else {
                    channelBuffer.readBytes(tlvHeader.tlvLength());
                }
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
     * Returns TLV body of IP external reachability TLV.
     *
     * @return byteArray TLV body of IP external reachability TLV.
     */
    private byte[] tlvBodyAsBytes() {
        List<Byte> bodyLst = new ArrayList<>();
        bodyLst.addAll(Bytes.asList(IsisUtil.convertToFourBytes(this.metric())));
        String controlInfo = "";
        if (this.isDown()) {
            controlInfo = controlInfo + "1";
        } else {
            controlInfo = controlInfo + "0";
        }
        if (this.isSubTlvPresence()) {
            controlInfo = controlInfo + "1";
        } else {
            controlInfo = controlInfo + "0";
        }
        String prefixLength = IsisUtil.toEightBitBinary(Integer.toBinaryString(this.prefixLength()));
        controlInfo = controlInfo + prefixLength.substring(2, prefixLength.length());
        bodyLst.add(Byte.parseByte(controlInfo, 2));
        if (this.isSubTlvPresence()) {
            bodyLst.add(this.subTlvLength());
            for (TrafficEngineeringSubTlv trafficEngineeringSubTlv : this.trafEnginSubTlv) {
                bodyLst.addAll(SubTlvToBytes.tlvToBytes(trafficEngineeringSubTlv));
            }
        }
        bodyLst.addAll(Bytes.asList(IsisUtil.prefixToBytes(this.prefix())));

        return Bytes.toArray(bodyLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("down", down)
                .add("subTlvPresence", subTlvPresence)
                .add("prefixLength", prefixLength)
                .add("metric", metric)
                .add("subTlvLength", subTlvLength)
                .add("prefix", prefix)
                .add("trafEnginSubTlv", trafEnginSubTlv)
                .toString();
    }
}