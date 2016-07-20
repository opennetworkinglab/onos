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
package org.onosproject.vtnrsc;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onlab.packet.IpPrefix;

import com.google.common.base.MoreObjects;

/**
 * Provides Default flow classifier.
 */
public final class DefaultFlowClassifier implements FlowClassifier {

    private final FlowClassifierId flowClassifierId;
    private final TenantId tenantId;
    private final String name;
    private final String description;
    private final String etherType;
    private final String protocol;
    private final int priority;
    private final int minSrcPortRange;
    private final int maxSrcPortRange;
    private final int minDstPortRange;
    private final int maxDstPortRange;
    private final IpPrefix srcIpPrefix;
    private final IpPrefix dstIpPrefix;
    private final VirtualPortId srcPort;
    private final VirtualPortId dstPort;
    private static final int NULL_PORT = 0;
    private static final String FLOW_CLASSIFIER_ID_NOT_NULL = "FlowClassifier id can not be null.";
    private static final String TENANT_ID_NOT_NULL = "Tenant id can not be null.";
    private static final String NAME_NOT_NULL = "Name can not be null.";
    private static final String ETHER_TYPE_NOT_NULL = "Ether Type can not be null.";
    private static final int DEFAULT_CLASSIFIER_PRIORITY = 0xCB20;

    /**
     * Constructor to create default flow classifier.
     *
     * @param flowClassifierId      flow classifier Id
     * @param tenantId              Tenant ID
     * @param name                  flow classifier name
     * @param description           flow classifier description
     * @param etherType             etherType
     * @param protocol              IP protocol
     * @param priority              priority for classification
     * @param minSrcPortRange       Minimum Source port range
     * @param maxSrcPortRange       Maximum Source port range
     * @param minDstPortRange       Minimum destination port range
     * @param maxDstPortRange       Maximum destination port range
     * @param srcIpPrefix           Source IP prefix
     * @param dstIpPrefix           destination IP prefix
     * @param srcPort               Source VirtualPort
     * @param dstPort               destination VirtualPort
     */
    private DefaultFlowClassifier(FlowClassifierId flowClassifierId, TenantId tenantId, String name,
                                  String description, String etherType, String protocol, int priority,
                                  int minSrcPortRange, int maxSrcPortRange, int minDstPortRange, int maxDstPortRange,
                                  IpPrefix srcIpPrefix, IpPrefix dstIpPrefix, VirtualPortId srcPort,
                                  VirtualPortId dstPort) {
        this.flowClassifierId = flowClassifierId;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.etherType = etherType;
        this.protocol = protocol;
        this.priority = priority;
        this.minSrcPortRange = minSrcPortRange;
        this.maxSrcPortRange = maxSrcPortRange;
        this.minDstPortRange = minDstPortRange;
        this.maxDstPortRange = maxDstPortRange;
        this.srcIpPrefix = srcIpPrefix;
        this.dstIpPrefix = dstIpPrefix;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
    }

    @Override
    public FlowClassifierId flowClassifierId() {
        return flowClassifierId;
    }

