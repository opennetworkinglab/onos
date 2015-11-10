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
package org.onosproject.bgpio.protocol.linkstate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.BGPValueType;
import org.onosproject.bgpio.types.IPv4AddressTlv;
import org.onosproject.bgpio.types.IPv6AddressTlv;
import org.onosproject.bgpio.types.LinkLocalRemoteIdentifiersTlv;
import org.onosproject.bgpio.types.attr.BgpAttrNodeMultiTopologyId;
import org.onosproject.bgpio.util.UnSupportedAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * Implementation of local node descriptors, remote node descriptors and link descriptors.
 */
public class BGPLinkLSIdentifier {
    private static final Logger log = LoggerFactory.getLogger(BGPLinkLSIdentifier.class);
    public static final short IPV4_INTERFACE_ADDRESS_TYPE = 259;
    public static final short IPV4_NEIGHBOR_ADDRESS_TYPE = 260;
    public static final short IPV6_INTERFACE_ADDRESS_TYPE = 261;
    public static final short IPV6_NEIGHBOR_ADDRESS_TYPE = 262;
    public static final int TYPE_AND_LEN = 4;

    private NodeDescriptors localNodeDescriptors;
    private NodeDescriptors remoteNodeDescriptors;
    private List<BGPValueType> linkDescriptor;

    /**
     * Initialize fields.
     */
    public BGPLinkLSIdentifier() {
        this.localNodeDescriptors = null;
        this.remoteNodeDescriptors = null;
        this.linkDescriptor = null;
    }

    /**
     * Constructors to initialize parameters.
     *
     * @param localNodeDescriptors local node descriptors
     * @param remoteNodeDescriptors remote node descriptors
     * @param linkDescriptor link descriptors
     */
    public BGPLinkLSIdentifier(NodeDescriptors localNodeDescriptors, NodeDescriptors remoteNodeDescriptors,
            LinkedList<BGPValueType> linkDescriptor) {
        this.localNodeDescriptors = Preconditions.checkNotNull(localNodeDescriptors);
        this.remoteNodeDescriptors = Preconditions.checkNotNull(remoteNodeDescriptors);
        this.linkDescriptor = Preconditions.checkNotNull(linkDescriptor);
    }

    /**
     * Reads channel buffer and parses link identifier.
     *
     * @param cb ChannelBuffer
     * @param protocolId in linkstate nlri
     * @return object of BGPLinkLSIdentifier
     * @throws BGPParseException while parsing link identifier
     */
    public static BGPLinkLSIdentifier parseLinkIdendifier(ChannelBuffer cb, byte protocolId) throws BGPParseException {
        //Parse local node descriptor
        NodeDescriptors localNodeDescriptors = new NodeDescriptors();
        localNodeDescriptors = parseNodeDescriptors(cb, NodeDescriptors.LOCAL_NODE_DES_TYPE, protocolId);

        //Parse remote node descriptor
        NodeDescriptors remoteNodeDescriptors = new NodeDescriptors();
        remoteNodeDescriptors = parseNodeDescriptors(cb, NodeDescriptors.REMOTE_NODE_DES_TYPE, protocolId);

        //Parse link descriptor
        LinkedList<BGPValueType> linkDescriptor = new LinkedList<>();
        linkDescriptor = parseLinkDescriptors(cb);
        return new BGPLinkLSIdentifier(localNodeDescriptors, remoteNodeDescriptors, linkDescriptor);
    }

    /**
     * Parses Local/Remote node descriptors.
     *
     * @param cb ChannelBuffer
     * @param desType descriptor type
     * @param protocolId protocol identifier
     * @return object of NodeDescriptors
     * @throws BGPParseException while parsing Local/Remote node descriptors
     */
    public static NodeDescriptors parseNodeDescriptors(ChannelBuffer cb, short desType, byte protocolId)
            throws BGPParseException {
        ChannelBuffer tempBuf = cb;
        short type = cb.readShort();
        short length = cb.readShort();
        if (cb.readableBytes() < length) {
            throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR, BGPErrorType.OPTIONAL_ATTRIBUTE_ERROR,
                    tempBuf.readBytes(cb.readableBytes() + TYPE_AND_LEN));
        }
        NodeDescriptors nodeIdentifier = new NodeDescriptors();
        ChannelBuffer tempCb = cb.readBytes(length);

