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

import com.google.common.base.Strings;

import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.IPProtocolCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;

import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.SetQueueInstruction;
import org.onosproject.net.flow.instructions.Instructions.MeterInstruction;

import org.onlab.packet.EthType;
import org.onlab.packet.MacAddress;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;

import org.slf4j.Logger;

import java.util.Objects;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_RULE_CORE_ID_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_RULE_IFACE_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_RULE_IFACE_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_RULE_SCOPE_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_RULE_TC_ID_NULL;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_TYPE;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.ETH_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.IP_PROTO;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.IPV4_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.UDP_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.UDP_DST;
import static org.onosproject.net.flow.criteria.Criterion.Type.TCP_SRC;
import static org.onosproject.net.flow.criteria.Criterion.Type.TCP_DST;
import static org.onosproject.net.flow.instructions.Instruction.Type.NOACTION;
import static org.onosproject.net.flow.instructions.Instruction.Type.QUEUE;
import static org.onosproject.net.flow.instructions.Instruction.Type.METER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of an abstract NIC rule.
 */
public abstract class DefaultNicFlowRule extends DefaultFlowRule implements NicFlowRule {

    protected static final Logger log = getLogger(DefaultNicFlowRule.class);

    // Additional members that a NIC rule requires
    protected final String trafficClassId;
    protected final String interfaceName;
    protected long interfaceNumber;
    protected long cpuCoreIndex;
    protected final NicRuleScope scope;

    // Derive from FlowRule's selector
    protected EthTypeCriterion ethTypeCriterion;
    protected EthCriterion ethSrcAddrCriterion;
    protected EthCriterion ethDstAddrCriterion;

    protected IPProtocolCriterion ipv4ProtoCriterion;
    protected IPCriterion ipv4SrcAddrCriterion;
    protected IPCriterion ipv4SrcMaskCriterion;
    protected IPCriterion ipv4DstAddrCriterion;
    protected IPCriterion ipv4DstMaskCriterion;

    protected UdpPortCriterion udpSrcPortCriterion;
    protected UdpPortCriterion udpDstPortCriterion;
    protected TcpPortCriterion tcpSrcPortCriterion;
    protected TcpPortCriterion tcpDstPortCriterion;

    // Derives from FlowRule's treatment
    protected Set<NicRuleAction> actions;

    protected DefaultNicFlowRule(
            FlowRule     flowRule,
            String       trafficClassId,
            String       interfaceName,
            long         interfaceNumber,
            long         cpuCoreIndex,
            NicRuleScope scope) {
        super(flowRule);

        checkArgument(!Strings.isNullOrEmpty(trafficClassId), MSG_NIC_FLOW_RULE_TC_ID_NULL);
        checkNotNull(interfaceName, MSG_NIC_FLOW_RULE_IFACE_NULL);
        checkArgument(interfaceNumber >= 0, MSG_NIC_FLOW_RULE_IFACE_NEGATIVE);
        checkArgument(cpuCoreIndex >= 0, MSG_NIC_FLOW_RULE_CORE_ID_NEGATIVE);
        checkNotNull(scope, MSG_NIC_FLOW_RULE_SCOPE_NULL);

        this.trafficClassId = trafficClassId;
        this.interfaceName = interfaceName;
        this.interfaceNumber = interfaceNumber;
        this.cpuCoreIndex = cpuCoreIndex;
        this.scope = scope;

        this.populate();
    }

    protected DefaultNicFlowRule(FlowRule flowRule) {
        super(flowRule);

        this.trafficClassId = Builder.DEFAULT_TRAFFIC_CLASS_ID;
        this.interfaceName = "";
        this.interfaceNumber = Builder.DEFAULT_INTERFACE_NB;
        this.cpuCoreIndex = Builder.DEFAULT_CPU_CORE_INDEX;
        this.scope = Builder.DEFAULT_RULE_SCOPE;

        this.populate();
    }

