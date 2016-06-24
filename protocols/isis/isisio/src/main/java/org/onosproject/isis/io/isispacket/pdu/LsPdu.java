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
 * Representation of an ISIS Link State packet.
 * Each Link State packet carries a collection of TLVs
 * Several TLVs may be included in a single packet.
 */
public class LsPdu extends IsisHeader {

    /*
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |             Intra-domain Routing Protocol  Discriminator      |
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
        |                       Remaining Lifetime                      |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          LSP ID                               |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                          PDU Length                           |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                        Sequence Number                        |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                        Checksum                               |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |   P     |   ATT   |     LSPDBOL         |       IS Type       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                      Variable Lengths Fields                  |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        LS PDU Format
        REFERENCE : ISO/IEC 10589
    */
    private int pduLength;
    private int remainingLifeTime;
    private String lspId;
    private int sequenceNumber;
    private int checkSum;
    private boolean partitionRepair;
    private AttachedToOtherAreas attachedToOtherAreas;
    private boolean lspDbol;
    private byte typeBlock;
    private byte intermediateSystemType;
    private List<IsisTlv> variableLengths = new ArrayList<>();

    /**
     * Creates an instance of Link State packet.
     *
     * @param isisHeader isis header details
     */
    public LsPdu(IsisHeader isisHeader) {
        populateHeader(isisHeader);
    }

    /**
     * Returns the ISIS tlvs.
     *
     * @return tlvs
     */
    public List<IsisTlv> tlvs() {
        return this.variableLengths;
    }

    /**
     * Adds the isis tlv to the list for the link state PDU.
     *
     * @param isisTlv isis tlv
     */
    public void addTlv(IsisTlv isisTlv) {
        variableLengths.add(isisTlv);
    }

    /**
     * Returns the remaining time of the link state pdu.
     * Number of seconds before LSP considered expired
     *
     * @return remainingTime remaining time
     */
    public int remainingLifeTime() {
        return remainingLifeTime;
    }

    /**
     * Sets the remaining time for the link state pdu.
     *
     * @param remainingLifeTime remaining time
     */
    public void setRemainingLifeTime(int remainingLifeTime) {
        this.remainingLifeTime = remainingLifeTime;
    }

    /**
     * Returns the link state database overload.
     *
     * @return lspdbol link state database overload
     */
    public boolean lspDbol() {
        return lspDbol;
    }

    /**
     * Sets the link state database overload for this pdu.
     *
     * @param lspDbol link state database overload
     */
    public void setLspDbol(boolean lspDbol) {
        this.lspDbol = lspDbol;
    }

    /**
     * Returns the type block.
     *
     * @return type block
     */
    public byte typeBlock() {
        return typeBlock;
    }

    /**
     * Sets the type block.
     *
     * @param typeBlock type block
     */
    public void setTypeBlock(byte typeBlock) {
        this.typeBlock = typeBlock;
    }

    /**
     * Returns the sequence number of LSP.
     *
     * @return sequenceNumber sequence number
     */
    public int sequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Sets the sequence nubmer for LSP.
     *
     * @param sequenceNumber sequence number
     */
    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * Returns the checksum of LSP from Source ID to end.
     *
     * @return checkSum check sum
     */
    public int checkSum() {
        return checkSum;
    }

    /**
     * Sets the checksum for LSP from Source ID to end.
     *
     * @param checkSum check sum
     */
    public void setCheckSum(int checkSum) {
        this.checkSum = checkSum;
    }

    /**
     * Returns the partition repair value of the intermediate system.
     *
     * @return partitionRepair partition repair
     */
    public boolean partitionRepair() {
        return partitionRepair;
    }

    /**
     * Sets partition repair value for the intermediate system.
     *
     * @param partitionRepair partition repair
     */
    public void setPartitionRepair(boolean partitionRepair) {
        this.partitionRepair = partitionRepair;
    }

    /**
     * Returns the value of intermediate system attached field.
     * return values based on type Default Metric, Delay Metric, Expense Metric, Error Metric
     *
     * @return attachedToOtherAreas attached to other areas
     */
    public AttachedToOtherAreas attachedToOtherAreas() {
        return attachedToOtherAreas;
    }

    /**
     * Sets the value for intermediate system attached field.
     * it will pass values based on type Default Metric, Delay Metric, Expense Metric, Error Metric
     *
     * @param attachedToOtherAreas attached to other areas
     */
    public void setAttachedToOtherAreas(AttachedToOtherAreas attachedToOtherAreas) {
        this.attachedToOtherAreas = attachedToOtherAreas;
    }

    /**
     * Returns the intermediate system type.
     * type will be level 1 or level 2
     *
     * @return intermediateSystemType intermediate system type
     */
    public byte intermediateSystemType() {
        return intermediateSystemType;
    }

    /**
     * Sets the value for intermediate system.
     * type will be level 1 or level 2
     *
     * @param intermediateSystemType intermediate system type
     */
    public void setIntermediateSystemType(byte intermediateSystemType) {
        this.intermediateSystemType = intermediateSystemType;
    }

    /**
     * Returns the link state ID of link state packet.
     * System ID of the source of link state PDU
     *
     * @return lspId link state packet ID
     */
    public String lspId() {
        return lspId;
    }

    /**
     * Sets the link state ID for link state packet.
     * System ID of the source of link state PDU
     *
     * @param lspId link state packet ID
     */
    public void setLspId(String lspId) {
        this.lspId = lspId;
    }

