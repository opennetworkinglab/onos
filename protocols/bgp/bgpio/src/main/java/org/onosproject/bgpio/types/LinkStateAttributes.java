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
package org.onosproject.bgpio.types;

import java.util.LinkedList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.attr.BgpAttrNodeFlagBitTlv;
import org.onosproject.bgpio.types.attr.BgpAttrNodeIsIsAreaId;
import org.onosproject.bgpio.types.attr.BgpAttrNodeMultiTopologyId;
import org.onosproject.bgpio.types.attr.BgpAttrNodeName;
import org.onosproject.bgpio.types.attr.BgpAttrOpaqueNode;
import org.onosproject.bgpio.types.attr.BgpAttrRouterIdV4;
import org.onosproject.bgpio.types.attr.BgpAttrRouterIdV6;
import org.onosproject.bgpio.types.attr.BgpLinkAttrIgpMetric;
import org.onosproject.bgpio.types.attr.BgpLinkAttrIsIsAdminstGrp;
import org.onosproject.bgpio.types.attr.BgpLinkAttrMplsProtocolMask;
import org.onosproject.bgpio.types.attr.BgpLinkAttrMaxLinkBandwidth;
import org.onosproject.bgpio.types.attr.BgpLinkAttrName;
import org.onosproject.bgpio.types.attr.BgpLinkAttrOpaqLnkAttrib;
import org.onosproject.bgpio.types.attr.BgpLinkAttrProtectionType;
import org.onosproject.bgpio.types.attr.BgpLinkAttrSrlg;
import org.onosproject.bgpio.types.attr.BgpLinkAttrTeDefaultMetric;
import org.onosproject.bgpio.types.attr.BgpLinkAttrUnRsrvdLinkBandwidth;
import org.onosproject.bgpio.types.attr.BgpPrefixAttrExtRouteTag;
import org.onosproject.bgpio.types.attr.BgpPrefixAttrIgpFlags;
import org.onosproject.bgpio.types.attr.BgpPrefixAttrMetric;
import org.onosproject.bgpio.types.attr.BgpPrefixAttrOspfFwdAddr;
import org.onosproject.bgpio.types.attr.BgpPrefixAttrOpaqueData;
import org.onosproject.bgpio.types.attr.BgpPrefixAttrRouteTag;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implements BGP Link state attribute.
 */
