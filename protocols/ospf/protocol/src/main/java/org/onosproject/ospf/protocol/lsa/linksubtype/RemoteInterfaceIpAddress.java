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
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of remote interface ip address TE value.
 */
public class RemoteInterfaceIpAddress extends TlvHeader implements LinkSubType {
    private static final Logger log =
            LoggerFactory.getLogger(RemoteInterfaceIpAddress.class);
    private List<String> remoteInterfaceAddress = new ArrayList<>();

    /**
     * Creates an instance of remote interface ip address.
     *
     * @param header tlv header instance
     */
    public RemoteInterfaceIpAddress(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Adds remote interface ip address.
     *
     * @param remoteAddress ip address
     */
    public void addRemoteInterfaceAddress(String remoteAddress) {
        remoteInterfaceAddress.add(remoteAddress);
    }

    /**
     * Gets remote interface ip address.
     *
     * @return remoteAddress ip address
     */
    public List<String> getRemoteInterfaceAddress() {
        return remoteInterfaceAddress;
    }

    /**
     * Reads bytes from channel buffer .
     *
     * @param channelBuffer channel buffer instance
     * @throws Exception might throws exception while parsing packet
     */
    public void readFrom(ChannelBuffer channelBuffer) throws Exception {
        while (channelBuffer.readableBytes() >= OspfUtil.FOUR_BYTES) {
            try {
                byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
                channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
                this.addRemoteInterfaceAddress(InetAddress.getByAddress(tempByteArray).getHostName());
            } catch (Exception e) {
                log.debug("Error::RemoteInterfaceIPAddress:: {}", e.getMessage());
                throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR,
                                             OspfErrorType.BAD_MESSAGE);
            }
        }
    }

    /**
     * Gets byte array of remote interface ip address .
     *
     * @return byte array of remote interface ip address
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
     * Gets byte array of remote interface ip address.
     *
     * @return byte array of remote interface ip address
     * @throws Exception might throws exception while parsing packet
     */
    public byte[] getLinkSubTypeTlvBodyAsByteArray() throws Exception {
        List<Byte> linkSubTypeBody = new ArrayList<>();

        for (String remoteAddress : this.remoteInterfaceAddress) {
            try {
                linkSubTypeBody.addAll(Bytes.asList(InetAddress.getByName(remoteAddress).getAddress()));
            } catch (Exception e) {
                log.debug("Error::RemoteInterfaceIPAddress:: {}", e.getMessage());
                throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR,
                                             OspfErrorType.BAD_MESSAGE);
            }
        }

        return Bytes.toArray(linkSubTypeBody);
    }

    /**
     * Returns instance as string.
     *
     * @return instance as string
     */
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("RemoteInterfaceIPAddress", remoteInterfaceAddress)
                .toString();
    }

}
