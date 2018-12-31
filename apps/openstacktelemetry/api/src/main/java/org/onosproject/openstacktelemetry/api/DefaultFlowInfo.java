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
package org.onosproject.openstacktelemetry.api;

import org.onlab.packet.IPv4;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation class of FlowInfo.
 */
public final class DefaultFlowInfo implements FlowInfo {
    private static final String INGRESS_STATS = "Ingress Port :";
    private static final String EGRESS_STATS = "Egress Port :";

    private static final String PROTOCOL_NAME_TCP = "tcp";
    private static final String PROTOCOL_NAME_UDP = "udp";
    private static final String PROTOCOL_NAME_ANY = "any";
    private static final int ARBITRARY_PROTOCOL = 0x0;

    private final byte flowType;
    private final DeviceId deviceId;
    private final int inputInterfaceId;
    private final int outputInterfaceId;
    private final VlanId vlanId;
    private final short vxlanId;
    private final IpPrefix srcIp;
    private final IpPrefix dstIp;
    private final TpPort srcPort;
    private final TpPort dstPort;
    private final byte protocol;
    private final MacAddress srcMac;
    private final MacAddress dstMac;
    private final StatsInfo statsInfo;

    private DefaultFlowInfo(byte flowType, DeviceId deviceId,
                            int inputInterfaceId, int outputInterfaceId,
                            VlanId vlanId, short vxlanId, IpPrefix srcIp,
                            IpPrefix dstIp, TpPort srcPort, TpPort dstPort,
                            byte protocol, MacAddress srcMac, MacAddress dstMac,
                            StatsInfo statsInfo) {
        this.flowType = flowType;
        this.deviceId = deviceId;
        this.inputInterfaceId = inputInterfaceId;
        this.outputInterfaceId = outputInterfaceId;
        this.vlanId = vlanId;
        this.vxlanId = vxlanId;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.protocol = protocol;
        this.srcMac = srcMac;
        this.dstMac = dstMac;
        this.statsInfo = statsInfo;
    }

    @Override
    public byte flowType() {
        return flowType;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public int inputInterfaceId() {
        return inputInterfaceId;
    }

    @Override
    public int outputInterfaceId() {
        return outputInterfaceId;
    }

    @Override
    public VlanId vlanId() {
        return vlanId;
    }

    @Override
    public short vxlanId() {
        return vxlanId;
    }

    @Override
    public IpPrefix srcIp() {
        return srcIp;
    }

    @Override
    public IpPrefix dstIp() {
        return dstIp;
    }

    @Override
    public TpPort srcPort() {
        return srcPort;
    }

    @Override
    public TpPort dstPort() {
        return dstPort;
    }

    @Override
    public byte protocol() {
        return protocol;
    }

    @Override
    public MacAddress srcMac() {
        return srcMac;
    }

    @Override
    public MacAddress dstMac() {
        return dstMac;
    }

    @Override
    public StatsInfo statsInfo() {
        return statsInfo;
    }

    @Override
    public boolean roughEquals(FlowInfo flowInfo) {
        final DefaultFlowInfo other = (DefaultFlowInfo) flowInfo;
        return Objects.equals(this.deviceId, other.deviceId) &&
                Objects.equals(this.srcIp, other.srcIp) &&
                Objects.equals(this.dstIp, other.dstIp) &&
                Objects.equals(this.srcPort, other.srcPort) &&
                Objects.equals(this.dstPort, other.dstPort) &&
                Objects.equals(this.protocol, other.protocol);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultFlowInfo) {
            final DefaultFlowInfo other = (DefaultFlowInfo) obj;
            return Objects.equals(this.flowType, other.flowType) &&
                    Objects.equals(this.deviceId, other.deviceId) &&
                    Objects.equals(this.inputInterfaceId, other.inputInterfaceId) &&
                    Objects.equals(this.outputInterfaceId, other.outputInterfaceId) &&
                    Objects.equals(this.vlanId, other.vlanId) &&
                    Objects.equals(this.vxlanId, other.vxlanId) &&
                    Objects.equals(this.srcIp, other.srcIp) &&
                    Objects.equals(this.dstIp, other.dstIp) &&
                    Objects.equals(this.srcPort, other.srcPort) &&
                    Objects.equals(this.dstPort, other.dstPort) &&
                    Objects.equals(this.protocol, other.protocol) &&
                    Objects.equals(this.srcMac, other.srcMac) &&
                    Objects.equals(this.dstMac, other.dstMac) &&
                    Objects.equals(this.statsInfo, other.statsInfo);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowType, deviceId, inputInterfaceId,
                outputInterfaceId, vlanId, vxlanId, srcIp, dstIp, srcPort, dstPort,
                protocol, srcMac, dstMac, statsInfo);
    }