    /**
     * Parses FlowRule's traffic selector and treatment
     * and keeps relevant information for this NIC rule.
     */
    private void populate() {
        this.ethTypeCriterion = (EthTypeCriterion) this.selector().getCriterion(ETH_TYPE);
        this.ethSrcAddrCriterion = (EthCriterion) this.selector().getCriterion(ETH_SRC);
        this.ethDstAddrCriterion = (EthCriterion) this.selector().getCriterion(ETH_DST);
        this.ipv4ProtoCriterion = (IPProtocolCriterion) this.selector().getCriterion(IP_PROTO);
        this.ipv4SrcAddrCriterion = (IPCriterion) this.selector().getCriterion(IPV4_SRC);
        this.ipv4DstAddrCriterion = (IPCriterion) this.selector().getCriterion(IPV4_DST);
        this.ipv4SrcMaskCriterion = (IPCriterion) this.selector().getCriterion(IPV4_SRC);
        this.ipv4DstMaskCriterion = (IPCriterion) this.selector().getCriterion(IPV4_DST);
        this.udpSrcPortCriterion = (UdpPortCriterion) this.selector().getCriterion(UDP_SRC);
        this.udpDstPortCriterion = (UdpPortCriterion) this.selector().getCriterion(UDP_DST);
        this.tcpSrcPortCriterion = (TcpPortCriterion) this.selector().getCriterion(TCP_SRC);
        this.tcpDstPortCriterion = (TcpPortCriterion) this.selector().getCriterion(TCP_DST);

        this.actions = new HashSet<NicRuleAction>();
        // TODO: Expand this translator with more actions
        for (Instruction instr : this.treatment().allInstructions()) {
            if (instr.type() == NOACTION) {
                this.actions.add(new NicRuleAction(NicRuleAction.Action.DROP));
            } else if (instr.type() == QUEUE) {
                SetQueueInstruction queueInstruction = (SetQueueInstruction) instr;
                this.actions.add(
                    new NicRuleAction(NicRuleAction.Action.QUEUE, queueInstruction.queueId()));
                this.interfaceNumber = queueInstruction.port().toLong();
            } else if (instr.type() == METER) {
                MeterInstruction meterInstruction = (MeterInstruction) instr;
                this.actions.add(
                    new NicRuleAction(NicRuleAction.Action.METER, meterInstruction.meterId().id()));
            }
        }

        // This action provides basic rule match counters
        this.actions.add(new NicRuleAction(NicRuleAction.Action.COUNT));
    }

    @Override
    public FlowRule flowRule() {
        return this;
    }

    @Override
    public String trafficClassId() {
        return trafficClassId;
    }

    @Override
    public String interfaceName() {
        return interfaceName;
    }

    @Override
    public long interfaceNumber() {
        return interfaceNumber;
    }

    @Override
    public long cpuCoreIndex() {
        return cpuCoreIndex;
    }

    @Override
    public NicRuleScope scope() {
        return scope;
    }

    @Override
    public EthType ethernetType() {
        return (ethTypeCriterion != null) ?
            ethTypeCriterion.ethType() : null;
    }

    @Override
    public short ethernetTypeValue() {
        return (ethTypeCriterion != null) ?
            ethTypeCriterion.ethType().toShort() : -1;
    }

    @Override
    public MacAddress ethernetSrcAddress() {
        return (ethSrcAddrCriterion != null) ?
            ethSrcAddrCriterion.mac() : null;
    }

    @Override
    public MacAddress ethernetDstAddress() {
        return (ethDstAddrCriterion != null) ?
            ethDstAddrCriterion.mac() : null;
    }

    @Override
    public boolean hasEthernet() {
        return (ethernetType() != null) ||
               (ethernetSrcAddress() != null) ||
               (ethernetDstAddress() != null);
    }

    @Override
    public short ipv4Protocol() {
        return (ipv4ProtoCriterion != null) ?
            ipv4ProtoCriterion.protocol() : -1;
    }

    @Override
    public Ip4Address ipv4SrcAddress() {
        return (ipv4SrcAddrCriterion != null) ?
            ipv4SrcAddrCriterion.ip().address().getIp4Address() : null;
    }

