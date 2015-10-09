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
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.BGPValueType;
import org.onosproject.bgpio.types.IPReachabilityInformationTlv;
import org.onosproject.bgpio.types.OSPFRouteTypeTlv;
import org.onosproject.bgpio.types.attr.BgpAttrNodeMultiTopologyId;
import org.onosproject.bgpio.util.UnSupportedAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Implementation of Local node descriptors and prefix descriptors.
 */
public class BGPPrefixLSIdentifier {

    protected static final Logger log = LoggerFactory.getLogger(BGPPrefixLSIdentifier.class);
    public static final int TYPE_AND_LEN = 4;
    private NodeDescriptors localNodeDescriptors;
    private LinkedList<BGPValueType> prefixDescriptor;

    /**
     * Resets parameters.
     */
    public BGPPrefixLSIdentifier() {
        this.localNodeDescriptors = null;
        this.prefixDescriptor = null;
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param localNodeDescriptors Local node descriptors
     * @param prefixDescriptor Prefix Descriptors
     */
    public BGPPrefixLSIdentifier(NodeDescriptors localNodeDescriptors, LinkedList<BGPValueType> prefixDescriptor) {
        this.localNodeDescriptors = localNodeDescriptors;
        this.prefixDescriptor = prefixDescriptor;
    }

    /**
     * Reads the channel buffer and parses Prefix Identifier.
     *
     * @param cb ChannelBuffer
     * @param protocolId protocol ID
     * @return object of this class
     * @throws BGPParseException while parsing Prefix Identifier
     */
    public static BGPPrefixLSIdentifier parsePrefixIdendifier(ChannelBuffer cb, byte protocolId)
            throws BGPParseException {
        //Parse Local Node descriptor
        NodeDescriptors localNodeDescriptors = new NodeDescriptors();
        localNodeDescriptors = parseLocalNodeDescriptors(cb, protocolId);

        //Parse Prefix descriptor
        LinkedList<BGPValueType> prefixDescriptor = new LinkedList<>();
        prefixDescriptor = parsePrefixDescriptors(cb);
        return new BGPPrefixLSIdentifier(localNodeDescriptors, prefixDescriptor);
    }

    /**
     * Parse local node descriptors.
     *
     * @param cb ChannelBuffer
     * @param protocolId protocol identifier
     * @return LocalNodeDescriptors
     * @throws BGPParseException while parsing local node descriptors
     */
    public static NodeDescriptors parseLocalNodeDescriptors(ChannelBuffer cb, byte protocolId)
                                                                 throws BGPParseException {
        ChannelBuffer tempBuf = cb;
        short type = cb.readShort();
        short length = cb.readShort();
        if (cb.readableBytes() < length) {
            //length + 4 implies data contains type, length and value
            throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR, BGPErrorType.OPTIONAL_ATTRIBUTE_ERROR,
                    tempBuf.readBytes(cb.readableBytes() + TYPE_AND_LEN));
        }
        NodeDescriptors localNodeDescriptors = new NodeDescriptors();
        ChannelBuffer tempCb = cb.readBytes(length);

        if (type == NodeDescriptors.LOCAL_NODE_DES_TYPE) {
            localNodeDescriptors = NodeDescriptors.read(tempCb, length, type, protocolId);
        } else {
            throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR,
                                           BGPErrorType.MALFORMED_ATTRIBUTE_LIST, null);
        }
        return localNodeDescriptors;
    }

    /**
     * Parse list of prefix descriptors.
     *
     * @param cb ChannelBuffer
     * @return list of prefix descriptors
     * @throws BGPParseException while parsing list of prefix descriptors
     */
    public static LinkedList<BGPValueType> parsePrefixDescriptors(ChannelBuffer cb) throws BGPParseException {
        LinkedList<BGPValueType> prefixDescriptor = new LinkedList<>();
        BGPValueType tlv = null;
        boolean isIpReachInfo = false;
        ChannelBuffer tempCb;
        int count = 0;

        while (cb.readableBytes() > 0) {
            ChannelBuffer tempBuf = cb;
            short type = cb.readShort();
            short length = cb.readShort();
            if (cb.readableBytes() < length) {
                //length + 4 implies data contains type, length and value
                throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR, BGPErrorType.OPTIONAL_ATTRIBUTE_ERROR,
                        tempBuf.readBytes(cb.readableBytes() + TYPE_AND_LEN));
            }
            tempCb = cb.readBytes(length);
            switch (type) {
            case OSPFRouteTypeTlv.TYPE:
                tlv = OSPFRouteTypeTlv.read(tempCb);
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
                    throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR,
                           BGPErrorType.OPTIONAL_ATTRIBUTE_ERROR, tempBuf.readBytes(length + TYPE_AND_LEN));
                }
                break;
            default:
                UnSupportedAttribute.skipBytes(tempCb, length);
            }
            prefixDescriptor.add(tlv);
        }

        if (!isIpReachInfo) {
            throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR, BGPErrorType.OPTIONAL_ATTRIBUTE_ERROR,
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
    public LinkedList<BGPValueType> getPrefixdescriptor() {
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

        if (obj instanceof BGPPrefixLSIdentifier) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            BGPPrefixLSIdentifier other = (BGPPrefixLSIdentifier) obj;

            Iterator<BGPValueType> objListIterator = other.prefixDescriptor.iterator();
            countOtherSubTlv = other.prefixDescriptor.size();
            countObjSubTlv = prefixDescriptor.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    BGPValueType subTlv = objListIterator.next();
                    isCommonSubTlv = Objects.equals(prefixDescriptor.contains(subTlv),
                            other.prefixDescriptor.contains(subTlv));
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
}