        if (type == desType) {
            nodeIdentifier = NodeDescriptors.read(tempCb, length, desType, protocolId);
        } else {
            throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR, BGPErrorType.MALFORMED_ATTRIBUTE_LIST, null);
        }
        return nodeIdentifier;
    }

    /**
     * Parses link descriptors.
     *
     * @param cb ChannelBuffer
     * @return list of link descriptors
     * @throws BGPParseException while parsing link descriptors
     */
    public static LinkedList<BGPValueType> parseLinkDescriptors(ChannelBuffer cb) throws BGPParseException {
        LinkedList<BGPValueType> linkDescriptor = new LinkedList<>();
        BGPValueType tlv = null;
        int count = 0;

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
            case LinkLocalRemoteIdentifiersTlv.TYPE:
                tlv = LinkLocalRemoteIdentifiersTlv.read(tempCb);
                break;
            case IPV4_INTERFACE_ADDRESS_TYPE:
                tlv = IPv4AddressTlv.read(tempCb, IPV4_INTERFACE_ADDRESS_TYPE);
                break;
            case IPV4_NEIGHBOR_ADDRESS_TYPE:
                tlv = IPv4AddressTlv.read(tempCb, IPV4_NEIGHBOR_ADDRESS_TYPE);
                break;
            case IPV6_INTERFACE_ADDRESS_TYPE:
                tlv = IPv6AddressTlv.read(tempCb, IPV6_INTERFACE_ADDRESS_TYPE);
                break;
            case IPV6_NEIGHBOR_ADDRESS_TYPE:
                tlv = IPv6AddressTlv.read(tempCb, IPV6_NEIGHBOR_ADDRESS_TYPE);
                break;
            case BgpAttrNodeMultiTopologyId.ATTRNODE_MULTITOPOLOGY:
                tlv = BgpAttrNodeMultiTopologyId.read(tempCb);
                count = count++;
                //MultiTopologyId TLV cannot repeat more than once
                if (count > 1) {
                    //length + 4 implies data contains type, length and value
                    throw new BGPParseException(BGPErrorType.UPDATE_MESSAGE_ERROR,
                            BGPErrorType.OPTIONAL_ATTRIBUTE_ERROR, tempBuf.readBytes(length
                                    + TYPE_AND_LEN));
                }
                break;
            default:
                UnSupportedAttribute.skipBytes(tempCb, length);
            }
            linkDescriptor.add(tlv);
        }
        return linkDescriptor;
    }

    /**
     * Returns local node descriptors.
     *
     * @return local node descriptors
     */
    public NodeDescriptors localNodeDescriptors() {
        return this.localNodeDescriptors;
    }

    /**
     * Returns remote node descriptors.
     *
     * @return remote node descriptors
     */
    public NodeDescriptors remoteNodeDescriptors() {
        return this.remoteNodeDescriptors;
    }

    /**
     * Returns link descriptors.
     *
     * @return link descriptors
     */
    public List<BGPValueType> linkDescriptors() {
        return this.linkDescriptor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkDescriptor, localNodeDescriptors, remoteNodeDescriptors);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BGPLinkLSIdentifier) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            BGPLinkLSIdentifier other = (BGPLinkLSIdentifier) obj;
            Iterator<BGPValueType> objListIterator = other.linkDescriptor.iterator();
            countOtherSubTlv = other.linkDescriptor.size();
            countObjSubTlv = linkDescriptor.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    BGPValueType subTlv = objListIterator.next();
                    isCommonSubTlv = Objects.equals(linkDescriptor.contains(subTlv),
                            other.linkDescriptor.contains(subTlv));
                }
                return isCommonSubTlv && Objects.equals(this.localNodeDescriptors, other.localNodeDescriptors)
                        && Objects.equals(this.remoteNodeDescriptors, other.remoteNodeDescriptors);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("localNodeDescriptors", localNodeDescriptors)
                .add("remoteNodeDescriptors", remoteNodeDescriptors)
                .add("linkDescriptor", linkDescriptor)
                .toString();
    }
}
