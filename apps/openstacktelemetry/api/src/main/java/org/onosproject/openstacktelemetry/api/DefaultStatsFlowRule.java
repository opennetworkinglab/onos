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

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Implementation of StatsFlowRule.
 */
public final class DefaultStatsFlowRule implements StatsFlowRule {
    private final IpPrefix srcIpPrefix;
    private final IpPrefix dstIpPrefix;
    private final byte     ipProtocol;
    private final TpPort   srcTpPort;
    private final TpPort   dstTpPort;

    private static final String NOT_NULL_MSG = "Element % cannot be null";

    protected DefaultStatsFlowRule(IpPrefix srcIpPrefix,
                                   IpPrefix dstIpPrefix,
                                   byte ipProtoco,
                                   TpPort srcTpPort,
                                   TpPort dstTpPort) {
        this.srcIpPrefix = srcIpPrefix;
        this.dstIpPrefix = dstIpPrefix;
        this.ipProtocol  = ipProtoco;
        this.srcTpPort   = srcTpPort;
        this.dstTpPort   = dstTpPort;
    }

    @Override
    public IpPrefix srcIpPrefix() {
        return srcIpPrefix;
    }

    @Override
    public IpPrefix dstIpPrefix() {
        return dstIpPrefix;
    }

    @Override
    public byte ipProtocol() {
        return ipProtocol;
    }

    @Override
    public TpPort srcTpPort() {
        return srcTpPort;
    }

    @Override
    public TpPort dstTpPort() {
        return dstTpPort;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("srcIpPrefix", srcIpPrefix)
                .add("dstIpPrefix", dstIpPrefix)
                .add("ipProtocol", ipProtocol)
                .add("srcTpPort", srcTpPort)
                .add("dstTpPort", dstTpPort)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultStatsFlowRule) {
            final DefaultStatsFlowRule other = (DefaultStatsFlowRule) obj;
            return Objects.equals(this.srcIpPrefix, other.srcIpPrefix) &&
                    Objects.equals(this.dstIpPrefix, other.dstIpPrefix) &&
                    Objects.equals(this.srcTpPort, other.srcTpPort) &&
                    Objects.equals(this.dstTpPort, other.dstTpPort) &&
                    Objects.equals(this.ipProtocol, other.ipProtocol);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcIpPrefix, dstIpPrefix, srcTpPort, dstTpPort, ipProtocol);
    }

    /**
     * Obtains a default stats flow rule builder object.
     *
     * @return flow rule builder object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Obtains the builder object existing flow rule.
     *
     * @param flowRule flow rule
     * @return builder object
     */
    public static Builder from(StatsFlowRule flowRule) {
        return new Builder()
                .srcIpPrefix(flowRule.srcIpPrefix())
                .dstIpPrefix(flowRule.dstIpPrefix())
                .ipProtocol(flowRule.ipProtocol())
                .srcTpPort(flowRule.srcTpPort())
                .dstTpPort(flowRule.dstTpPort());
    }

    /**
     * A builder class for openstack flow rule.
     */
    public static final class Builder implements StatsFlowRule.Builder {
        private IpPrefix srcIpPrefix;
        private IpPrefix dstIpPrefix;
        private byte     ipProtocol;
        private TpPort   srcTpPort;
        private TpPort   dstTpPort;

        // private constructor not intended to use from external
        private Builder() {
        }

        @Override
        public DefaultStatsFlowRule build() {
            checkArgument(srcIpPrefix != null, NOT_NULL_MSG, "Source IP Prefix");
            checkArgument(dstIpPrefix != null, NOT_NULL_MSG, "Destination IP Prefix");

            return new DefaultStatsFlowRule(srcIpPrefix,
                                                dstIpPrefix,
                                                ipProtocol,
                                                srcTpPort,
                                                dstTpPort);
        }

        @Override
        public Builder srcIpPrefix(IpPrefix srcIpPrefix) {
            this.srcIpPrefix = srcIpPrefix;
            return this;
        }

        @Override
        public Builder dstIpPrefix(IpPrefix dstIpPrefix) {
            this.dstIpPrefix = dstIpPrefix;
            return this;
        }

        @Override
        public Builder ipProtocol(byte ipProtocol) {
            this.ipProtocol = ipProtocol;
            return this;
        }

        @Override
        public Builder srcTpPort(TpPort srcTpPort) {
            this.srcTpPort = srcTpPort;
            return this;
        }

        @Override
        public Builder dstTpPort(TpPort dstTpPort) {
            this.dstTpPort = dstTpPort;
            return this;
        }
    }
}
