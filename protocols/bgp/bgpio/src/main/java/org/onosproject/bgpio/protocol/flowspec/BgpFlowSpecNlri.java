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
package org.onosproject.bgpio.protocol.flowspec;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onosproject.bgpio.types.BgpFsDestinationPortNum;
import org.onosproject.bgpio.types.BgpFsDestinationPrefix;
import org.onosproject.bgpio.types.BgpFsDscpValue;
import org.onosproject.bgpio.types.BgpFsFragment;
import org.onosproject.bgpio.types.BgpFsIcmpCode;
import org.onosproject.bgpio.types.BgpFsIcmpType;
import org.onosproject.bgpio.types.BgpFsIpProtocol;
import org.onosproject.bgpio.types.BgpFsPacketLength;
import org.onosproject.bgpio.types.BgpFsPortNum;
import org.onosproject.bgpio.types.BgpFsSourcePortNum;
import org.onosproject.bgpio.types.BgpFsSourcePrefix;
import org.onosproject.bgpio.types.BgpFsTcpFlags;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Constants;

import com.google.common.base.MoreObjects;

/**
 * This Class stores flow specification components and action.
 */
public class BgpFlowSpecNlri {
    private List<BgpValueType> flowSpecComponents;
    private List<BgpValueType> fsActionTlv;
    private RouteDistinguisher routeDistinguisher;
    public static final short FLOW_SPEC_LEN = 240;

    /**
     * Flow specification details object constructor with the parameter.
     *
     * @param flowSpecComponents flow specification components
     */
    public BgpFlowSpecNlri(List<BgpValueType> flowSpecComponents) {
        this.flowSpecComponents = flowSpecComponents;
    }

    /**
     * Flow specification details object constructor.
     *
     */
    public BgpFlowSpecNlri() {

    }

    /**
     * Returns flow specification action tlv.
     *
     * @return flow specification action tlv
     */
    public List<BgpValueType> fsActionTlv() {
        return this.fsActionTlv;
    }

    /**
     * Set flow specification action tlv.
     *
     * @param fsActionTlv flow specification action tlv
     */
    public void setFsActionTlv(List<BgpValueType> fsActionTlv) {
        this.fsActionTlv = fsActionTlv;
    }

    /**
     * Returns route distinguisher for the flow specification components.
     *
     * @return route distinguisher for the flow specification components
     */
    public RouteDistinguisher routeDistinguisher() {
        return this.routeDistinguisher;
    }

    /**
     * Set route distinguisher for flow specification component.
     *
     * @param routeDistinguisher route distinguisher
     */
    public void setRouteDistinguiher(RouteDistinguisher routeDistinguisher) {
        this.routeDistinguisher = routeDistinguisher;
    }

    /**
     * Returns flow specification components.
     *
     * @return flow specification components
     */
    public List<BgpValueType> flowSpecComponents() {
        return this.flowSpecComponents;
    }