    @Override
    public String uniqueFlowInfoKey() {
        if (srcIp.address().isZero() || dstIp.address().isZero()) {
            if (!srcIp.address().isZero()) {
                return INGRESS_STATS + srcIp.toString();
            }
            if (!dstIp.address().isZero()) {
                return EGRESS_STATS + dstIp.toString();
            }
        }
        return srcIp.toString() + ":" +
                ((srcPort == null) ? "any" : srcPort.toString()) +
                " -> " +
                dstIp.toString() + ":" +
                ((dstPort == null) ? "any" : dstPort.toString()) +
                " (" + getProtocolNameFromType(protocol) + ")";
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("flowType", flowType)
                .add("deviceId", deviceId)
                .add("inputInterfaceId", inputInterfaceId)
                .add("outputInterfaceId", outputInterfaceId)
                .add("vlanId", vlanId)
                .add("vxlanId", vxlanId)
                .add("srcIp", srcIp)
                .add("dstIp", dstIp)
                .add("srcPort", srcPort)
                .add("dstPort", dstPort)
                .add("protocol", protocol)
                .add("srcMac", srcMac)
                .add("dstMac", dstMac)
                .add("statsInfo", statsInfo)
                .toString();
    }

    /**
     * Obtains a default flow info builder object.
     *
     * @return flow info builder object
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    /**
     * Builder class of DefaultFlowInfo.
     */
    public static final class DefaultBuilder implements FlowInfo.Builder {

        private byte flowType;
        private DeviceId deviceId;
        private int inputInterfaceId;
        private int outputInterfaceId;
        private VlanId vlanId;
        private short vxlanId;
        private IpPrefix srcIp;
        private IpPrefix dstIp;
        private TpPort srcPort;
        private TpPort dstPort;
        private byte protocol;
        private MacAddress srcMac;
        private MacAddress dstMac;
        private StatsInfo statsInfo;

        @Override
        public Builder withFlowType(byte flowType) {
            this.flowType = flowType;
            return this;
        }

        @Override
        public Builder withDeviceId(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @Override
        public Builder withInputInterfaceId(int inputInterfaceId) {
            this.inputInterfaceId = inputInterfaceId;
            return this;
        }

        @Override
        public Builder withOutputInterfaceId(int outputInterfaceId) {
            this.outputInterfaceId = outputInterfaceId;
            return this;
        }

        @Override
        public Builder withVlanId(VlanId vlanId) {
            this.vlanId = vlanId;
            return this;
        }

        @Override
        public Builder withVxlanId(short vxlanId) {
            this.vxlanId = vxlanId;
            return this;
        }

        @Override
        public Builder withSrcIp(IpPrefix srcIp) {
            this.srcIp = srcIp;
            return this;
        }

        @Override
        public Builder withDstIp(IpPrefix dstIp) {
            this.dstIp = dstIp;
            return this;
        }

        @Override
        public Builder withSrcPort(TpPort srcPort) {
            this.srcPort = srcPort;
            return this;
        }

        @Override
        public Builder withDstPort(TpPort dstPort) {
            this.dstPort = dstPort;
            return this;
        }

        @Override
        public Builder withProtocol(byte protocol) {
            this.protocol = protocol;
            return this;
        }

        @Override
        public Builder withSrcMac(MacAddress srcMac) {
            this.srcMac = srcMac;
            return this;
        }

        @Override
        public Builder withDstMac(MacAddress dstMac) {
            this.dstMac = dstMac;
            return this;
        }

        @Override
        public Builder withStatsInfo(StatsInfo statsInfo) {
            this.statsInfo = statsInfo;
            return this;
        }

        @Override
        public FlowInfo build() {

            // TODO: need to check the null value for more properties
            checkNotNull(srcIp, "Source IP address cannot be null");
            checkNotNull(dstIp, "Destination IP address cannot be null");
            checkNotNull(statsInfo, "StatsInfo cannot be null");

            return new DefaultFlowInfo(flowType, deviceId, inputInterfaceId,
                    outputInterfaceId, vlanId, vxlanId, srcIp, dstIp, srcPort, dstPort,
                    protocol, srcMac, dstMac, statsInfo);
        }
    }


    /**
     * Obtains protocol name from the protocol type.
     *
     * @param type transport protocol type
     * @return transport protocol name
     */
    private String getProtocolNameFromType(byte type) {
        switch (type) {
            case IPv4.PROTOCOL_TCP:
                return PROTOCOL_NAME_TCP;
            case IPv4.PROTOCOL_UDP:
                return PROTOCOL_NAME_UDP;
            case ARBITRARY_PROTOCOL:
                return PROTOCOL_NAME_ANY;
            default:
                return PROTOCOL_NAME_ANY;
        }
    }
}
