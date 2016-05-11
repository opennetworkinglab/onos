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
 * Representation of LSP entries TLV.
 */
public class LspEntriesTlv extends TlvHeader implements IsisTlv {
    private List<LspEntry> lspEntryList = new ArrayList<>();

    /**
     * Creates an instance of LSP entries TLV.
     *
     * @param tlvHeader TLV header
     */
    public LspEntriesTlv(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    /**
     * Returns the LSP entry of LSP entries TLV.
     *
     * @return LSP entries
     */
    public List<LspEntry> lspEntry() {
        return lspEntryList;
    }

    /**
     * Adds the the LSP entry to LSP entries TLV.
     *
     * @param lspEntry LSP entry
     */
    public void addLspEntry(LspEntry lspEntry) {
        this.lspEntryList.add(lspEntry);
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        while (channelBuffer.readableBytes() >= (IsisUtil.EIGHT_BYTES * 2)) {
            LspEntry lspEntry = new LspEntry();
            lspEntry.readFrom(channelBuffer.readBytes(IsisUtil.EIGHT_BYTES * 2));
            lspEntryList.add(lspEntry);
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
     * Returns TLV body of LSP entries TLV.
     *
     * @return byteArray TLV body of LSP entries TLV
     */
    private byte[] tlvBodyAsBytes() {
        List<Byte> bytes = new ArrayList<>();
        for (LspEntry lspEntry : lspEntryList) {
            bytes.addAll(Bytes.asList(lspEntry.lspEntryAsBytes()));
        }
        return Bytes.toArray(bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("lspEntryList", lspEntryList)
                .toString();
    }
}