    /**
     * Sets flow specification components.
     *
     * @param flowSpecComponents flow specification components
     */
    public void setFlowSpecComponents(List<BgpValueType> flowSpecComponents) {
        this.flowSpecComponents = flowSpecComponents;
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowSpecComponents);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpFlowSpecNlri) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            BgpFlowSpecNlri other = (BgpFlowSpecNlri) obj;
            Iterator<BgpValueType> objListIterator = other.flowSpecComponents.iterator();
            countOtherSubTlv = other.flowSpecComponents.size();
            countObjSubTlv = flowSpecComponents.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    BgpValueType subTlv = objListIterator.next();
                    if (flowSpecComponents.contains(subTlv) && other.flowSpecComponents.contains(subTlv)) {
                        isCommonSubTlv = Objects.equals(flowSpecComponents.get(flowSpecComponents.indexOf(subTlv)),
                                            other.flowSpecComponents.get(other.flowSpecComponents.indexOf(subTlv)));
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
     * Write flow type to channel buffer.
     *
     * @param tlv flow type
     * @param cb channel buffer
     */
    public static void writeFlowType(BgpValueType tlv, ChannelBuffer cb) {

        switch (tlv.getType()) {
            case Constants.BGP_FLOWSPEC_DST_PREFIX:
                BgpFsDestinationPrefix fsDstPrefix = (BgpFsDestinationPrefix) tlv;
                fsDstPrefix.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_SRC_PREFIX:
                BgpFsSourcePrefix fsSrcPrefix = (BgpFsSourcePrefix) tlv;
                fsSrcPrefix.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_IP_PROTO:
                BgpFsIpProtocol fsIpProtocol = (BgpFsIpProtocol) tlv;
                fsIpProtocol.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_PORT:
                BgpFsPortNum fsPortNum = (BgpFsPortNum) tlv;
                fsPortNum.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_DST_PORT:
                BgpFsDestinationPortNum fsDstPortNum = (BgpFsDestinationPortNum) tlv;
                fsDstPortNum.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_SRC_PORT:
                BgpFsSourcePortNum fsSrcPortNum = (BgpFsSourcePortNum) tlv;
                fsSrcPortNum.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_ICMP_TP:
                BgpFsIcmpType fsIcmpType = (BgpFsIcmpType) tlv;
                fsIcmpType.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_ICMP_CD:
                BgpFsIcmpCode fsIcmpCode = (BgpFsIcmpCode) tlv;
                fsIcmpCode.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_TCP_FLAGS:
                BgpFsTcpFlags fsTcpFlags = (BgpFsTcpFlags) tlv;
                fsTcpFlags.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_PCK_LEN:
                BgpFsPacketLength fsPacketLen = (BgpFsPacketLength) tlv;
                fsPacketLen.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_DSCP:
                BgpFsDscpValue fsDscpVal = (BgpFsDscpValue) tlv;
                fsDscpVal.write(cb);
                break;
            case Constants.BGP_FLOWSPEC_FRAGMENT:
                BgpFsFragment fsFragment = (BgpFsFragment) tlv;
                fsFragment.write(cb);
                break;
            default:
                break;
        }
        return;
    }

    /**
     * Update buffer with identical flow types.
     *
     * @param cb channel buffer
     * @param bgpFlowSpecNlri flow specification
     */
    public static void updateBufferIdenticalFlowTypes(ChannelBuffer cb, BgpFlowSpecNlri bgpFlowSpecNlri) {

        List<BgpValueType> flowSpec = bgpFlowSpecNlri.flowSpecComponents();
        ListIterator<BgpValueType> listIterator = flowSpec.listIterator();

        while (listIterator.hasNext()) {
            ChannelBuffer flowSpecTmpBuff = ChannelBuffers.dynamicBuffer();
            int tmpBuffStartIndx = flowSpecTmpBuff.writerIndex();

            BgpValueType tlv = listIterator.next();
            writeFlowType(tlv, flowSpecTmpBuff);

            /* RFC 5575: section 4,  If the NLRI length value is smaller than 240 (0xf0 hex), the length
                                     field can be encoded as a single octet.  Otherwise, it is encoded as
                                     an extended-length 2-octet values */
            int len = flowSpecTmpBuff.writerIndex() - tmpBuffStartIndx;
            if (len >= FLOW_SPEC_LEN) {
                cb.writeShort(len);
            } else {
                cb.writeByte(len);
            }
            //Copy from bynamic buffer to channel buffer
            cb.writeBytes(flowSpecTmpBuff);
        }
        return;
    }

    /**
     * Update buffer with non-identical flow types.
     *
     * @param cb channel buffer
     * @param bgpFlowSpecNlri flow specification
     */
    public static void updateBufferNonIdenticalFlowTypes(ChannelBuffer cb, BgpFlowSpecNlri bgpFlowSpecNlri) {
        ChannelBuffer flowSpecTmpBuff = ChannelBuffers.dynamicBuffer();
        List<BgpValueType> flowSpec = bgpFlowSpecNlri.flowSpecComponents();
        ListIterator<BgpValueType> listIterator = flowSpec.listIterator();
        int tmpBuffStartIndx = flowSpecTmpBuff.writerIndex();

        flowSpec = bgpFlowSpecNlri.flowSpecComponents();
        listIterator = flowSpec.listIterator();

        while (listIterator.hasNext()) {
            BgpValueType tlv = listIterator.next();
            writeFlowType(tlv, flowSpecTmpBuff);
        }

        /* RFC 5575: section 4,  If the NLRI length value is smaller than 240 (0xf0 hex), the length
                                 field can be encoded as a single octet.  Otherwise, it is encoded as
                                 an extended-length 2-octet values */
        int len = flowSpecTmpBuff.writerIndex() - tmpBuffStartIndx;
        if (len >= FLOW_SPEC_LEN) {
            cb.writeShort(len);
        } else {
            cb.writeByte(len);
        }
        //Copy from bynamic buffer to channel buffer
        cb.writeBytes(flowSpecTmpBuff);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("flowSpecComponents", flowSpecComponents)
                .toString();
    }
}
