/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.server.devices.nic;

import org.onosproject.net.flow.FlowRule;

import org.onlab.packet.EthType;
import org.onlab.packet.MacAddress;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;

import java.util.Set;

/**
 * Definition of a network interface card (NIC) flow rule.
 */
public interface NicFlowRule extends FlowRule {

    /**
     * Returns parent FlowRule object associated with this NIC rule.
     *
     * @return FlowRule object
     */
    FlowRule flowRule();

    /**
     * Returns the traffic class ID associated with this NIC rule.
     * This ID is related to the software module that processes the
     * traffic matched by this rule.
     *
     * @return traffic class ID
     */
    String trafficClassId();

    /**
     * Returns the NIC's interface name to accommodate this rule.
     *
     * @return NIC's interface name
     */
    String interfaceName();

    /**
     * Returns the NIC's interface number to accommodate this rule.
     *
     * @return NIC's interface number
     */
    long interfaceNumber();

    /**
     * Returns the CPU core index that handles the traffic matched
     * by this NIC rule.
     *
     * @return CPU core index
     */
    long cpuCoreIndex();

    /**
     * Returns the scope of this rule.
     *
     * @return rule scope
     */
    NicRuleScope scope();

    /**
     * Returns the Ethernet type field of this rule
     * or NULL if no Ethernet type exists in the traffic selector.
     *
     * @return Ethernet type field
     */
    EthType ethernetType();

    /**
     * Returns the Ethernet type value field of this rule
     * or negative if no Ethernet type value exists in the traffic selector.
     *
     * @return Ethernet type value field
     */
    short ethernetTypeValue();

    /**
     * Returns the source MAC address field of this rule
     * or NULL if no source MAC address exists in the traffic selector.
     *
     * @return source MAC address field
     */
    MacAddress ethernetSrcAddress();

    /**
     * Returns the destination MAC address field of this rule
     * or NULL if no destination MAC address exists in the traffic selector.
     *
     * @return destination MAC address field
     */
    MacAddress ethernetDstAddress();

    /**
     * Returns whether Ethernet fields are present in this rule.
     *
     * @return boolean Ethernet fields' presence
     */
    boolean hasEthernet();

    /**
     * Returns the IP protocol field of this rule
     * or negative if no IP protocol field exists in the traffic selector.
     *
     * @return IP protocol field
     */
    short ipv4Protocol();

    /**
     * Returns the source IP address field of this rule
     * or null if no source IP address field exists in the traffic selector.
     *
     * @return source IP address field
     */
    Ip4Address ipv4SrcAddress();

    /**
     * Returns the source IP mask field of this rule
     * or null if no source IP mask exists in the traffic selector.
     *
     * @return source IP mask field
     */
    Ip4Prefix ipv4SrcMask();

    /**
     * Returns the destination IP address field of this rule
     * or null if no destination IP address field exists in the traffic selector.
     *
     * @return destination IP address field
     */
    Ip4Address ipv4DstAddress();

    /**
     * Returns the destination IP mask field of this rule
     * or null if no destination IP mask exists in the traffic selector.
     *
     * @return destination IP mask field
     */
    Ip4Prefix ipv4DstMask();

    /**
     * Returns whether IPv4 fields are present in this rule.
     *
     * @return boolean IPv4 fields' presence
     */
    boolean hasIpv4();

    /**
     * Returns whether IPv6 fields are present in this rule.
     *
     * @return boolean IPv6 fields' presence
     */
    boolean hasIpv6();

    /**
     * Returns the source port field of this rule
     * or negative if no source port field exists in the traffic selector.
     *
     * @return source port field
     */
    int sourcePort();

    /**
     * Returns the destination port field of this rule
     * or negative if no destination port field exists in the traffic selector.
     *
     * @return destination port field
     */
    int destinationPort();

    /**
     * Returns whether UDP header fields are present in this rule.
     *
     * @return boolean UDP header fields' presence
     */
    boolean hasUdp();

    /**
     * Returns whether TCP header fields are present in this rule.
     *
     * @return boolean TCP header fields' presence
     */
    boolean hasTcp();

    /**
     * Returns whether transport header fields are present in this rule.
     *
     * @return boolean transport header fields' presence
     */
    boolean hasTransport();

    /**
     * Returns whether this rule is a full wildcard or not.
     *
     * @return boolean full wildcard status
     */
    boolean isFullWildcard();

    /**
     * Returns the set of actions of this rule.
     *
     * @return rule's set of actions
     */
    Set<NicRuleAction> actions();

    /**
     * Adds a new action to the set of actions of this rule.
     *
     * @param action rule action
     */
    void addAction(NicRuleAction action);

    /**
     * Returns the queue number of this rule's action (if any).
     *
     * @return rule action's queue number
     */
    long queueNumber();

    /**
     * Creates rule using NIC-specific rule format.
     *
     * @return rule creation command as a string
     */
    abstract String createRule();

    /**
     * Removes rule using NIC-specific rule format.
     *
     * @return rule removal command as a string
     */
    abstract String removeRule();

    /**
     * Forms a NIC rule body.
     *
     * @return rule body as a string
     */
    abstract String ruleBody();

    /**
     * A builder for NIC rules.
     */
    interface Builder {

        /**
         * Creates a NIC rule out of an ONOS FlowRule.
         *
         * @param flowRule an ONOS FlowRule object
         * @return this
         */
        Builder fromFlowRule(FlowRule flowRule);

        /**
         * Creates a NIC rule with a given traffic class ID.
         *
         * @param trafficClassId a traffic class ID
         * @return this
         */
        Builder withTrafficClassId(String trafficClassId);

        /**
         * Sets the interface name for this NIC rule.
         *
         * @param interfaceName an interface's name
         * @return this
         */
        Builder withInterfaceName(String interfaceName);

        /**
         * Sets the interface number for this NIC rule.
         *
         * @param interfaceNumber an interface's number
         * @return this
         */
        Builder withInterfaceNumber(long interfaceNumber);

        /**
         * Sets the CPU core index to accommodate the traffic
         * matched by this NIC rule.
         *
         * @param cpuCoreIndex a CPU core index
         * @return this
         */
        Builder assignedToCpuCore(long cpuCoreIndex);

        /**
         * Sets the scope for this NIC rule.
         *
         * @param scope an NIC rule scope
         * @return this
         */
        Builder withScope(NicRuleScope scope);

        /**
         * Builds a NIC rule object.
         *
         * @return a NIC rule.
         */
        abstract NicFlowRule build();

    }

}