public class LinkStateAttributes implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(LinkStateAttributes.class);

    /* Node Attributes */
    public static final short ATTR_NODE_MT_TOPOLOGY_ID = 263;
    public static final short ATTR_NODE_FLAG_BITS = 1024;
    public static final short ATTR_NODE_OPAQUE_NODE = 1025;
    public static final short ATTR_NODE_NAME = 1026;
    public static final short ATTR_NODE_ISIS_AREA_ID = 1027;
    public static final short ATTR_NODE_IPV4_LOCAL_ROUTER_ID = 1028;
    public static final short ATTR_NODE_IPV6_LOCAL_ROUTER_ID = 1029;

    /* Link Attributes */
    public static final short ATTR_LINK_IPV4_REMOTE_ROUTER_ID = 1030;
    public static final short ATTR_LINK_IPV6_REMOTE_ROUTER_ID = 1031;
    public static final short ATTR_LINK_ADMINISTRATIVE_GRPS = 1088;
    public static final short ATTR_LINK_MAX_BANDWIDTH = 1089;
    public static final short ATTR_LINK_MAX_RES_BANDWIDTH = 1090;
    public static final short ATTR_LINK_UNRES_BANDWIDTH = 1091;
    public static final short ATTR_LINK_TE_DEFAULT_METRIC = 1092;
    public static final short ATTR_LINK_PROTECTION_TYPE = 1093;
    public static final short ATTR_LINK_MPLS_PROTOCOL_MASK = 1094;
    public static final short ATTR_LINK_IGP_METRIC = 1095;
    public static final short ATTR_LINK_SHR_RISK_GRP = 1096;
    public static final short ATTR_LINK_OPAQUE_ATTR = 1097;
    public static final short ATTR_LINK_NAME_ATTR = 1098;

    /* Prefix Attributes */
    public static final short ATTR_PREFIX_IGP_FLAG = 1152;
    public static final short ATTR_PREFIX_ROUTE_TAG = 1153;
    public static final short ATTR_PREFIX_EXTENDED_TAG = 1154;
    public static final short ATTR_PREFIX_METRIC = 1155;
    public static final short ATTR_PREFIX_OSPF_FWD_ADDR = 1156;
    public static final short ATTR_PREFIX_OPAQUE_ATTR = 1157;

    public static final byte LINKSTATE_ATTRIB_TYPE = 29;
    public static final byte TYPE_AND_LEN = 4;
    private boolean isLinkStateAttribute = false;
    private List<BgpValueType> linkStateAttribList;

    /**
     * Constructor to reset parameters.
     */
    LinkStateAttributes() {
        this.linkStateAttribList = null;
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param linkStateAttribList Linked list of Link, Node and Prefix TLVs
     */
    public LinkStateAttributes(List<BgpValueType> linkStateAttribList) {
        this.linkStateAttribList = linkStateAttribList;
        this.isLinkStateAttribute = true;
    }

    /**
     * Returns linked list of Link, Node and Prefix TLVs.
     *
     * @return linked list of Link, Node and Prefix TLVs
     */
    public List<BgpValueType> linkStateAttributes() {
        return this.linkStateAttribList;
    }

    /**
     * Returns if the Link state attributes are set or not.
     *
     * @return a boolean value to to check if the LS attributes are set or not
     */
    public boolean isLinkStateAttributeSet() {
        return this.isLinkStateAttribute;
    }

    /**
     * Reads the Link state attribute TLVs.
     *
     * @param cb ChannelBuffer
     * @return constructor of LinkStateAttributes
     * @throws BgpParseException while parsing link state attributes
     */
    public static LinkStateAttributes read(ChannelBuffer cb)
            throws BgpParseException {

        ChannelBuffer tempBuf = cb.copy();
        Validation parseFlags = Validation.parseAttributeHeader(cb);
        int len = parseFlags.isShort() ? parseFlags.getLength() + TYPE_AND_LEN
                                      : parseFlags.getLength() + 3;

        ChannelBuffer data = tempBuf.readBytes(len);
        if (!parseFlags.getFirstBit() && parseFlags.getSecondBit()
                && parseFlags.getThirdBit()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                        BgpErrorType.ATTRIBUTE_FLAGS_ERROR,
                                        data);
        }

        if (cb.readableBytes() < parseFlags.getLength()) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.BAD_MESSAGE_LENGTH,
                                   parseFlags.getLength());
        }

        BgpValueType bgpLSAttrib = null;
        LinkedList<BgpValueType> linkStateAttribList;
        linkStateAttribList = new LinkedList<BgpValueType>();
        ChannelBuffer tempCb = cb.readBytes(parseFlags.getLength());
        while (tempCb.readableBytes() > 0) {
            short tlvCodePoint = tempCb.readShort();
            switch (tlvCodePoint) {

            /********* 7 NODE ATTRIBUTES ********/
            case ATTR_NODE_MT_TOPOLOGY_ID: /* 263 Multi-Topology Identifier*/
                bgpLSAttrib = BgpAttrNodeMultiTopologyId.read(tempCb);
                break;

            case ATTR_NODE_FLAG_BITS: /*Node flag bit TLV*/
                bgpLSAttrib = BgpAttrNodeFlagBitTlv.read(tempCb);
                break;

            case ATTR_NODE_OPAQUE_NODE: /*Opaque Node Attribute*/
                bgpLSAttrib = BgpAttrOpaqueNode.read(tempCb);
                break;

            case ATTR_NODE_NAME: /*Node Name*/
                bgpLSAttrib = BgpAttrNodeName.read(tempCb);
                break;

            case ATTR_NODE_ISIS_AREA_ID: /*IS-IS Area Identifier TLV*/
                bgpLSAttrib = BgpAttrNodeIsIsAreaId.read(tempCb);
                break;

            case ATTR_NODE_IPV4_LOCAL_ROUTER_ID: /*IPv4 Router-ID of Local Node*/
                bgpLSAttrib = BgpAttrRouterIdV4.read(tempCb, (short) ATTR_NODE_IPV4_LOCAL_ROUTER_ID);
                break;

            case ATTR_NODE_IPV6_LOCAL_ROUTER_ID: /*IPv6 Router-ID of Local Node*/
                bgpLSAttrib = BgpAttrRouterIdV6.read(tempCb, (short) ATTR_NODE_IPV6_LOCAL_ROUTER_ID);
                break;

            /********* 15 LINK ATTRIBUTES ********/

            case ATTR_LINK_IPV4_REMOTE_ROUTER_ID: /*IPv4 Router-ID of Remote Node*/
                bgpLSAttrib = BgpAttrRouterIdV4.read(tempCb, (short) 1030);
                break;

            case ATTR_LINK_IPV6_REMOTE_ROUTER_ID: /*IPv6 Router-ID of Remote Node*/
                bgpLSAttrib = BgpAttrRouterIdV6.read(tempCb, (short) 1031);
                break;

            case ATTR_LINK_ADMINISTRATIVE_GRPS: /*ISIS Administrative group STLV 3*/
                bgpLSAttrib = BgpLinkAttrIsIsAdminstGrp.read(tempCb);
                break;

            case ATTR_LINK_MAX_BANDWIDTH: /*Maximum link bandwidth*/
                bgpLSAttrib = BgpLinkAttrMaxLinkBandwidth.read(tempCb,
                                                               (short) 1089);
                break;

            case ATTR_LINK_MAX_RES_BANDWIDTH: /* Maximum Reservable link bandwidth */
                bgpLSAttrib = BgpLinkAttrMaxLinkBandwidth.read(tempCb,
                                                               (short) 1090);
                break;

            case ATTR_LINK_UNRES_BANDWIDTH: /* UnReserved link bandwidth */
                bgpLSAttrib = BgpLinkAttrUnRsrvdLinkBandwidth
                        .read(tempCb, (short) 1091);
                break;

            case ATTR_LINK_TE_DEFAULT_METRIC: /* TE Default Metric */
                bgpLSAttrib = BgpLinkAttrTeDefaultMetric.read(tempCb);
                break;

            case ATTR_LINK_PROTECTION_TYPE:/* Link Protection type */
                bgpLSAttrib = BgpLinkAttrProtectionType.read(tempCb);
                break;

            case ATTR_LINK_MPLS_PROTOCOL_MASK: /* MPLS Protocol Mask */
                bgpLSAttrib = BgpLinkAttrMplsProtocolMask.read(tempCb); // 2
                break;

            case ATTR_LINK_IGP_METRIC: /* IGP Metric */
                bgpLSAttrib = BgpLinkAttrIgpMetric.read(tempCb); // 2
                break;

            case ATTR_LINK_SHR_RISK_GRP: /* Shared Risk Link Group */
                bgpLSAttrib = BgpLinkAttrSrlg.read(tempCb); // 3
                break;

            case ATTR_LINK_OPAQUE_ATTR: /* Opaque link attribute */
                bgpLSAttrib = BgpLinkAttrOpaqLnkAttrib.read(tempCb);
                break;

            case ATTR_LINK_NAME_ATTR: /* Link Name attribute */
                bgpLSAttrib = BgpLinkAttrName.read(tempCb);
                break;

            /********* 6 PREFIX ATTRIBUTES ********/

            case ATTR_PREFIX_IGP_FLAG: /* IGP Flags */
                bgpLSAttrib = BgpPrefixAttrIgpFlags.read(tempCb);
                break;

            case ATTR_PREFIX_ROUTE_TAG: /* Route Tag */
                bgpLSAttrib = BgpPrefixAttrRouteTag.read(tempCb);
                break;

            case ATTR_PREFIX_EXTENDED_TAG: /* Extended Tag */
                bgpLSAttrib = BgpPrefixAttrExtRouteTag.read(tempCb);
                break;

            case ATTR_PREFIX_METRIC: /* Prefix Metric */
                bgpLSAttrib = BgpPrefixAttrMetric.read(tempCb);
                break;

            case ATTR_PREFIX_OSPF_FWD_ADDR: /* OSPF Forwarding Address */
                bgpLSAttrib = BgpPrefixAttrOspfFwdAddr.read(tempCb);
                break;

            case ATTR_PREFIX_OPAQUE_ATTR: /* Opaque Prefix Attribute */
                bgpLSAttrib = BgpPrefixAttrOpaqueData.read(tempCb);
                break;

            default:
                throw new BgpParseException(
                                            "The Bgp-LS Attribute is not supported : "
                                                    + tlvCodePoint);
            }

            linkStateAttribList.add(bgpLSAttrib);
        }
        return new LinkStateAttributes(linkStateAttribList);
    }

    @Override
    public short getType() {
        return LINKSTATE_ATTRIB_TYPE;
    }

    @Override
    public int write(ChannelBuffer cb) {
        // TODO This will be implemented in the next version
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("linkStateAttribList", linkStateAttribList).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
