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
package org.onosproject.ospf.protocol.lsa.types;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.exceptions.OspfErrorType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a Network LSA and fields and methods to access them.
 */
public class NetworkLsa extends LsaHeader {

    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |            LS age             |      Options  |      2        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                        Link State ID                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     Advertising Router                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     LS sequence number                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |         LS checksum           |             length            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         Network Mask                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                        Attached Router                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                              ...                              |
     */
    private static final Logger log =
            LoggerFactory.getLogger(NetworkLsa.class);
    private Ip4Address networkMask;
    private List<Ip4Address> attachedRouters = new ArrayList<>();

    /**
     * Creates an instance of Network LSA.
     */
    public NetworkLsa() {
    }

    /**
     * Creates an instance of Network LSA.
     *
     * @param lsaHeader lsa header instance.
     */
    public NetworkLsa(LsaHeader lsaHeader) {
        populateHeader(lsaHeader);
    }

    /**
     * Gets network mask.
     *
     * @return network mask
     */
    public Ip4Address networkMask() {
        return networkMask;
    }

    /**
     * Sets network mask.
     *
     * @param networkMask network mask
     */
    public void setNetworkMask(Ip4Address networkMask) {
        this.networkMask = networkMask;
    }

    /**
     * Adds attached router to list.
     *
     * @param attachedRouter attached router ip address.
     */
    public void addAttachedRouter(Ip4Address attachedRouter) {
        attachedRouters.add(attachedRouter);
    }

    /**
     * Gets the list of attached routers.
     *
     * @return list of attached routers
     */
    public List<Ip4Address> attachedRouters() {

        return attachedRouters;
    }

    /**
     * Reads from channel buffer and populate instance.
     *
     * @param channelBuffer channel buffer instance
     * @throws OspfParseException might throws exception while parsing buffer
     */
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {

        try {
            byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
            this.setNetworkMask(Ip4Address.valueOf(tempByteArray));
            //add all the attached routers
            while (channelBuffer.readableBytes() > 0) {
                tempByteArray = new byte[OspfUtil.FOUR_BYTES];
                channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
                this.addAttachedRouter(Ip4Address.valueOf(tempByteArray));
            }
        } catch (Exception e) {
            log.debug("Error::NetworkLSA::readFrom:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR, OspfErrorType.BAD_MESSAGE);
        }
    }

    /**
     * Gets LSA as bytes.
     *
     * @return LSA as byte array
     * @throws OspfParseException might throws exception while parsing packet
     */
    public byte[] asBytes() throws OspfParseException {
        byte[] lsaMessage = null;

        byte[] lsaHeader = getLsaHeaderAsByteArray();
        byte[] lsaBody = getLsaBodyAsByteArray();
        lsaMessage = Bytes.concat(lsaHeader, lsaBody);

        return lsaMessage;
    }

    /**
     * Gets LSA body as byte array.
     *
     * @return LSA body as byte array
     * @throws OspfParseException might throws exception while parsing packet
     */
    public byte[] getLsaBodyAsByteArray() throws OspfParseException {
        List<Byte> bodyLst = new ArrayList<>();

        try {
            bodyLst.addAll(Bytes.asList(this.networkMask().toOctets()));
            //add each attachedRouters details
            for (Ip4Address attachedRouter : attachedRouters) {
                //attached router
                bodyLst.addAll(Bytes.asList(attachedRouter.toOctets()));
            }
        } catch (Exception e) {
            log.debug("Error::NetworkLSA::getLsrBodyAsByteArray {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR, OspfErrorType.BAD_MESSAGE);
        }

        return Bytes.toArray(bodyLst);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkLsa that = (NetworkLsa) o;
        return Objects.equal(networkMask, that.networkMask) &&
                Objects.equal(attachedRouters, that.attachedRouters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkMask, attachedRouters);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("networkMask", networkMask)
                .add("attachedRouters", attachedRouters)
                .toString();
    }
}