    /**
     * Returns the packet data unit length of link state packet.
     * Entire length of this PDU, in octets
     *
     * @return pduLength packet data unit length
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
        this.setRemainingLifeTime(channelBuffer.readUnsignedShort());
        //lsp id + 2 value
        byte[] tempByteArray = new byte[IsisUtil.ID_PLUS_TWO_BYTE];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_PLUS_TWO_BYTE);
        this.setLspId(IsisUtil.systemIdPlus(tempByteArray));
        //sequence number 4
        this.setSequenceNumber(channelBuffer.readInt());
        this.setCheckSum(channelBuffer.readUnsignedShort());
        int typeTemp = channelBuffer.readUnsignedByte();
        byte isTypeByte = (byte) typeTemp;
        String tempValue = String.format("%8s", Integer.toBinaryString(isTypeByte & 0xFF)).replace(' ', '0');
        int pBit = Integer.parseInt(new Character(tempValue.charAt(0)).toString());
        if (pBit == 1) {
            this.setPartitionRepair(true);
        } else {
            this.setPartitionRepair(false);
        }
        int attValue = Integer.parseInt(tempValue.substring(1, 5), 2);
        switch (AttachedToOtherAreas.get(attValue)) {
            case DEFAULTMETRIC:
                this.setAttachedToOtherAreas(AttachedToOtherAreas.DEFAULTMETRIC);
                break;
            case DELAYMETRIC:
                this.setAttachedToOtherAreas(AttachedToOtherAreas.DELAYMETRIC);
                break;
            case EXPENSEMETRIC:
                this.setAttachedToOtherAreas(AttachedToOtherAreas.EXPENSEMETRIC);
                break;
            case ERRORMETRIC:
                this.setAttachedToOtherAreas(AttachedToOtherAreas.ERRORMETRIC);
                break;
            case NONE:
                this.setAttachedToOtherAreas(AttachedToOtherAreas.NONE);
                break;
            default:
                break;
        }
        int lspdbol = Integer.parseInt(new Character(tempValue.charAt(5)).toString());
        if (lspdbol == 1) {
            this.setLspDbol(true);
        } else {
            this.setLspDbol(false);
        }
        int isType = Integer.parseInt(tempValue.substring(6, 8), 2);
        byte isTypeByteValue = (byte) isType;
        this.setIntermediateSystemType(isTypeByteValue);
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
        byte[] lspMessage = null;
        byte[] helloHeader = l1l2IsisPduHeader();
        byte[] lspBody = l1l2LsPduBody();
        lspMessage = Bytes.concat(helloHeader, lspBody);
        return lspMessage;
    }

    /**
     * Builds ISIS PDU header from ISIS message.
     *
     * @return headerList ISIS PDU header
     */
    public byte[] l1l2IsisPduHeader() {
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
     * Builds link state PDU body from ISIS message.
     *
     * @return bodyList link state PDU body
     */
    public byte[] l1l2LsPduBody() {
        List<Byte> bodyList = new ArrayList<>();
        bodyList.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.pduLength())));
        bodyList.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.remainingLifeTime())));
        bodyList.addAll(IsisUtil.sourceAndLanIdToBytes(this.lspId()));
        bodyList.addAll(Bytes.asList(IsisUtil.convertToFourBytes(this.sequenceNumber())));
        bodyList.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.checkSum())));
        String temString = "";
        if (this.partitionRepair()) {
            temString = "1" + temString;
        } else {
            temString = "0" + temString;
        }
        switch (this.attachedToOtherAreas()) {
            case ERRORMETRIC:
                temString = temString + "1000";
                break;
            case EXPENSEMETRIC:
                temString = temString + "0100";
                break;
            case DELAYMETRIC:
                temString = temString + "0010";
                break;
            case DEFAULTMETRIC:
                temString = temString + "0001";
                break;
            case NONE:
                temString = temString + "0000";
                break;
            default:
                break;
        }
        if (this.lspDbol()) {
            temString = temString + "1";
        } else {
            temString = temString + "0";
        }
        String isType = Integer.toBinaryString(this.intermediateSystemType());
        if (isType.length() % 2 != 0) {
            isType = "0" + isType;
        }
        temString = temString + isType;
        bodyList.add((byte) Integer.parseInt(temString, 2));
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
                .add("remainingLifeTime", remainingLifeTime)
                .add("lspId", lspId)
                .add("sequenceNumber", sequenceNumber)
                .add("checkSum", checkSum)
                .add("partitionRepair", partitionRepair)
                .add("lspDbol", lspDbol)
                .add("typeBlock", typeBlock)
                .add("intermediateSystemType", intermediateSystemType)
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
        LsPdu that = (LsPdu) o;
        return Objects.equal(pduLength, that.pduLength) &&
                Objects.equal(remainingLifeTime, that.remainingLifeTime) &&
                Objects.equal(lspId, that.lspId) &&
                Objects.equal(sequenceNumber, that.sequenceNumber) &&
                Objects.equal(checkSum, that.checkSum) &&
                Objects.equal(partitionRepair, that.partitionRepair) &&
                Objects.equal(lspDbol, that.lspDbol) &&
                Objects.equal(typeBlock, that.typeBlock) &&
                Objects.equal(intermediateSystemType, that.intermediateSystemType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pduLength, remainingLifeTime, lspId, sequenceNumber,
                                checkSum, partitionRepair, lspDbol, typeBlock, intermediateSystemType);
    }
}