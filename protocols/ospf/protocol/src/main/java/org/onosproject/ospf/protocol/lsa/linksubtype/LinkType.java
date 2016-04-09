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

/**
 * Representation of link type TE value.
 */
public class LinkType extends TlvHeader implements LinkSubType {
    private static final Logger log =
            LoggerFactory.getLogger(LinkType.class);
    private int linkType;

    /**
     * Creates link type instance .
     */
    public LinkType() {

    }

    /**
     * Creates link type instance.
     *
     * @param header tlv header instance
     */
    public LinkType(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Sets link type.
     *
     * @param linkType value of link type
     */
    public void setLinkType(int linkType) {
        this.linkType = linkType;
    }

    /**
     * Reads from channel buffer.
     *
     * @param channelBuffer channel buffer instance
     * @throws Exception might throws exception while parsing buffer
     */
    public void readFrom(ChannelBuffer channelBuffer) throws Exception {
        try {
            int len = channelBuffer.readableBytes();
            byte[] tempByteArray = new byte[len];
            channelBuffer.readBytes(tempByteArray, 0, len);
            this.setLinkType(OspfUtil.byteToInteger(tempByteArray));
        } catch (Exception e) {
            log.debug("Error::LinkType:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR,
                                         OspfErrorType.BAD_MESSAGE);
        }
    }

    /**
     * Gets link subtype as byte array.
     *
     * @return byte array of link subtype
     */
    public byte[] asBytes() {
        byte[] linkSubType = null;

        byte[] linkSubTlvHeader = getTlvHeaderAsByteArray();
        byte[] linkSubTlvBody = getLinkSubTypeTlvBodyAsByteArray();
        linkSubType = Bytes.concat(linkSubTlvHeader, linkSubTlvBody);
        return linkSubType;
    }

    /**
     * Gets link subtype as bytes.
     *
     * @return byte array of link subtype
     */
    public byte[] getLinkSubTypeTlvBodyAsByteArray() {
        byte[] linkSubTypeBody = new byte[4];
        linkSubTypeBody[0] = (byte) this.linkType;
        linkSubTypeBody[1] = 0;
        linkSubTypeBody[2] = 0;
        linkSubTypeBody[3] = 0;
        return linkSubTypeBody;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("linkType", linkType)
                .toString();
    }
}