    @Override
    public TenantId tenantId() {
        return tenantId;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String etherType() {
        return etherType;
    }

    @Override
    public String protocol() {
        return protocol;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public int minSrcPortRange() {
        return minSrcPortRange;
    }

    @Override
    public int maxSrcPortRange() {
        return maxSrcPortRange;
    }

    @Override
    public int minDstPortRange() {
        return minDstPortRange;
    }

    @Override
    public int maxDstPortRange() {
        return maxDstPortRange;
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
    public VirtualPortId srcPort() {
        return srcPort;
    }

    @Override
    public VirtualPortId dstPort() {
        return dstPort;
    }

    /**
     * Builder class for constructing Flow classifier.
     */
    public static class Builder implements FlowClassifier.Builder {

        private FlowClassifierId flowClassifierId;
        private TenantId tenantId;
        private String name;
        private String description;
        private boolean isFlowClassifierDescriptionSet = false;
        private String etherType;
        private String protocol;
        private boolean isProtocolSet = false;
        private int priority;
        private boolean isPrioritySet = false;
        private int minSrcPortRange;
        private boolean isMinSrcPortRangeSet = false;
        private int maxSrcPortRange;
        private boolean isMaxSrcPortRangeSet = false;
        private int minDstPortRange;
        private boolean isMinDstPortRangeSet = false;
        private int maxDstPortRange;
        private boolean isMaxDstPortRangeSet = false;
        private IpPrefix srcIpPrefix;
        private boolean isSrcIpPrefixSet = false;
        private IpPrefix dstIpPrefix;
        private boolean isDstIpPrefixSet = false;
        private VirtualPortId srcPort;
        private boolean isSrcPortSet = false;
        private VirtualPortId dstPort;
        private boolean isDstPortSet = false;

        @Override
        public FlowClassifier build() {

            checkNotNull(flowClassifierId, FLOW_CLASSIFIER_ID_NOT_NULL);
            checkNotNull(tenantId, TENANT_ID_NOT_NULL);
            checkNotNull(name, NAME_NOT_NULL);
            checkNotNull(etherType, ETHER_TYPE_NOT_NULL);
            String description = null;
            String protocol = null;
            int priority = DEFAULT_CLASSIFIER_PRIORITY;
            int minSrcPortRange = NULL_PORT;
            int maxSrcPortRange = NULL_PORT;
            int minDstPortRange = NULL_PORT;
            int maxDstPortRange = NULL_PORT;
            IpPrefix srcIpPrefix = null;
            IpPrefix dstIpPrefix = null;
            VirtualPortId srcPort = null;
            VirtualPortId dstPort = null;

            if (isFlowClassifierDescriptionSet) {
                description = this.description;
            }
            if (isProtocolSet) {
                protocol = this.protocol;
            }
            if (isPrioritySet) {
                priority = this.priority;
            }
            if (isMinSrcPortRangeSet) {
                minSrcPortRange = this.minSrcPortRange;
            }
            if (isMaxSrcPortRangeSet) {
                maxSrcPortRange = this.maxSrcPortRange;
            }
            if (isMinDstPortRangeSet) {
                minDstPortRange = this.minDstPortRange;
            }
            if (isMaxDstPortRangeSet) {
                maxDstPortRange = this.maxDstPortRange;
            }
            if (isSrcIpPrefixSet) {
                srcIpPrefix = this.srcIpPrefix;
            }
            if (isDstIpPrefixSet) {
                dstIpPrefix = this.dstIpPrefix;
            }
            if (isSrcPortSet) {
                srcPort = this.srcPort;
            }
            if (isDstPortSet) {
                dstPort = this.dstPort;
            }

            return new DefaultFlowClassifier(flowClassifierId, tenantId, name, description, etherType, protocol,
                                             priority, minSrcPortRange, maxSrcPortRange, minDstPortRange,
                                             maxDstPortRange, srcIpPrefix, dstIpPrefix, srcPort, dstPort);
        }

        @Override
        public Builder setFlowClassifierId(FlowClassifierId flowClassifierId) {
            this.flowClassifierId = flowClassifierId;
            return this;
        }

        @Override
        public Builder setTenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        @Override
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder setDescription(String description) {
            this.description = description;
            this.isFlowClassifierDescriptionSet = true;
            return this;
        }

        @Override
        public Builder setEtherType(String etherType) {
            this.etherType = etherType;
            return this;
        }

        @Override
        public Builder setProtocol(String protocol) {
            this.protocol = protocol;
            this.isProtocolSet = true;
            return this;
        }

        @Override
        public Builder setPriority(int priority) {
            this.priority = priority;
            this.isPrioritySet = true;
            return this;
        }

        @Override
        public Builder setMinSrcPortRange(int minSrcPortRange) {
            this.minSrcPortRange = minSrcPortRange;
            this.isMinSrcPortRangeSet = true;
            return this;
        }

        @Override
        public Builder setMaxSrcPortRange(int maxSrcPortRange) {
            this.maxSrcPortRange = maxSrcPortRange;
            this.isMaxSrcPortRangeSet = true;
            return this;
        }

        @Override
        public Builder setMinDstPortRange(int minDstPortRange) {
            this.minDstPortRange = minDstPortRange;
            this.isMinDstPortRangeSet = true;
            return this;
        }

        @Override
        public Builder setMaxDstPortRange(int maxDstPortRange) {
            this.maxDstPortRange = maxDstPortRange;
            this.isMaxDstPortRangeSet = true;
            return this;
        }

        @Override
        public Builder setSrcIpPrefix(IpPrefix srcIpPrefix) {
            this.srcIpPrefix = srcIpPrefix;
            this.isSrcIpPrefixSet = true;
            return this;
        }

        @Override
        public Builder setDstIpPrefix(IpPrefix dstIpPrefix) {
            this.dstIpPrefix = dstIpPrefix;
            this.isDstIpPrefixSet = true;
            return this;
        }

        @Override
        public Builder setSrcPort(VirtualPortId srcPort) {
            this.srcPort = srcPort;
            this.isSrcPortSet = true;
            return this;
        }

        @Override
        public Builder setDstPort(VirtualPortId dstPort) {
            this.dstPort = dstPort;
            this.isDstPortSet = true;
            return this;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowClassifierId, tenantId, name, description, etherType, protocol, minSrcPortRange,
                maxSrcPortRange, minDstPortRange, maxDstPortRange, srcIpPrefix, dstIpPrefix, srcPort, dstPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultFlowClassifier) {
            DefaultFlowClassifier other = (DefaultFlowClassifier) obj;
            return Objects.equals(this.flowClassifierId, other.flowClassifierId)
                    && Objects.equals(this.tenantId, other.tenantId)
                    && Objects.equals(this.name, other.name)
                    && Objects.equals(this.description, other.description)
                    && Objects.equals(this.etherType, other.etherType)
                    && Objects.equals(this.protocol, other.protocol)
                    && Objects.equals(this.priority, other.priority)
                    && Objects.equals(this.minSrcPortRange, other.minSrcPortRange)
                    && Objects.equals(this.maxSrcPortRange, other.maxSrcPortRange)
                    && Objects.equals(this.minDstPortRange, other.minDstPortRange)
                    && Objects.equals(this.maxDstPortRange, other.maxDstPortRange)
                    && Objects.equals(this.srcIpPrefix, other.srcIpPrefix)
                    && Objects.equals(this.dstIpPrefix, other.dstIpPrefix)
                    && Objects.equals(this.srcPort, other.srcPort)
                    && Objects.equals(this.dstPort, other.dstPort);
        }
        return false;
    }

    @Override
    public boolean exactMatch(FlowClassifier flowClassifier) {
        return this.equals(flowClassifier)
                && Objects.equals(this.flowClassifierId, flowClassifier.flowClassifierId())
                && Objects.equals(this.tenantId, flowClassifier.tenantId())
                && Objects.equals(this.name, flowClassifier.name())
                && Objects.equals(this.description, flowClassifier.description())
                && Objects.equals(this.etherType, flowClassifier.etherType())
                && Objects.equals(this.protocol, flowClassifier.protocol())
                && Objects.equals(this.priority, flowClassifier.priority())
                && Objects.equals(this.minSrcPortRange, flowClassifier.minSrcPortRange())
                && Objects.equals(this.maxSrcPortRange, flowClassifier.maxSrcPortRange())
                && Objects.equals(this.minDstPortRange, flowClassifier.minDstPortRange())
                && Objects.equals(this.maxDstPortRange, flowClassifier.maxDstPortRange())
                && Objects.equals(this.srcIpPrefix, flowClassifier.srcIpPrefix())
                && Objects.equals(this.dstIpPrefix, flowClassifier.dstIpPrefix())
                && Objects.equals(this.srcPort, flowClassifier.srcPort())
                && Objects.equals(this.dstPort, flowClassifier.dstPort());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("FlowClassifierId", flowClassifierId)
                .add("TenantId", tenantId)
                .add("Name", name)
                .add("Description", description)
                .add("String", etherType)
                .add("Protocol", protocol)
                .add("Priority", priority)
                .add("MinSrcPortRange", minSrcPortRange)
                .add("MaxSrcPortRange", maxSrcPortRange)
                .add("MinDstPortRange", minDstPortRange)
                .add("MaxDstPortRange", maxDstPortRange)
                .add("SrcIpPrefix", srcIpPrefix)
                .add("DstIpPrefix", dstIpPrefix)
                .add("SrcPort", srcPort)
                .add("DstPort", dstPort)
                .toString();
    }
}