    @Override
    public Ip4Prefix ipv4SrcMask() {
        return (ipv4SrcMaskCriterion != null) ?
            ipv4SrcMaskCriterion.ip().getIp4Prefix() : null;
    }

    @Override
    public Ip4Address ipv4DstAddress() {
        return (ipv4DstAddrCriterion != null) ?
            ipv4DstAddrCriterion.ip().address().getIp4Address() : null;
    }

    @Override
    public Ip4Prefix ipv4DstMask() {
        return (ipv4DstMaskCriterion != null) ?
            ipv4DstMaskCriterion.ip().getIp4Prefix() : null;
    }

    @Override
    public boolean hasIpv4() {
        return (ipv4Protocol() > 0) ||
               (ipv4SrcAddress() != null) ||
               (ipv4DstAddress() != null);
    }

    @Override
    public boolean hasIpv6() {
        return false;
    }

    @Override
    public int sourcePort() {
        return ((udpSrcPortCriterion != null) ?
                 udpSrcPortCriterion.udpPort().toInt() :
                    ((tcpSrcPortCriterion != null) ?
                      tcpSrcPortCriterion.tcpPort().toInt() : -1));
    }

    @Override
    public int destinationPort() {
        return ((udpDstPortCriterion != null) ?
                 udpDstPortCriterion.udpPort().toInt() :
                    ((tcpDstPortCriterion != null) ?
                      tcpDstPortCriterion.tcpPort().toInt() : -1));
    }

    @Override
    public boolean hasUdp() {
        return (udpSrcPortCriterion != null) || (udpDstPortCriterion != null);
    }

    @Override
    public boolean hasTcp() {
        return (tcpSrcPortCriterion != null) || (tcpDstPortCriterion != null);
    }

    @Override
    public boolean hasTransport() {
        return (sourcePort() > 0) || (destinationPort() > 0);
    }

    @Override
    public boolean isFullWildcard() {
        if (((ipv4SrcAddress() != null) && !ipv4SrcAddress().isZero()) ||
            ((ipv4DstAddress() != null) && !ipv4DstAddress().isZero()) ||
            (ipv4Protocol() > 0) || (sourcePort() > 0) || (destinationPort() > 0)) {
            return false;
        }
        return true;
    }

    @Override
    public Set<NicRuleAction> actions() {
        return actions;
    }

    @Override
    public void addAction(NicRuleAction action) {
        checkNotNull(action, "Cannot add a NULL NIC rule action");
        if (actions == null) {
            actions = new HashSet<NicRuleAction>();
        }
        actions.add(action);
    }

    @Override
    public long queueNumber() {
        for (NicRuleAction action : actions) {
            if (action.actionType() == NicRuleAction.Action.QUEUE) {
                return action.actionValue();
            }
        }

        return (long) -1;
    }

    @Override
    public abstract String createRule();

    @Override
    public abstract String removeRule();

