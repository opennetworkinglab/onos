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
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of LSP entry.
 */
public class LspEntry {

    private int lspSequenceNumber;
    private int lspChecksum;
    private int remainingTime;
    private String lspId;


    /**
     * Returns LSP sequence number of LSP entry.
     *
     * @return LSP sequence number
     */
    public int lspSequenceNumber() {
        return lspSequenceNumber;
    }

    /**
     * Sets LSP sequenceNumber for LSP entry.
     *
     * @param lspSequenceNumber lspSequenceNumber
     */
    public void setLspSequenceNumber(int lspSequenceNumber) {
        this.lspSequenceNumber = lspSequenceNumber;
    }

    /**
     * Returns LSP checksum of LSP entry.
     *
     * @return LSP checksum
     */
    public int lspChecksum() {
        return lspChecksum;
    }

    /**
     * Sets LSP checksum for LSP entry.
     *
     * @param lspChecksum LSP checksum
     */
    public void setLspChecksum(int lspChecksum) {
        this.lspChecksum = lspChecksum;
    }

    /**
     * Returns remaining time of LSP entry.
     *
     * @return remaining time
     */
    public int remainingTime() {
        return remainingTime;
    }

    /**
     * Sets remaining time for LSP entry.
     *
     * @param remainingTime remaining time
     */
    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    /**
     * Returns LSP ID of LSP entry.
     *
     * @return LSP ID
     */
    public String lspId() {
        return lspId;
    }

    /**
     * Sets LSP ID for LSp entry.
     *
     * @param lspId LSP ID
     */
    public void setLspId(String lspId) {
        this.lspId = lspId;
    }

    /**
     * Sets the LSP entry values for  LSP entry from byte buffer.
     *
     * @param channelBuffer channel Buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        this.setRemainingTime(channelBuffer.readUnsignedShort());
        byte[] tempByteArray = new byte[IsisUtil.ID_PLUS_TWO_BYTE];
        channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_PLUS_TWO_BYTE);
        this.setLspId(IsisUtil.systemIdPlus(tempByteArray));
        this.setLspSequenceNumber(channelBuffer.readInt());
        this.setLspChecksum(channelBuffer.readUnsignedShort());
    }

    /**
     * Returns LSP entry values as bytes of LSP entry.
     *
     * @return byteArray LSP entry values as bytes of LSP entry
     */
    public byte[] lspEntryAsBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.remainingTime())));
        bytes.addAll(IsisUtil.sourceAndLanIdToBytes(this.lspId()));
        bytes.addAll(Bytes.asList(IsisUtil.convertToFourBytes(this.lspSequenceNumber())));
        bytes.addAll(Bytes.asList(IsisUtil.convertToTwoBytes(this.lspChecksum())));
        return Bytes.toArray(bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("lspSequenceNumber", lspSequenceNumber)
                .add("lspChecksum", lspChecksum)
                .add("remainingTime", remainingTime)
                .add("lspId", lspId)
                .toString();
    }
}