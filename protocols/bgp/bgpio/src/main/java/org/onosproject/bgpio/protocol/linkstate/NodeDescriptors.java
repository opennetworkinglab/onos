/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.bgpio.protocol.linkstate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.AreaIDTlv;
import org.onosproject.bgpio.types.AutonomousSystemTlv;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpLSIdentifierTlv;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.IsIsNonPseudonode;
import org.onosproject.bgpio.types.IsIsPseudonode;
import org.onosproject.bgpio.types.OspfNonPseudonode;
import org.onosproject.bgpio.types.OspfPseudonode;
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

    private static final Logger log = LoggerFactory.getLogger(NodeDescriptors.class);

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
    private List<BgpValueType> subTlvs;
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
    public NodeDescriptors(List<BgpValueType> subTlvs, short deslength, short desType) {
        this.subTlvs = subTlvs;
        this.deslength = deslength;
        this.desType = desType;
    }

    /**
     * Returns list of subTlvs.
     *
     * @return subTlvs list of subTlvs
     */
    public List<BgpValueType> getSubTlvs() {
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
            Iterator<BgpValueType> objListIterator = other.subTlvs.iterator();
            countOtherSubTlv = other.subTlvs.size();
            countObjSubTlv = subTlvs.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    BgpValueType subTlv = objListIterator.next();
                    if (subTlvs.contains(subTlv) && other.subTlvs.contains(subTlv)) {
                        isCommonSubTlv = Objects.equals(subTlvs.get(subTlvs.indexOf(subTlv)),
                                         other.subTlvs.get(other.subTlvs.indexOf(subTlv)));
                    } else {
                        isCommonSubTlv = false;
                    }
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
     * @throws BgpParseException while parsing node descriptors
     */
    public static NodeDescriptors read(ChannelBuffer cb, short desLength, short desType, byte protocolId)
            throws BgpParseException {
        log.debug("Read NodeDescriptor");
        List<BgpValueType> subTlvs = new LinkedList<>();
        BgpValueType tlv = null;

        while (cb.readableBytes() > 0) {
            ChannelBuffer tempBuf = cb.copy();
            short type = cb.readShort();
            short length = cb.readShort();
            if (cb.readableBytes() < length) {
                throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.OPTIONAL_ATTRIBUTE_ERROR,
                        tempBuf.readBytes(cb.readableBytes() + TYPE_AND_LEN));
            }
            ChannelBuffer tempCb = cb.readBytes(length);
            switch (type) {
            case AutonomousSystemTlv.TYPE:
                tlv = AutonomousSystemTlv.read(tempCb);
                break;
            case BgpLSIdentifierTlv.TYPE:
                tlv = BgpLSIdentifierTlv.read(tempCb);
                break;
            case AreaIDTlv.TYPE:
                tlv = AreaIDTlv.read(tempCb);
                break;
            case IGP_ROUTERID_TYPE:
                if (protocolId == IS_IS_LEVEL_1_PROTOCOL_ID || protocolId == IS_IS_LEVEL_2_PROTOCOL_ID) {
                    boolean isNonPseudoNode = true;
                    if ((length == ISISPSEUDONODE_LEN) && (tempCb.getByte(ISISPSEUDONODE_LEN - 1) != 0)) {
                        isNonPseudoNode = false;
                    }
                    if (isNonPseudoNode) {
                        tlv = IsIsNonPseudonode.read(tempCb);
                    } else {
                        tlv = IsIsPseudonode.read(tempCb);
                    }
                } else if (protocolId == OSPF_V2_PROTOCOL_ID || protocolId == OSPF_V3_PROTOCOL_ID) {
                    if (length == OSPFNONPSEUDONODE_LEN) {
                        tlv = OspfNonPseudonode.read(tempCb);
                    } else if (length == OSPFPSEUDONODE_LEN) {
                        tlv = OspfPseudonode.read(tempCb);
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

    /**
     * Compares this and o object.
     *
     * @param o object to be compared with this object
     * @return which object is greater
     */
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }
        ListIterator<BgpValueType> listIterator = subTlvs.listIterator();
        int countOtherSubTlv = ((NodeDescriptors) o).subTlvs.size();
        int countObjSubTlv = subTlvs.size();
        boolean tlvFound = false;
        if (countOtherSubTlv != countObjSubTlv) {
            if (countOtherSubTlv > countObjSubTlv) {
                return 1;
            } else {
                return -1;
            }
        } else {
            while (listIterator.hasNext()) {
                BgpValueType tlv1 = listIterator.next();
                log.debug("NodeDescriptor compare subtlv's");
                for (BgpValueType tlv : ((NodeDescriptors) o).subTlvs) {
                    if (tlv.getType() == tlv1.getType()) {
                        if (tlv.getType() == IGP_ROUTERID_TYPE) {
                            if ((tlv1 instanceof IsIsNonPseudonode && tlv instanceof IsIsPseudonode)
                                || (tlv1 instanceof IsIsPseudonode && tlv instanceof IsIsNonPseudonode)
                                || (tlv1 instanceof OspfNonPseudonode && tlv instanceof OspfPseudonode)
                                || (tlv1 instanceof OspfPseudonode && tlv instanceof OspfNonPseudonode)) {
                                continue;
                            }
                        }
                        int result = subTlvs.get(subTlvs.indexOf(tlv1)).compareTo(
                                ((NodeDescriptors) o).subTlvs.get(((NodeDescriptors) o).subTlvs.indexOf(tlv)));
                        if (result != 0) {
                            return result;
                        }
                        tlvFound = true;
                        break;
                    }
                }
                if (!tlvFound) {
                    return 1;
                }
            }
        }
        return 0;
    }
}
