/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.IPReachabilityInformationTlv;
import org.onosproject.bgpio.types.OspfRouteTypeTlv;
import org.onosproject.bgpio.types.attr.BgpAttrNodeMultiTopologyId;
import org.onosproject.bgpio.util.UnSupportedAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Implementation of Local node descriptors and prefix descriptors.
 */
public class BgpPrefixLSIdentifier implements Comparable<Object> {

    protected static final Logger log = LoggerFactory.getLogger(BgpPrefixLSIdentifier.class);
    public static final int TYPE_AND_LEN = 4;
    private NodeDescriptors localNodeDescriptors;
    private List<BgpValueType> prefixDescriptor;

    /**
     * Resets parameters.
     */
    public BgpPrefixLSIdentifier() {
        this.localNodeDescriptors = null;
        this.prefixDescriptor = null;
        log.debug("Parameters are reset");
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param localNodeDescriptors Local node descriptors
     * @param prefixDescriptor Prefix Descriptors
     */
    public BgpPrefixLSIdentifier(NodeDescriptors localNodeDescriptors, List<BgpValueType> prefixDescriptor) {
        this.localNodeDescriptors = localNodeDescriptors;
        this.prefixDescriptor = prefixDescriptor;
    }

    /**
     * Reads the channel buffer and parses Prefix Identifier.
     *
     * @param cb ChannelBuffer
     * @param protocolId protocol ID
     * @return object of this class
     * @throws BgpParseException while parsing Prefix Identifier
     */
    public static BgpPrefixLSIdentifier parsePrefixIdendifier(ChannelBuffer cb, byte protocolId)
            throws BgpParseException {
        log.debug("Parse local node descriptor");
        NodeDescriptors localNodeDescriptors = new NodeDescriptors();
        localNodeDescriptors = parseLocalNodeDescriptors(cb, protocolId);

        log.debug("MultiTopologyId TLV cannot repeat more than once");
        List<BgpValueType> prefixDescriptor = new LinkedList<>();
        prefixDescriptor = parsePrefixDescriptors(cb);
        return new BgpPrefixLSIdentifier(localNodeDescriptors, prefixDescriptor);
    }

    /**
     * Parse local node descriptors.
     *
     * @param cb ChannelBuffer
     * @param protocolId protocol identifier
     * @return LocalNodeDescriptors
     * @throws BgpParseException while parsing local node descriptors
     */
    public static NodeDescriptors parseLocalNodeDescriptors(ChannelBuffer cb, byte protocolId)
                                                                 throws BgpParseException {
        ChannelBuffer tempBuf = cb.copy();
        short type = cb.readShort();
        short length = cb.readShort();
        if (cb.readableBytes() < length) {
            //length + 4 implies data contains type, length and value
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.OPTIONAL_ATTRIBUTE_ERROR,
                    tempBuf.readBytes(cb.readableBytes() + TYPE_AND_LEN));
        }
        NodeDescriptors localNodeDescriptors = new NodeDescriptors();
        ChannelBuffer tempCb = cb.readBytes(length);

        if (type == NodeDescriptors.LOCAL_NODE_DES_TYPE) {
            localNodeDescriptors = NodeDescriptors.read(tempCb, length, type, protocolId);
        } else {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                           BgpErrorType.MALFORMED_ATTRIBUTE_LIST, null);
        }
        return localNodeDescriptors;
    }

    /**
     * Parse list of prefix descriptors.
     *
     * @param cb ChannelBuffer
     * @return list of prefix descriptors
     * @throws BgpParseException while parsing list of prefix descriptors
     */
    public static List<BgpValueType> parsePrefixDescriptors(ChannelBuffer cb) throws BgpParseException {
        LinkedList<BgpValueType> prefixDescriptor = new LinkedList<>();
        BgpValueType tlv = null;
        boolean isIpReachInfo = false;
        ChannelBuffer tempCb;
        int count = 0;

        while (cb.readableBytes() > 0) {
            ChannelBuffer tempBuf = cb.copy();
            short type = cb.readShort();
            short length = cb.readShort();
            if (cb.readableBytes() < length) {
                //length + 4 implies data contains type, length and value
                throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.OPTIONAL_ATTRIBUTE_ERROR,
                        tempBuf.readBytes(cb.readableBytes() + TYPE_AND_LEN));
            }
            tempCb = cb.readBytes(length);
            switch (type) {
            case OspfRouteTypeTlv.TYPE:
                tlv = OspfRouteTypeTlv.read(tempCb);
                break;
            case IPReachabilityInformationTlv.TYPE:
                tlv = IPReachabilityInformationTlv.read(tempCb, length);
                isIpReachInfo = true;
                break;
            case BgpAttrNodeMultiTopologyId.ATTRNODE_MULTITOPOLOGY:
                tlv = BgpAttrNodeMultiTopologyId.read(tempCb);
                count = count + 1;
                if (count > 1) {
                    //length + 4 implies data contains type, length and value
                    throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR,
                           BgpErrorType.OPTIONAL_ATTRIBUTE_ERROR, tempBuf.readBytes(length + TYPE_AND_LEN));
                }
                break;
            default:
                UnSupportedAttribute.skipBytes(tempCb, length);
            }
            prefixDescriptor.add(tlv);
        }

        if (!isIpReachInfo) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.OPTIONAL_ATTRIBUTE_ERROR,
                    null);
        }
        return prefixDescriptor;
    }

    /**
     * Returns local node descriptors.
     *
     * @return local node descriptors
     */
    public NodeDescriptors getLocalNodeDescriptors() {
        return this.localNodeDescriptors;
    }

    /**
     * Returns Prefix descriptors.
     *
     * @return Prefix descriptors
     */
    public List<BgpValueType> getPrefixdescriptor() {
        return this.prefixDescriptor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefixDescriptor.hashCode(), localNodeDescriptors);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpPrefixLSIdentifier) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            BgpPrefixLSIdentifier other = (BgpPrefixLSIdentifier) obj;

            Iterator<BgpValueType> objListIterator = other.prefixDescriptor.iterator();
            countOtherSubTlv = other.prefixDescriptor.size();
            countObjSubTlv = prefixDescriptor.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    BgpValueType subTlv = objListIterator.next();
                    if (prefixDescriptor.contains(subTlv) && other.prefixDescriptor.contains(subTlv)) {
                        isCommonSubTlv = Objects.equals(prefixDescriptor.get(prefixDescriptor.indexOf(subTlv)),
                                         other.prefixDescriptor.get(other.prefixDescriptor.indexOf(subTlv)));
                    } else {
                        isCommonSubTlv = false;
                    }
                }
                return isCommonSubTlv && Objects.equals(this.localNodeDescriptors, other.localNodeDescriptors);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("localNodeDescriptors", localNodeDescriptors)
                .add("prefixDescriptor", prefixDescriptor)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }
        int result = this.localNodeDescriptors.compareTo(((BgpPrefixLSIdentifier) o).localNodeDescriptors);
        boolean tlvFound = false;
        if (result != 0) {
            return result;
        } else {
            int countOtherSubTlv = ((BgpPrefixLSIdentifier) o).prefixDescriptor.size();
            int countObjSubTlv = prefixDescriptor.size();
            if (countOtherSubTlv != countObjSubTlv) {
                if (countOtherSubTlv > countObjSubTlv) {
                    return 1;
                } else {
                    return -1;
                }
            }

            ListIterator<BgpValueType> listIterator = prefixDescriptor.listIterator();
            while (listIterator.hasNext()) {
                BgpValueType tlv1 = listIterator.next();
                for (BgpValueType tlv : ((BgpPrefixLSIdentifier) o).prefixDescriptor) {
                    if (tlv.getType() == tlv1.getType()) {
                        result = prefixDescriptor.get(prefixDescriptor.indexOf(tlv1)).compareTo(
                                ((BgpPrefixLSIdentifier) o).prefixDescriptor
                                        .get(((BgpPrefixLSIdentifier) o).prefixDescriptor.indexOf(tlv)));
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
