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
package org.onosproject.openstackvtap.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of an immutable openstack vtap criterion.
 */
public final class DefaultOpenstackVtapCriterion implements OpenstackVtapCriterion {

    private final IpPrefix srcIpPrefix;
    private final IpPrefix dstIpPrefix;
    private final byte ipProtocol;
    private final TpPort srcTpPort;
    private final TpPort dstTpPort;

    // private constructor not intended to use from external
    private DefaultOpenstackVtapCriterion(IpPrefix srcIpPrefix,
                                          IpPrefix dstIpPrefix,
                                          byte ipProtocol,
                                          TpPort srcTpPort,
                                          TpPort dstTpPort) {
        this.srcIpPrefix = srcIpPrefix;
        this.dstIpPrefix = dstIpPrefix;
        this.ipProtocol  = ipProtocol;
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
    public int hashCode() {
        return Objects.hashCode(srcIpPrefix, dstIpPrefix,
                                ipProtocol, srcTpPort, dstTpPort);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultOpenstackVtapCriterion that = (DefaultOpenstackVtapCriterion) o;
        return Objects.equal(this.srcIpPrefix, that.srcIpPrefix) &&
                Objects.equal(this.dstIpPrefix, that.dstIpPrefix) &&
                Objects.equal(this.ipProtocol, that.ipProtocol) &&
                Objects.equal(this.srcTpPort, that.srcTpPort) &&
                Objects.equal(this.dstTpPort, that.dstTpPort);
    }

    /**
     * Creates a new default openstack vtap criterion builder.
     *
     * @return default openstack vtap criterion builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for openstack vtap criterion builder.
     */
    public static final class Builder implements OpenstackVtapCriterion.Builder {
        private static final String NOT_NULL_MSG = "OpenstackVtapCriterion % cannot be null";

        private IpPrefix srcIpPrefix;
        private IpPrefix dstIpPrefix;
        private byte     ipProtocol;
        private TpPort   srcTpPort;
        private TpPort   dstTpPort;

        // private constructor not intended to use from external
        Builder() {
        }

        @Override
        public DefaultOpenstackVtapCriterion build() {
            checkArgument(srcIpPrefix != null, NOT_NULL_MSG, "Source IP Prefix");
            checkArgument(dstIpPrefix != null, NOT_NULL_MSG, "Destination IP Prefix");

            return new DefaultOpenstackVtapCriterion(srcIpPrefix, dstIpPrefix,
                                                ipProtocol, srcTpPort, dstTpPort);
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
