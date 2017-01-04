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
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.exceptions.OspfErrorType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.onosproject.ospf.protocol.lsa.TlvHeader;
import org.onosproject.ospf.protocol.lsa.tlvtypes.LinkTlv;
import org.onosproject.ospf.protocol.lsa.tlvtypes.OpaqueTopLevelTlvTypes;
import org.onosproject.ospf.protocol.lsa.tlvtypes.RouterTlv;
import org.onosproject.ospf.protocol.util.OspfParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Representation of an Opaque LSA of type area local (10).
 */
public class OpaqueLsa10 extends OpaqueLsaHeader {
    /*
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |            LS age             |     Options   |   9, 10 or 11 |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |  Opaque Type  |               Opaque ID                       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                      Advertising Router                       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                      LS Sequence Number                       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |         LS checksum           |           Length              |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                                                               |
     +                                                               +
     |                      Opaque Information                       |
     +                                                               +
     |                              ...                              |

        Opaque LSA format
        REFERENCE : RFC 5250
    */
    private List<TopLevelTlv> topLevelValues = new ArrayList<>();
    private byte[] opaqueInfo = null;

    /**
     * Creates an instance of Opaque type 10 LSA.
     *
     * @param lsaHeader LSA header instance
     */
    public OpaqueLsa10(OpaqueLsaHeader lsaHeader) {
        populateHeader(lsaHeader);
    }

    /**
     * Returns the list of top level TLVs.
     *
     * @return list of top level TLVs
     */
    public List<TopLevelTlv> topLevelValues() {
        return topLevelValues;
    }

    /**
     * Adds TLV value.
     *
     * @param value TLV value
     */
    public void addValue(TopLevelTlv value) {
        topLevelValues.add(value);
    }

    /**
     * Reads from channel buffer and populate instance.
     *
     * @param channelBuffer channelBuffer instance
     * @throws OspfParseException might throws exception while parsing buffer
     */
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {

        try {
            if (this.opaqueId() == OspfParameters.TRAFFIC_ENGINEERING) {
                while (channelBuffer.readableBytes() > 0) {
                    TlvHeader tlvHeader = new TlvHeader();
                    tlvHeader.setTlvType(channelBuffer.readUnsignedShort());
                    tlvHeader.setTlvLength(channelBuffer.readUnsignedShort());
                    if (tlvHeader.tlvType() == OpaqueTopLevelTlvTypes.ROUTER.value()) {
                        RouterTlv routerTlv = new RouterTlv(tlvHeader);
                        routerTlv.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                        this.addValue(routerTlv);
                    } else if (tlvHeader.tlvType() == OpaqueTopLevelTlvTypes.LINK.value()) {
                        LinkTlv linkTlv = new LinkTlv(tlvHeader);
                        linkTlv.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                        this.addValue(linkTlv);
                    }
                }
            } else {
                int length = channelBuffer.readableBytes();
                opaqueInfo = new byte[length];
                channelBuffer.readBytes(opaqueInfo, 0, length);
            }

        } catch (Exception e) {
            log.debug("Error::OpaqueLsa10:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR,
                                         OspfErrorType.BAD_MESSAGE);
        }
    }

    /**
     * Returns instance as bytes.
     *
     * @return instance as bytes
     * @throws Exception might throws exception while parsing packet
     */
    public byte[] asBytes() throws Exception {

        byte[] lsaMessage = null;
        byte[] lsaHeader = getOpaqueLsaHeaderAsByteArray();
        byte[] lsaBody = getLsaBodyAsByteArray();
        lsaMessage = Bytes.concat(lsaHeader, lsaBody);
        return lsaMessage;

    }

    /**
     * Gets the LSA body as byte array.
     *
     * @return the lsa body as byte array
     * @throws Exception might throws exception while parsing packet
     */
    public byte[] getLsaBodyAsByteArray() throws Exception {
        List<Byte> bodyLst = new ArrayList<>();
        if (this.opaqueId() == 1) {
            for (TopLevelTlv tlv : this.topLevelValues) {
                //Check the sub type of lsa and build bytes accordingly
                if (tlv instanceof RouterTlv) {
                    RouterTlv routerTlv = (RouterTlv) tlv;
                    bodyLst.addAll(Bytes.asList(routerTlv.asBytes()));
                } else if (tlv instanceof LinkTlv) {
                    LinkTlv linkTlv = (LinkTlv) tlv;
                    bodyLst.addAll(Bytes.asList(linkTlv.asBytes()));
                }
            }
        } else {
            return opaqueInfo;
        }

        return Bytes.toArray(bodyLst);
    }

    @Override
    public OspfLsaType getOspfLsaType() {
        return OspfLsaType.AREA_LOCAL_OPAQUE_LSA;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("topLevelValues", topLevelValues)
                .add("opaqueInfo", opaqueInfo)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpaqueLsa10 that = (OpaqueLsa10) o;
        return Objects.equal(topLevelValues, that.topLevelValues) &&
                Arrays.equals(opaqueInfo, that.opaqueInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Arrays.hashCode(opaqueInfo), topLevelValues);
    }
}