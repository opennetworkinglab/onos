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
package org.onosproject.isis.io.isispacket.pdu;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.tlv.IsisTlv;
import org.onosproject.isis.io.isispacket.tlv.TlvFinder;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;
import org.onosproject.isis.io.isispacket.tlv.TlvType;
import org.onosproject.isis.io.isispacket.tlv.TlvsToBytes;
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of L1L2 hello PDU.
 */
public class L1L2HelloPdu extends HelloPdu {

    /*
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |             Intra-domain Routing Protocol  Discriminator       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          Length Indicator                     |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                           Version/Protocol ID Extension       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                           ID Length                           |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |     R     |     R     |    R      |       PDU Type            |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                             Version                           |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                             Reserved                          |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                     Maximum area address                      |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          Circuit Type                         |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          Source ID                            |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          Holding Time                         |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          PDU Length                           |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          PDU Length                           |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |    R     |                   Priority                         |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                             LAN ID                            |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                      Variable Lengths Fields                  |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        Hello Message Format
        REFERENCE : ISO/IEC 10589
    */

    private byte priority;
    private String lanId;

    /**
     * Parametrized constructor.
     *
     * @param isisHeader ISIs header
     */
    public L1L2HelloPdu(IsisHeader isisHeader) {
        populateHeader(isisHeader);
    }

    /**
     * Returns the LAN ID.
     *
     * @return LAN ID
     */

    public String lanId() {
        return lanId;
    }

    /**
     * Sets the LAN ID.
     *
     * @param lanId LAN ID
     */
    public void setLanId(String lanId) {
        this.lanId = lanId;
    }

    /**
     * Returns the priority.
     *
     * @return priority
     */
    public byte priority() {
        return priority;
    }

    /**
     * Sets priority.
     *
     * @param priority priority
     */
    public void setPriority(byte priority) {
        this.priority = priority;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        this.setCircuitType(channelBuffer.readByte());
        //sorce id
        byte[] tempByteArray = new byte[IsisUtil.ID_SIX_BYTES];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_SIX_BYTES);
        this.setSourceId(IsisUtil.systemId(tempByteArray));
        this.setHoldingTime(channelBuffer.readUnsignedShort());
        this.setPduLength(channelBuffer.readUnsignedShort());
        this.setPriority(channelBuffer.readByte());
        //landid  id + 1 value
        tempByteArray = new byte[IsisUtil.ID_PLUS_ONE_BYTE];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_PLUS_ONE_BYTE);
        this.setLanId(IsisUtil.systemIdPlus(tempByteArray));
        //tlv here
        while (channelBuffer.readableBytes() > 0) {
            TlvHeader tlvHeader = new TlvHeader();
            tlvHeader.setTlvType(channelBuffer.readUnsignedByte());
            tlvHeader.setTlvLength(channelBuffer.readUnsignedByte());
            TlvType tlvType = TlvType.get(tlvHeader.tlvType());
            if (tlvType != null) {
                IsisTlv tlv = TlvFinder.findTlv(tlvHeader, channelBuffer.readBytes(tlvHeader.tlvLength()));
                if (tlv != null) {
                    this.variableLengths.add(tlv);
                }
            } else {
                channelBuffer.readBytes(tlvHeader.tlvLength());
            }
        }
    }

    @Override
    public byte[] asBytes() {
        byte[] helloMessage = null;
        byte[] helloHeader = l1l2IsisPduHeader();
        byte[] helloBody = l1l2HelloPduBody();
        helloMessage = Bytes.concat(helloHeader, helloBody);
        return helloMessage;
    }

    /**
     * Parse the ISIS L1L2 PDU header.
     *
     * @return ISIS L1L2 PDU header
     */
    public byte[] l1l2IsisPduHeader() {
        List<Byte> headerLst = new ArrayList<>();
        headerLst.add(this.irpDiscriminator());
        headerLst.add((byte) IsisUtil.getPduHeaderLength(this.pduType()));
        headerLst.add(this.version());
        headerLst.add(this.idLength());
        headerLst.add((byte) this.pduType());
        headerLst.add(this.version2());
        headerLst.add(this.reserved());
        headerLst.add(this.maximumAreaAddresses());
        return Bytes.toArray(headerLst);
    }

    /**
     * Parse the ISIS L1L2 PDU body.
     *
     * @return ISIS L1L2 PDU body
     */
    public byte[] l1l2HelloPduBody() {
        List<Byte> bodyLst = new ArrayList<>();

        bodyLst.add(this.circuitType());
        bodyLst.addAll(IsisUtil.sourceAndLanIdToBytes(this.sourceId()));
        bodyLst.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.holdingTime())));
        bodyLst.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.pduLength())));
        bodyLst.add(this.priority);
        bodyLst.addAll(IsisUtil.sourceAndLanIdToBytes(this.lanId()));
        for (IsisTlv isisTlv : variableLengths) {
            bodyLst.addAll(TlvsToBytes.tlvToBytes(isisTlv));
        }
        return Bytes.toArray(bodyLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("priority", priority)
                .add("lanId", lanId)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        L1L2HelloPdu that = (L1L2HelloPdu) o;
        return Objects.equal(priority, that.priority) &&
                Objects.equal(lanId, that.lanId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(priority, lanId);
    }
}