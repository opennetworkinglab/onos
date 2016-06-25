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
import org.onosproject.ospf.exceptions.OspfErrorType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.TlvHeader;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * Representation of link id value of link tlv of Traffic Engineering.
 */
public class LinkId extends TlvHeader implements LinkSubType {
    private static final Logger log =
            LoggerFactory.getLogger(LinkId.class);
    private String linkId;

    /**
     * Creates an instance of link id.
     *
     * @param header tlv header instance
     */
    public LinkId(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Sets link type.
     *
     * @param linkType link type value
     */
    public void setLinkId(String linkType) {
        this.linkId = linkType;
    }

    /**
     * Reads bytes from channel buffer.
     *
     * @param channelBuffer channel buffer instance
     * @throws Exception might throws exception while parsing packet
     */
    public void readFrom(ChannelBuffer channelBuffer) throws Exception {
        try {
            byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
            this.setLinkId(InetAddress.getByAddress(tempByteArray).getHostName());
        } catch (Exception e) {
            log.debug("Error::LinkId:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR,
                                         OspfErrorType.BAD_MESSAGE);
        }
    }

    /**
     * Returns instance as byte array.
     *
     * @return instance as bytes
     * @throws Exception might throws exception while parsing packet
     */
    public byte[] asBytes() throws Exception {
        byte[] linkSubType = null;

        byte[] linkSubTlvHeader = getTlvHeaderAsByteArray();
        byte[] linkSubTlvBody = getLinkSubTypeTlvBodyAsByteArray();
        linkSubType = Bytes.concat(linkSubTlvHeader, linkSubTlvBody);

        return linkSubType;
    }

    /**
     * Gets byte array of link id sub tlv body.
     *
     * @return gets the body as byte array
     * @throws Exception might throws exception while parsing packet
     */
    public byte[] getLinkSubTypeTlvBodyAsByteArray() throws Exception {
        byte[] linkSubTypeBody = null;
        try {
            linkSubTypeBody = InetAddress.getByName(this.linkId).getAddress();
        } catch (Exception e) {
            log.debug("Error::LinkId:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR,
                                         OspfErrorType.BAD_MESSAGE);
        }
        return linkSubTypeBody;
    }

    /**
     * Returns this instance as string.
     *
     * @return this instance as string
     */
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("linkId", linkId)
                .toString();
    }
}