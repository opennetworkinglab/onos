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
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.exceptions.OspfErrorType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.subtypes.OspfExternalDestination;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of an External LSA and the fields and methods to access them.
 */
public class ExternalLsa extends LsaHeader {

    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |            LS age             |     Options   |      5        |
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
        |E|     0       |                  metric                       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                      Forwarding address                       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                      External Route Tag                       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |E|    TOS      |                TOS  metric                    |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                      Forwarding address                       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                      External Route Tag                       |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |                              ...                              |

        External LSA format
        REFERENCE : RFC 2328
     */
    private static final Logger log =
            LoggerFactory.getLogger(ExternalLsa.class);
    private Ip4Address networkMask;
    private List<OspfExternalDestination> externalDestinations = new ArrayList<>();

    /**
     * Creates an instance of External LSA.
     *
     * @param lsaHeader lsa header instance.
     */
    public ExternalLsa(LsaHeader lsaHeader) {
        populateHeader(lsaHeader);
    }


    /**
     * Gets the network mask.
     *
     * @return networkMask
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
     * Adds the external destination details to the list.
     *
     * @param externalDestination external destination details
     */
    public void addExternalDestination(OspfExternalDestination externalDestination) {
        if (!externalDestinations.contains(externalDestination)) {
            externalDestinations.add(externalDestination);
        }
    }

    /**
     * Reads from channel buffer and populate instance.
     *
     * @param channelBuffer channelBuffer instance
     * @throws OspfParseException might throws exception while parsing buffer
     */
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {

        try {
            byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
            this.setNetworkMask(Ip4Address.valueOf(tempByteArray));

            while (channelBuffer.readableBytes() >= OspfUtil.EXTERNAL_DESTINATION_LENGTH) {
                OspfExternalDestination externalDestination = new OspfExternalDestination();

                //E Bit - use to find type1 or type2
                int eIsSet = channelBuffer.readByte();
                if (eIsSet != 0) {
                    externalDestination.setType1orType2Metric(true);
                } else {
                    externalDestination.setType1orType2Metric(false);
                }
                externalDestination.setMetric(channelBuffer.readUnsignedMedium());
                tempByteArray = new byte[OspfUtil.FOUR_BYTES];
                channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
                externalDestination.setForwardingAddress(Ip4Address.valueOf(tempByteArray));
                externalDestination.setExternalRouterTag(channelBuffer.readInt());

                this.addExternalDestination(externalDestination);
            }
        } catch (Exception e) {
            log.debug("Error::ExternalLSA:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR, OspfErrorType.BAD_MESSAGE);
        }
    }

    /**
     * Gets LSA as bytes.
     *
     * @return LSA as bytes.
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
     * @return byte array contains LSA body
     * @throws OspfParseException might throws exception while parsing buffer
     */
    public byte[] getLsaBodyAsByteArray() throws OspfParseException {
        List<Byte> bodyLst = new ArrayList<>();

        try {
            bodyLst.addAll(Bytes.asList(this.networkMask().toOctets()));

            //add each OSPFExternalDestination details
            for (OspfExternalDestination externalDest : externalDestinations) {
                if (externalDest.isType1orType2Metric()) {
                    //add 1 followed by 7 zeros equals to decimal 128
                    bodyLst.add((byte) 128);
                } else {
                    bodyLst.add((byte) 0);
                }

                bodyLst.addAll(Bytes.asList(OspfUtil.convertToThreeBytes(externalDest.metric())));
                bodyLst.addAll(Bytes.asList(externalDest.forwardingAddress().toOctets()));
                bodyLst.addAll(Bytes.asList(OspfUtil.convertToFourBytes(externalDest.externalRouterTag())));
            }
        } catch (Exception e) {
            log.debug("Error::getLsrBodyAsByteArray {}", e.getMessage());
            return Bytes.toArray(bodyLst);
        }

        return Bytes.toArray(bodyLst);
    }

    @Override
    public OspfLsaType getOspfLsaType() {
        return OspfLsaType.EXTERNAL_LSA;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        ExternalLsa that = (ExternalLsa) other;
        return Objects.equal(networkMask, that.networkMask) &&
                Objects.equal(externalDestinations, that.externalDestinations);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkMask, externalDestinations);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("networkMask", networkMask)
                .add("externalDestinations", externalDestinations)
                .toString();
    }
}