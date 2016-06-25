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

/**
 * Representation of host name TLV.
 */
public class HostNameTlv extends TlvHeader {
    private String hostName;

    /**
     * Creates an instance of host name TLV.
     *
     * @param tlvHeader TLV header
     */
    public HostNameTlv(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());

    }

    /**
     * Returns host name of host name TLV.
     *
     * @return host name
     */
    public String hostName() {
        return hostName;
    }

    /**
     * Sets host name for host name TLV.
     *
     * @param hostName host name.
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        byte[] addressbytes = new byte[this.tlvLength()];
        channelBuffer.readBytes(addressbytes, 0, this.tlvLength());
        this.hostName = new String(addressbytes);
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
     * Returns TLV body of host name TLV.
     *
     * @return byteArray TLV body of host name TLV
     */
    private byte[] tlvBodyAsBytes() {
        byte[] bytes = this.hostName.getBytes();
        return bytes;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("hostName", hostName)
                .toString();
    }
}