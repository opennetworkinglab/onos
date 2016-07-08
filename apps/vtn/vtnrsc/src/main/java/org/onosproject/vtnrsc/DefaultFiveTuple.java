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
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.PortNumber;

/**
 * Representation for five tuple information from packet.
 */
public final class DefaultFiveTuple implements FiveTuple {

    private final MacAddress macSrc;
    private final MacAddress macDst;
    private final IpAddress ipSrc;
    private final IpAddress ipDst;
    private final PortNumber portSrc;
    private final PortNumber portDst;
    private final byte protocol;
    private final TenantId tenantId;

    /**
     * Constructor for packet five tuple information.
     *
     * @param protocol protocol of the packet
     * @param ipSrc source ip address of the packet
     * @param ipDst destination ip address of the packet
     * @param portSrc source port of the packet
     * @param portDst destination port of the packet
     */
    private DefaultFiveTuple(byte protocol, IpAddress ipSrc, IpAddress ipDst, PortNumber portSrc, PortNumber portDst,
                             TenantId tenantId, MacAddress macSrc, MacAddress macDst) {

        this.protocol = protocol;
        this.ipSrc = ipSrc;
        this.ipDst = ipDst;
        this.portSrc = portSrc;
        this.portDst = portDst;
        this.tenantId = tenantId;
        this.macSrc = macSrc;
        this.macDst = macDst;
    }

    @Override
    public byte protocol() {
        return protocol;
    }

    @Override
    public IpAddress ipSrc() {
        return ipSrc;
    }

    @Override
    public IpAddress ipDst() {
        return ipDst;
    }

    @Override
    public PortNumber portSrc() {
        return portSrc;
    }

    @Override
    public PortNumber portDst() {
        return portDst;
    }

    @Override
    public MacAddress macSrc() {
        return macSrc;
    }

    @Override
    public MacAddress macDst() {
        return macDst;
    }

    @Override
    public TenantId tenantId() {
        return tenantId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultFiveTuple) {
            final DefaultFiveTuple other = (DefaultFiveTuple) obj;
            return Objects.equals(this.protocol, other.protocol) &&
                    Objects.equals(this.ipSrc, other.ipSrc) &&
                    Objects.equals(this.ipDst, other.ipDst) &&
                    Objects.equals(this.portSrc, other.portSrc) &&
                    Objects.equals(this.portDst, other.portDst);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.protocol, this.ipSrc, this.ipDst, this.portSrc, this.portDst);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("protocol", protocol)
                .add("ipSrc", ipSrc)
                .add("ipDst", ipDst)
                .add("portSrc", portSrc)
                .add("portDst", portDst)
                .toString();
    }

    /**
     * To create an instance of the builder.
     *
     * @return instance of builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for Five tuple info.
     */
    public static final class Builder implements FiveTuple.Builder {

        private IpAddress ipSrc;
        private IpAddress ipDst;
        private PortNumber portSrc;
        private PortNumber portDst;
        private byte protocol;
        private TenantId tenantId;
        private MacAddress macSrc;
        private MacAddress macDst;

        @Override
        public Builder setIpSrc(IpAddress ipSrc) {
            this.ipSrc = ipSrc;
            return this;
        }

        @Override
        public Builder setIpDst(IpAddress ipDst) {
            this.ipDst = ipDst;
            return this;
        }

        @Override
        public Builder setPortSrc(PortNumber portSrc) {
            this.portSrc = portSrc;
            return this;
        }

        @Override
        public Builder setPortDst(PortNumber portDst) {
            this.portDst = portDst;
            return this;
        }

        @Override
        public Builder setProtocol(byte protocol) {
            this.protocol = protocol;
            return this;
        }

        @Override
        public Builder setTenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        @Override
        public Builder setMacSrc(MacAddress macSrc) {
            this.macSrc = macSrc;
            return this;
        }

        @Override
        public Builder setMacDst(MacAddress macDst) {
            this.macDst = macDst;
            return this;
        }

        @Override
        public FiveTuple build() {
            checkArgument(protocol == IPv4.PROTOCOL_TCP || protocol == IPv4.PROTOCOL_UDP ||
                    protocol == IPv4.PROTOCOL_ICMP, "Unsupported value for protocol while creating five tuple");

            return new DefaultFiveTuple(protocol, ipSrc, ipDst, portSrc, portDst, tenantId, macSrc, macDst);
        }
    }
}
