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
package org.onosproject.ospf.protocol.lsa.linksubtype;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.ospf.protocol.lsa.TlvHeader;

/**
 * Representation of an unknown or experimental TE value.
 */
public class UnknownLinkSubType extends TlvHeader implements LinkSubType {

    private byte[] value;

    /**
     * Creates an instance of this.
     *
     * @param header tlv header
     */
    public UnknownLinkSubType(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Gets the unknown subtype value .
     *
     * @return unknown subtype value
     */
    public byte[] value() {
        return value;
    }

    /**
     * Sets unknown subtype value.
     *
     * @param value unknown subtype value.
     */
    public void setValue(byte[] value) {
        this.value = value;
    }

    /**
     * Reads bytes from channel buffer .
     *
     * @param channelBuffer channel buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        byte[] tempByteArray = new byte[tlvLength()];
        channelBuffer.readBytes(tempByteArray, 0, tlvLength());
        this.setValue(tempByteArray);
    }

    /**
     * Returns instance as byte array.
     *
     * @return instance as byte array
     */
    public byte[] asBytes() {
        byte[] linkSubType = null;

        byte[] linkSubTlvHeader = getTlvHeaderAsByteArray();
        byte[] linkSubTlvBody = getLinkSubTypeTlvBodyAsByteArray();
        linkSubType = Bytes.concat(linkSubTlvHeader, linkSubTlvBody);
        return linkSubType;

    }

    /**
     * Gets instance body as byte array.
     *
     * @return instance body as byte array
     */
    public byte[] getLinkSubTypeTlvBodyAsByteArray() {
        return value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("value", value)
                .toString();
    }
}