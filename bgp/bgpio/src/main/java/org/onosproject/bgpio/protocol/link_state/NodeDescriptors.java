/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.bgpio.protocol.link_state;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.types.AreaIDTlv;
import org.onosproject.bgpio.types.AutonomousSystemTlv;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.BGPLSIdentifierTlv;
import org.onosproject.bgpio.types.BGPValueType;
import org.onosproject.bgpio.types.IsIsNonPseudonode;
import org.onosproject.bgpio.types.IsIsPseudonode;
import org.onosproject.bgpio.types.OSPFNonPseudonode;
import org.onosproject.bgpio.types.OSPFPseudonode;
import org.onosproject.bgpio.util.UnSupportedAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Local and Remote NodeDescriptors which contains Node Descriptor Sub-TLVs.
 */
public class NodeDescriptors {

    /*
     *Reference :draft-ietf-idr-ls-distribution-11
          0                   1                   2                   3
          0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         |              Type             |             Length            |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         |                                                               |
         //              Node Descriptor Sub-TLVs (variable)            //
         |                                                               |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                   Figure : Local or Remote Node Descriptors TLV format
     */

    protected static final Logger log = LoggerFactory.getLogger(NodeDescriptors.class);

    public static final short LOCAL_NODE_DES_TYPE = 256;
    public static final short REMOTE_NODE_DES_TYPE = 257;
    public static final short IGP_ROUTERID_TYPE = 515;
    public static final short IS_IS_LEVEL_1_PROTOCOL_ID = 1;
    public static final short IS_IS_LEVEL_2_PROTOCOL_ID = 2;
    public static final short OSPF_V2_PROTOCOL_ID = 3;
    public static final short OSPF_V3_PROTOCOL_ID = 6;
    public static final int TYPE_AND_LEN = 4;
    public static final int ISISNONPSEUDONODE_LEN = 6;
    public static final int ISISPSEUDONODE_LEN = 7;
    public static final int OSPFNONPSEUDONODE_LEN = 4;
    public static final int OSPFPSEUDONODE_LEN = 8;
    private LinkedList<BGPValueType> subTlvs;
    private short deslength;
    private short desType;

    /**
     * Resets parameters.
     */
    public NodeDescriptors() {
        this.subTlvs = null;
        this.deslength = 0;
        this.desType = 0;
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param subTlvs list of subTlvs
     * @param deslength Descriptors length
     * @param desType local node descriptor or remote node descriptor type
     */
    public NodeDescriptors(LinkedList<BGPValueType> subTlvs, short deslength, short desType) {
        this.subTlvs = subTlvs;
        this.deslength = deslength;
        this.desType = desType;
    }

    /**
     * Returns list of subTlvs.
     *
     * @return subTlvs list of subTlvs
     */
    public LinkedList<BGPValueType> getSubTlvs() {
        return subTlvs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subTlvs.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof NodeDescriptors) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            NodeDescriptors other = (NodeDescriptors) obj;
            Iterator<BGPValueType> objListIterator = other.subTlvs.iterator();
            countOtherSubTlv = other.subTlvs.size();
            countObjSubTlv = subTlvs.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    BGPValueType subTlv = objListIterator.next();
                    isCommonSubTlv = Objects.equals(subTlvs.contains(subTlv), other.subTlvs.contains(subTlv));
                }
                return isCommonSubTlv;
            }
        }
        return false;
    }

    /**
     * Reads node descriptors Sub-TLVs.
     *
     * @param cb ChannelBuffer
     * @param desLength node descriptor length
     * @param desType local node descriptor or remote node descriptor type
     * @param protocolId protocol ID
     * @return object of NodeDescriptors
     * @throws BGPParseException while parsing node descriptors
     */
    public static NodeDescriptors read(ChannelBuffer cb, short desLength, short desType, byte protocolId)
            throws BGPParseException {
        LinkedList<BGPValueType> subTlvs;
        subTlvs = new LinkedList<>();
        BGPValueType tlv = null;

        while (cb.readableBytes() > 0) {
            ChannelBuffer tempBuf = cb;
            short type = cb.readShort();
            short length = cb.readShort();
            if (cb.readableBytes() < length) {
                throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR, BGPErrorType.OPTIONAL_ATTRIBUTE_ERROR,
                        tempBuf.readBytes(cb.readableBytes() + TYPE_AND_LEN));
            }
            ChannelBuffer tempCb = cb.readBytes(length);
            switch (type) {
            case AutonomousSystemTlv.TYPE:
                tlv = AutonomousSystemTlv.read(tempCb);
                break;
            case BGPLSIdentifierTlv.TYPE:
                tlv = BGPLSIdentifierTlv.read(tempCb);
                break;
            case AreaIDTlv.TYPE:
                tlv = AreaIDTlv.read(tempCb);
                break;
            case IGP_ROUTERID_TYPE:
                if (protocolId == IS_IS_LEVEL_1_PROTOCOL_ID || protocolId == IS_IS_LEVEL_2_PROTOCOL_ID) {
                    if (length == ISISNONPSEUDONODE_LEN) {
                        tlv = IsIsNonPseudonode.read(tempCb);
                    } else if (length == ISISPSEUDONODE_LEN) {
                        tlv = IsIsPseudonode.read(tempCb);
                    }
                } else if (protocolId == OSPF_V2_PROTOCOL_ID || protocolId == OSPF_V3_PROTOCOL_ID) {
                    if (length == OSPFNONPSEUDONODE_LEN) {
                        tlv = OSPFNonPseudonode.read(tempCb);
                    } else if (length == OSPFPSEUDONODE_LEN) {
                        tlv = OSPFPseudonode.read(tempCb);
                    }
                }
                break;
            default:
                UnSupportedAttribute.skipBytes(tempCb, length);
            }
            subTlvs.add(tlv);
        }
        return new NodeDescriptors(subTlvs, desLength, desType);
    }

    /**
     * Returns node descriptors length.
     *
     * @return node descriptors length
     */
    public short getLength() {
        return this.deslength;
    }

    /**
     * Returns node descriptors type.
     *
     * @return node descriptors type
     */
    public short getType() {
        return this.desType;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("desType", desType)
                .add("deslength", deslength)
                .add("subTlvs", subTlvs)
                .toString();
    }
}