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
 * Representation of P2P hello.
 */
public class P2PHelloPdu extends HelloPdu {
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
        |                        Local Circuit Id                       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                      Variable Lengths Fields                  |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        P2P Hello Message Format
        REFERENCE : ISO/IEC 10589
    */
    private byte localCircuitId;

    /**
     * Sets the ISIS header.
     *
     * @param isisHeader isisHeader
     */
    public P2PHelloPdu(IsisHeader isisHeader) {
        populateHeader(isisHeader);
    }

    /**
     * Returns the local circuit ID.
     *
     * @return Local circuit ID
     */
    public byte localCircuitId() {
        return localCircuitId;
    }

    /**
     * Sets the local circuit ID.
     *
     * @param localCircuitId Local circuit ID
     */
    public void setLocalCircuitId(byte localCircuitId) {
        this.localCircuitId = localCircuitId;
    }

    /**
     * Sets the variable lengths.
     *
     * @param variableLengths variable lengths.
     */
    public void setVariableLengths(List<IsisTlv> variableLengths) {
        this.variableLengths = variableLengths;
    }


    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        this.setCircuitType(channelBuffer.readByte());
        //source id
        byte[] tempByteArray = new byte[IsisUtil.ID_SIX_BYTES];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_SIX_BYTES);
        this.setSourceId(IsisUtil.systemId(tempByteArray));
        this.setHoldingTime(channelBuffer.readUnsignedShort());
        this.setPduLength(channelBuffer.readUnsignedShort());
        this.setLocalCircuitId((byte) channelBuffer.readUnsignedByte());
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
        byte[] helloHeader = p2PHeader();
        byte[] helloBody = p2P2HelloPduBody();
        helloMessage = Bytes.concat(helloHeader, helloBody);
        return helloMessage;
    }

    /**
     * Builds the point to point header.
     *
     * @return headerList point to point header
     */
    public byte[] p2PHeader() {
        List<Byte> headerList = new ArrayList<>();
        headerList.add(this.irpDiscriminator());
        headerList.add((byte) IsisUtil.getPduHeaderLength(this.pduType()));
        headerList.add(this.version());
        headerList.add(this.idLength());
        headerList.add((byte) this.pduType());
        headerList.add(this.version2());
        headerList.add(this.reserved());
        headerList.add(this.maximumAreaAddresses());
        return Bytes.toArray(headerList);
    }

    /**
     * Builds the point to point hello PDU body.
     *
     * @return bodyList point to point hello PDU body
     */
    public byte[] p2P2HelloPduBody() {
        List<Byte> bodyList = new ArrayList<>();
        bodyList.add(this.circuitType());
        bodyList.addAll(IsisUtil.sourceAndLanIdToBytes(this.sourceId()));
        bodyList.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.holdingTime())));
        bodyList.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.pduLength())));
        bodyList.add((byte) this.localCircuitId());
        for (IsisTlv isisTlv : variableLengths) {
            bodyList.addAll(TlvsToBytes.tlvToBytes(isisTlv));
        }
        return Bytes.toArray(bodyList);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("localCircuitId", localCircuitId)
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
        P2PHelloPdu that = (P2PHelloPdu) o;
        return Objects.equal(localCircuitId, that.localCircuitId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(localCircuitId);
    }
}