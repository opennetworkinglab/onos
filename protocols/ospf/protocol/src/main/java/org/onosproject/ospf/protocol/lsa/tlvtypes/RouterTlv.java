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
package org.onosproject.ospf.protocol.lsa.tlvtypes;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.exceptions.OspfErrorType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.TlvHeader;
import org.onosproject.ospf.protocol.lsa.types.TopLevelTlv;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of an OSPF Opaque router tlv.
 */
public class RouterTlv extends TlvHeader implements TopLevelTlv {

    private static final Logger log =
            LoggerFactory.getLogger(RouterTlv.class);
    private Ip4Address routerAddress;

    /**
     * Creates an instance of Opaque router tlv.
     *
     * @param header tlv header
     */
    public RouterTlv(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Gets router address.
     *
     * @return router address
     */
    public Ip4Address routerAddress() {
        return routerAddress;
    }

    /**
     * Sets router address.
     *
     * @param routerAddress router address.
     */
    public void setRouterAddress(Ip4Address routerAddress) {
        this.routerAddress = routerAddress;
    }

    /**
     * Reads bytes from channel buffer .
     *
     * @param channelBuffer channel buffer instance
     * @throws Exception might throws exception while parsing buffer
     */
    public void readFrom(ChannelBuffer channelBuffer) throws Exception {
        try {
            byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
            this.setRouterAddress(Ip4Address.valueOf(tempByteArray));
        } catch (Exception e) {
            log.debug("Error::RouterTLV:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR,
                                         OspfErrorType.BAD_MESSAGE);
        }
    }

    /**
     * Gets router tlv as byte array.
     *
     * @return router tlv as byte array
     */
    public byte[] asBytes() {
        byte[] lsaMessage = null;

        byte[] tlvHeader = getTlvHeaderAsByteArray();
        byte[] tlvBody = getTlvBodyAsByteArray();
        lsaMessage = Bytes.concat(tlvHeader, tlvBody);

        return lsaMessage;
    }

    /**
     * Gets tlv body as byte array.
     *
     * @return tlv body as byte array
     */
    public byte[] getTlvBodyAsByteArray() {
        List<Byte> bodyLst = new ArrayList<>();
        bodyLst.addAll(Bytes.asList(this.routerAddress().toOctets()));

        return Bytes.toArray(bodyLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("routerAddress", routerAddress)
                .toString();
    }
}


