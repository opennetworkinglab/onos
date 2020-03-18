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

import org.onlab.packet.MacAddress;

/**
 * Implementation of a DPDK-based network interface card (NIC) flow rule.
 */
public class DefaultDpdkNicFlowRule extends DefaultNicFlowRule {

    public DefaultDpdkNicFlowRule(
            FlowRule flowRule,
            String trafficClassId,
            String interfaceName,
            long interfaceNumber,
            long cpuCoreIndex,
            NicRuleScope scope) {
        super(flowRule, trafficClassId, interfaceName,
            interfaceNumber, cpuCoreIndex, scope);
    }

    @Override
    public String createRule() {
        return "flow create " + Long.toString(this.interfaceNumber) + " " + ruleBody();
    }

    @Override
    public String removeRule() {
        return "flow destroy " + Long.toString(this.interfaceNumber) + " rule " + id().toString();
    }

    @Override
    public String ruleBody() {
        String rule = "";

        rule += this.scope() + " pattern ";

        if (this.hasEthernet()) {
            rule += "eth ";

            if (this.ethernetType() != null) {
                rule += "type is " + Integer.toString(this.ethernetTypeValue()) + " ";
            }

            if (this.ethernetSrcAddress() != null) {
                rule += "src spec " + this.ethernetSrcAddress().toString() + " ";
                rule += "src mask " + MacAddress.BROADCAST.toString() + " ";
            }

            if (this.ethernetDstAddress() != null) {
                rule += "dst spec " + this.ethernetDstAddress().toString() + " ";
                rule += "dst mask " + MacAddress.BROADCAST.toString() + " ";
            }

            rule += "/ ";
        }

        if (this.hasIpv4()) {
            rule += "ipv4 ";

            if (this.ipv4Protocol() > 0) {
                rule += "proto is " + Integer.toString(this.ipv4Protocol()) + " ";
            }

            if (this.ipv4SrcAddress() != null) {
                if ((this.ipv4SrcMask() != null) && (this.ipv4SrcMask().prefixLength() < 32)) {
                    rule += "src spec " + this.ipv4SrcAddress().toString() + " ";
                    rule += "src prefix " + this.ipv4SrcMask().prefixLength() + " ";
                } else {
                    rule += "src is " + this.ipv4SrcAddress().toString() + " ";
                }
            }

            if (this.ipv4DstAddress() != null) {
                if ((this.ipv4DstMask() != null) && (this.ipv4DstMask().prefixLength() < 32)) {
                    rule += "dst spec " + this.ipv4DstAddress().toString() + " ";
                    rule += "dst prefix " + this.ipv4DstMask().prefixLength() + " ";
                } else {
                    rule += "dst is " + this.ipv4DstAddress().toString() + " ";
                }
            }

            rule += "/ ";
        }

        if (this.hasTransport()) {

            if (this.hasUdp()) {
                rule += "udp ";
            } else if (this.hasTcp()) {
                rule += "tcp ";
            }

            if (this.sourcePort() > 0) {
                rule += "src is " + Integer.toString(this.sourcePort()) + " ";
            }

            if (this.destinationPort() > 0) {
                rule += "dst is " + Integer.toString(this.destinationPort()) + " ";
            }

            rule += "/ ";
        }

        rule += "end ";

        if (this.actions() != null) {
            rule += "actions ";

            for (NicRuleAction action : this.actions()) {
                rule += action.actionType().toString() + " ";

                // No subsequent field
                if (action.actionField().isEmpty()) {
                    rule += "/ ";
                    continue;
                }

                // A subsequent field is associated with a value
                rule += action.actionField() + " ";
                rule += Long.toString(action.actionValue()) + " / ";
            }

            rule += " end";
        }

        return rule;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * Returns a DPDK NIC rule builder.
     *
     * @return builder
     */
    public static Builder nicRulebuilder() {
        return new Builder();
    }

    /**
     * Default DPDK NIC rule builder.
     */
    public static final class Builder extends DefaultNicFlowRule.Builder {

        public Builder() {
            super();
        }

        @Override
        public NicFlowRule build() {
            return new DefaultDpdkNicFlowRule(
                flowRule, trafficClassId, interfaceName, interfaceNumber,
                cpuCoreIndex, scope);
        }

    }

}
