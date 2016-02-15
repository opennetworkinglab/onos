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
package org.onosproject.fnl.base;

import org.onosproject.fnl.intf.NetworkAnomaly;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.IPDscpCriterion;
import org.onosproject.net.flow.criteria.IPEcnCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.criteria.VlanPcpCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

import static org.onosproject.fnl.intf.NetworkAnomaly.Type.LOOP;
import static org.onosproject.fnl.base.TsLoopPacket.SetHeaderResult.SETHEADER_FAILURE_NULL;
import static org.onosproject.fnl.base.TsLoopPacket.SetHeaderResult.SETHEADER_OVERRIDE;
import static org.onosproject.fnl.base.TsLoopPacket.SetHeaderResult.SETHEADER_SUCCESS;
import static org.onosproject.net.flow.criteria.Criteria.*;

/**
 * Virtual packet for Default Loop Checking.
 */
public final class TsLoopPacket implements NetworkAnomaly {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String EOL = String.format("%n");
    private static final String LINE =
            EOL + "====================================================" + EOL;
    private static final String HDR_FMT = EOL + "---------- %s ----------" + EOL;
    private static final String LOOP_HEADER = makeHeader("Loop Header");
    private static final String LOOP_FLOW_ENTRIES = makeHeader("Loop Flow Entries");
    private static final String LOOP_LINKS = makeHeader("Loop Links");

    private Map<Criterion.Type, Criterion> match;
    private Stack<FlowEntry> pathFlow;
    // when Upgrade, check to MAKE SURE it include just Link but not EdgeLink
    private Stack<Link> pathLink;

    /**
     * Create an initial virtual packet inside for Loop Checking.
     */
    private TsLoopPacket() {
        match = new HashMap<>();
        pathFlow = new Stack<>();
        pathLink = new Stack<>();
    }

    @Override
    public Type type() {
        return LOOP;
    }

    /**
     * Represents the result of setting a header to virtual packet.
     */
    public enum SetHeaderResult {
        /**
         * Set header successfully.
         */
        SETHEADER_SUCCESS,

        /**
         * Set header successfully but override old value.
         */
        SETHEADER_OVERRIDE,

        /**
         * Fail to set Header because NULL value.
         */
        SETHEADER_FAILURE_NULL,

        /**
         * Fail to set Header, but reason is not defined, defined in advance.
         */
        SETHEADER_FAILURE
    }

    /**
     * Creates and returns a new packet instance with the copied match fields.
     *
     * With hard-copied match fields, references to path flows and path links.
     *
     * @return new loop packet instance with the copied match fields
     */
    public TsLoopPacket copyPacketMatch() {

        TsLoopPacket newOne = new TsLoopPacket();

        newOne.pathFlow = this.pathFlow;
        newOne.pathLink = this.pathLink;

        Map<Criterion.Type, Criterion> m = newOne.match;

        for (Map.Entry<Criterion.Type, Criterion> entry : this.match.entrySet()) {
            Criterion.Type k = entry.getKey();
            Criterion v = entry.getValue();

            switch (k) {
                case IN_PORT:
                    m.put(k, matchInPort(((PortCriterion) v).port()));
                    break;
                case ETH_SRC: // At present, not support Ethernet mask (ONOS?)
                    m.put(k, matchEthSrc(((EthCriterion) v).mac()));
                    break;
                case ETH_DST: // At present, not support Ethernet mask (ONOS?)
                    m.put(k, matchEthDst(((EthCriterion) v).mac()));
                    break;
                case ETH_TYPE:
                    m.put(k, matchEthType(((EthTypeCriterion) v).ethType()));
                    break;
                case VLAN_VID: // At present, not support VLAN mask (ONOS?)
                    m.put(k, matchVlanId(((VlanIdCriterion) v).vlanId()));
                    break;
                case VLAN_PCP:
                    m.put(k, matchVlanPcp(((VlanPcpCriterion) v).priority()));
                    break;
                case IPV4_SRC:
                    m.put(k, matchIPSrc(((IPCriterion) v).ip()));
                    break;
                case IPV4_DST:
                    m.put(k, matchIPDst(((IPCriterion) v).ip()));
                    break;
                case IP_PROTO:
                    m.put(k, matchIPProtocol(((IPProtocolCriterion) v).protocol()));
                    break;
                case IP_DSCP: // can't be supported by now
                    m.put(k, matchIPDscp(((IPDscpCriterion) v).ipDscp()));
                    break;
                case IP_ECN: // can't be supported by now
                    m.put(k, matchIPEcn(((IPEcnCriterion) v).ipEcn()));
                    break;
                case TCP_SRC:
                    m.put(k, matchTcpSrc(((TcpPortCriterion) v).tcpPort()));
                    break;
                case TCP_DST:
                    m.put(k, matchTcpDst(((TcpPortCriterion) v).tcpPort()));
                    break;
                case UDP_SRC:
                    m.put(k, matchUdpSrc(((UdpPortCriterion) v).udpPort()));
                    break;
                case UDP_DST:
                    m.put(k, matchUdpDst(((UdpPortCriterion) v).udpPort()));
                    break;
                default:    //can't be supported by OF1.0
                    log.debug("{} can't be supported by OF1.0", k);
                    break;
            }
        }
        return newOne;
    }