    @Override
    public abstract String ruleBody();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultNicFlowRule) {
            DefaultNicFlowRule that = (DefaultNicFlowRule) obj;
            return  Objects.equals(trafficClassId, that.trafficClassId) &&
                    Objects.equals(interfaceName, that.interfaceName) &&
                    Objects.equals(interfaceNumber, that.interfaceNumber) &&
                    Objects.equals(cpuCoreIndex, that.cpuCoreIndex) &&
                    Objects.equals(scope, that.scope) &&
                    Objects.equals(actions, that.actions) &&
                    Objects.equals(deviceId(), that.deviceId()) &&
                    Objects.equals(id(), that.id()) &&
                    Objects.equals(selector(), that.selector());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            trafficClassId, interfaceName, interfaceNumber,
            cpuCoreIndex, scope, deviceId(), id(), selector()
        );
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("Device ID", deviceId())
                .add("Traffic class ID", trafficClassId())
                .add("Flow ID", id())
                .add("Interface name", interfaceName())
                .add("Interface number", interfaceNumber())
                .add("CPU core index", cpuCoreIndex())
                .add("Scope", scope())
                .add("Traffic selector", selector())
                .add("Traffic treatment", treatment())
                .add("Priority", priority())
                .add("Ethernet type", ethernetTypeValue())
                .add("Ethernet address source", ethernetSrcAddress())
                .add("Ethernet address destination", ethernetDstAddress())
                .add("IP protocol", ipv4Protocol())
                .add("IP address source", ipv4SrcAddress())
                .add("IP mask source", ipv4SrcMask())
                .add("IP address destination", ipv4DstAddress())
                .add("IP mask destination", ipv4DstMask())
                .add("Source port", sourcePort())
                .add("Destination port", destinationPort())
                .add("Actions", actions())
                .toString();
    }


    public abstract static class Builder implements NicFlowRule.Builder {

        protected FlowRule flowRule;

        protected String trafficClassId;
        protected String interfaceName;
        protected long interfaceNumber;
        protected long cpuCoreIndex;
        protected NicRuleScope scope;

        protected EthTypeCriterion ethTypeCriterion;
        protected EthCriterion ethSrcAddrCriterion;
        protected EthCriterion ethDstAddrCriterion;

        protected IPProtocolCriterion ipv4ProtoCriterion;
        protected IPCriterion ipv4SrcAddrCriterion;
        protected IPCriterion ipv4SrcMaskCriterion;
        protected IPCriterion ipv4DstAddrCriterion;
        protected IPCriterion ipv4DstMaskCriterion;

        protected UdpPortCriterion udpSrcPortCriterion;
        protected UdpPortCriterion udpDstPortCriterion;
        protected TcpPortCriterion tcpSrcPortCriterion;
        protected TcpPortCriterion tcpDstPortCriterion;

        protected Set<NicRuleAction> actions;

        protected static final String DEFAULT_TRAFFIC_CLASS_ID = "tc:00001";
        protected static final long DEFAULT_INTERFACE_NB = (long) 0;
        protected static final long DEFAULT_CPU_CORE_INDEX = (long) 0;
        protected static final NicRuleScope DEFAULT_RULE_SCOPE =
            NicRuleScope.INGRESS;

        protected Builder() {
            this.flowRule = null;

            this.trafficClassId = null;
            this.interfaceName = "";
            this.interfaceNumber = DEFAULT_INTERFACE_NB;
            this.cpuCoreIndex = DEFAULT_CPU_CORE_INDEX;
            this.scope = DEFAULT_RULE_SCOPE;

            this.ethTypeCriterion = null;
            this.ethSrcAddrCriterion = null;
            this.ethDstAddrCriterion = null;
            this.ipv4ProtoCriterion = null;
            this.ipv4SrcAddrCriterion = null;
            this.ipv4SrcMaskCriterion = null;
            this.ipv4DstAddrCriterion = null;
            this.ipv4DstMaskCriterion = null;
            this.udpSrcPortCriterion = null;
            this.udpDstPortCriterion = null;
            this.tcpSrcPortCriterion = null;
            this.tcpDstPortCriterion = null;

            this.actions = new HashSet<NicRuleAction>();
        }

        @Override
        public Builder fromFlowRule(FlowRule flowRule) {
            this.flowRule = flowRule;
            return this;
        }

        @Override
        public Builder withTrafficClassId(String trafficClassId) {
            this.trafficClassId = trafficClassId;
            return this;
        }

        @Override
        public Builder withInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
            return this;
        }

        @Override
        public Builder withInterfaceNumber(long interfaceNumber) {
            this.interfaceNumber = interfaceNumber;
            return this;
        }

        @Override
        public Builder assignedToCpuCore(long cpuCoreIndex) {
            this.cpuCoreIndex = cpuCoreIndex;
            return this;
        }

        @Override
        public Builder withScope(NicRuleScope scope) {
            this.scope = scope;
            return this;
        }

        @Override
        public abstract NicFlowRule build();

    }

}
