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
 * Representation of partial sequence number PDU.
 */
public class Psnp extends IsisHeader {
    /*
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |             Intradomain Routing Protocol  Discriminator       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          Length Indicator                     |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                           Version/Protocol ID Extension       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                           ID Length                           |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |     R     |     R     |    R      |       PDU Type            |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          Version                              |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          Reserved                             |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                     Maximum area address                      |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          PDU Length                           |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                         Source ID                             |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                       Start LSP ID                            |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                        End LSP ID                             |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                      Variable Lengths Fields                  |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        Hello Message Format
        REFERENCE : ISO/IEC 10589
    */
    private int pduLength;
    private String sourceId;
    private List<IsisTlv> variableLengths = new ArrayList<>();

    /**
     * Creates the instance for this class.
     *
     * @param isisHeader ISIS header
     */
    public Psnp(IsisHeader isisHeader) {
        populateHeader(isisHeader);
    }

    /**
     * Adds the TLV to list.
     *
     * @param isisTlv ISIS TLV instance
     */
    public void addTlv(IsisTlv isisTlv) {
        variableLengths.add(isisTlv);
    }

    /**
     * Returns the list of all tlvs.
     *
     * @return variableLengths list of tlvs
     */
    public List<IsisTlv> getAllTlv() {
        return variableLengths;
    }

    /**
     * Returns the source ID of csnp.
     *
     * @return sourceId source ID
     */
    public String sourceId() {
        return sourceId;
    }

    /**
     * Sets the source ID for csnp.
     *
     * @param sourceId source ID
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * Returns the packet data unit length of link state packet.
     * Entire length of this PDU, in octets
     *
     * @return pduLength packte date unit length
     */
    public int pduLength() {
        return pduLength;
    }

    /**
     * Sets the packet data unit length for link state packet.
     * Entire Length of this PDU, in octets
     *
     * @param pduLength packte data length
     */
    public void setPduLength(int pduLength) {
        this.pduLength = pduLength;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        this.setPduLength(channelBuffer.readUnsignedShort());
        //source id + 2 value
        byte[] tempByteArray = new byte[IsisUtil.ID_PLUS_ONE_BYTE];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_PLUS_ONE_BYTE);
        this.setSourceId(IsisUtil.systemIdPlus(tempByteArray));
        //tlv here
        while (channelBuffer.readableBytes() > 0) {
            TlvHeader tlvHeader = new TlvHeader();
            tlvHeader.setTlvType(channelBuffer.readUnsignedByte());
            tlvHeader.setTlvLength(channelBuffer.readUnsignedByte());
            TlvType tlvValue = TlvType.get(tlvHeader.tlvType());
            if (tlvValue != null) {
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
        byte[] psnpMessage = null;
        byte[] isisPduHeader = isisPduHeader();
        byte[] psnpBody = partialSequenceNumberPduBody();
        psnpMessage = Bytes.concat(isisPduHeader, psnpBody);
        return psnpMessage;
    }

    /**
     * Builds the ISIS PDU header.
     *
     * @return headerList ISIS PDU header
     */
    public byte[] isisPduHeader() {
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
     * Builds the partial sequence number PDU body.
     *
     * @return bodyList partial sequence number PDU body
     */
    public byte[] partialSequenceNumberPduBody() {
        List<Byte> bodyList = new ArrayList<>();
        bodyList.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.pduLength())));
        bodyList.addAll(IsisUtil.sourceAndLanIdToBytes(this.sourceId()));
        for (IsisTlv isisTlv : variableLengths) {
            bodyList.addAll(TlvsToBytes.tlvToBytes(isisTlv));
        }
        return Bytes.toArray(bodyList);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("pduLength", pduLength)
                .add("sourceId", sourceId)
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
        Psnp that = (Psnp) o;
        return Objects.equal(pduLength, that.pduLength);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sourceId, pduLength);
    }
}