    /**
     * Sets the given criterion as a packet header field.
     *
     * @param criterion as packet header field
     * @return the result of set action
     */
    public SetHeaderResult setHeader(Criterion criterion) {

        if (criterion == null) {
            return SETHEADER_FAILURE_NULL;
        }

        boolean hasKey = match.containsKey(criterion.type());

        match.put(criterion.type(), criterion);

        return hasKey ? SETHEADER_OVERRIDE : SETHEADER_SUCCESS;
    }

    /**
     * Deletes a packet header field by the designated header type.
     *
     * @param criterionType as packet header type
     * @return true, if packet contained the corresponding type of header;
     *         false, otherwise
     */
    public boolean delHeader(Criterion.Type criterionType) {
        return match.remove(criterionType) != null;
    }

    /**
     * Returns a packet header field value by the designated header type.
     *
     * Returns null if the field does not exist.
     *
     * @param criterionType as packet header type
     * @return the packet header field value; may be null
     */
    public Criterion getHeader(Criterion.Type criterionType) {
        return match.get(criterionType);
    }

    /**
     * Returns true if there is the type of header field in the packet.
     *
     * @param criterionType packet header type
     * @return true if the field exists; false otherwise
     */
    public boolean headerExists(Criterion.Type criterionType) {
        return match.containsKey(criterionType);
    }

    /**
     * Pushes the given flow entry onto the path flow stack.
     * Packet matches this entry in specific switch hop.
     *
     * @param entry the matched entry
     */
    public void pushPathFlow(FlowEntry entry) {
        pathFlow.push(entry);
    }

    /**
     * Pops a FlowEntry from path flow entry stack.
     */
    public void popPathFlow() {
        pathFlow.pop();
    }

    /**
     * Returns links in the path which the packet passes through.
     *
     * @return an iterator over the set of links
     */
    public Iterator<Link> getPathLink() {
        return pathLink.iterator();
    }

    /**
     * Adds a Link to path link list.
     * Packet goes through this link between two switches.
     *
     * @param link The link through which the packet go
     */
    public void pushPathLink(Link link) {
        // TODO - need CPY link manual?
        pathLink.push(link);
    }

    /**
     * Removes a Link from path link list.
     */
    public void popPathLink() {
        pathLink.pop();
    }

    /**
     * Returns true if the packet passed through the specific device.
     *
     * @param deviceId identify of the divice to test
     * @return true if packet passed through the specific device;
     *         false otherwise
     */
    public boolean isPassedDevice(DeviceId deviceId) {
        for (Link linkTemp : pathLink) {
            if (deviceId.equals(linkTemp.src().deviceId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the IN_PORT header field of the packet.
     *
     * Attention:
     * IN_PORT field will be changed when packet goes into the next switch hop.
     *
     * @return a port criterion object.
     */
    public PortCriterion getInport() {
        // TODO - check IN_PORT or IN_PHY_PORT
        return (PortCriterion) match.get(Criterion.Type.IN_PORT);
    }

    /**
     * Creates and returns a loop packet instance with given Match Fields.
     *
     * Returns null,
     * whenever SetHeader_FAILURE or SETHEADER_FAILURE_NULL happened.
     *
     * @param criteria match field of one flow entry
     * @param collision as return value;
     *                  true, if criteria contain multiple ones with same type
     * @return a new loop packet instance; may be null
     */
    public static TsLoopPacket matchBuilder(Iterable<Criterion> criteria,
                                            TsReturn<Boolean> collision) {

        if (null != collision) {
            collision.setValue(false);
        }

        TsLoopPacket pkt = new TsLoopPacket();

        for (Criterion criterion : criteria) {

            SetHeaderResult ret = pkt.setHeader(criterion);

            if (SETHEADER_SUCCESS == ret) {
                //TODO - in the future, we may need to resolve this condition
            } else if (SETHEADER_OVERRIDE == ret) {
                if (null != collision) {
                    collision.setValue(true);
                }
            } else { // SetHeader_FAILURE  or SetHeader_FAILURE_NULL
                pkt = null;
                break;
            }
        }

        return pkt;
    }

    /**
     * Hands in the header of virtual packet one by one.
     * Let the header go up through every layer of recursion.
     * It is called when a loop is discovered.
     *
     * @param loopPkt virtual packet that will trigger Loop Storm
     */
    public void handInLoopMatch(TsLoopPacket loopPkt) {
        match = loopPkt.match;
    }

    /**
     * Resets the path link and path flow structures.
     * And initializing the path flow with the gicen flow entry.
     *
     * @param firstEntry the flow entry from which this packet is built
     */
    public void resetLinkFlow(FlowEntry firstEntry) {
        pathLink = new Stack<>();
        pathFlow = new Stack<>();
        pathFlow.push(firstEntry);
    }

    private static String makeHeader(String title) {
        return String.format(HDR_FMT, title);
    }

    /**
     * Returns a multi-line string representation of this loop packet instance.
     *
     * @return formatted string
     */
    @Override
    public String toString() {
        StringBuilder me = new StringBuilder();

        me.append(LINE);

        me.append(LOOP_HEADER);

        List<Criterion> criteria = new ArrayList<>(match.values());
        Collections.sort(criteria, (o1, o2) -> o1.type().compareTo(o2.type()));

        for (Criterion c : criteria) {
            me.append(c).append(EOL);
        }

        me.append(LOOP_FLOW_ENTRIES);

        for (FlowEntry flow : pathFlow) {
            me.append(flow).append(EOL);
        }

        me.append(LOOP_LINKS);

        for (Link l : pathLink) {
            me.append(l).append(EOL);
        }

        return me.toString();
    }
}
