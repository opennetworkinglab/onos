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
 * Representation of complete sequence number PDU.
 */
public class Csnp extends IsisHeader {

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

        CSNP Message Format
        REFERENCE : ISO/IEC 10589
    */
    private int pduLength;
    private String sourceId;
    private String startLspId;
    private String endLspId;
    private List<IsisTlv> variableLengths = new ArrayList<>();

    /**
     * Creates the instance for this class.
     *
     * @param isisHeader ISIS header
     */
    public Csnp(IsisHeader isisHeader) {
        populateHeader(isisHeader);
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
     * Returns the initial link state packet ID of csnp.
     *
     * @return startLspId start link state packet ID
     */
    public String startLspId() {
        return startLspId;
    }

    /**
     * Sets the initial link state packet ID for csnp.
     *
     * @param startLspId start link state packet ID
     */
    public void setStartLspId(String startLspId) {
        this.startLspId = startLspId;
    }

    /**
     * Returns the end link state packet ID of csnp.
     *
     * @return endLspId end link state packet ID of csnp.
     */
    public String endLspId() {
        return endLspId;
    }

    /**
     * Sets the end link state packet ID for csnp.
     *
     * @param endLspId end link state packet ID of csnp.
     */
    public void setEndLspId(String endLspId) {
        this.endLspId = endLspId;
    }

    /**
     * Returns the packet data unit length of link state packet.
     * Entire length of this PDU, in octets
     *
     * @return pduLength packet date unit length
     */
    public int pduLength() {
        return pduLength;
    }

    /**
     * Sets the packet data unit length for link state packet.
     * Entire Length of this PDU, in octets
     *
     * @param pduLength packet data length
     */
    public void setPduLength(int pduLength) {
        this.pduLength = pduLength;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        this.setPduLength(channelBuffer.readUnsignedShort());
        //source id + 1 value
        byte[] tempByteArray = new byte[IsisUtil.ID_PLUS_ONE_BYTE];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_PLUS_ONE_BYTE);
        this.setSourceId(IsisUtil.systemIdPlus(tempByteArray));
        //start lsp id + 2 value
        tempByteArray = new byte[IsisUtil.ID_PLUS_TWO_BYTE];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_PLUS_TWO_BYTE);
        this.setStartLspId(IsisUtil.systemIdPlus(tempByteArray));
        //end lsp id + 2 value
        tempByteArray = new byte[IsisUtil.ID_PLUS_TWO_BYTE];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_PLUS_TWO_BYTE);
        this.setEndLspId(IsisUtil.systemIdPlus(tempByteArray));
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
        byte[] csnpMessage = null;
        byte[] isisPduHeader = isisPduHeader();
        byte[] csnpBody = completeSequenceNumberPduBody();
        csnpMessage = Bytes.concat(isisPduHeader, csnpBody);
        return csnpMessage;
    }

    /**
     * Builds ISIS PDU header for complete sequence numbers PDU.
     *
     * @return isisPduHeader ISIS PDU header
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
     * Builds complete sequence numbers PDU body.
     *
     * @return bodyList complete sequence numbers PDU body
     */
    public byte[] completeSequenceNumberPduBody() {
        List<Byte> bodyList = new ArrayList<>();
        bodyList.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.pduLength())));
        bodyList.addAll(IsisUtil.sourceAndLanIdToBytes(this.sourceId()));
        bodyList.addAll(IsisUtil.sourceAndLanIdToBytes(this.startLspId()));
        bodyList.addAll(IsisUtil.sourceAndLanIdToBytes(this.endLspId()));
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
                .add("startLspId", startLspId)
                .add("endLspId", endLspId)
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
        Csnp that = (Csnp) o;
        return Objects.equal(pduLength, that.pduLength) &&
                Objects.equal(sourceId, that.sourceId) &&
                Objects.equal(startLspId, that.startLspId) &&
                Objects.equal(endLspId, that.endLspId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pduLength, sourceId, startLspId, endLspId);
    }
}