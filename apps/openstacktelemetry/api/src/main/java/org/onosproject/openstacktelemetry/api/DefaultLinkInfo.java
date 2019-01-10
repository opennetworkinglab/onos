/*
 * Copyright 2019-present Open Networking Foundation
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
import com.google.common.base.Objects;

/**
 * Implementation of Link info.
 */
public final class DefaultLinkInfo implements LinkInfo {

    private final String linkId;
    private final String srcIp;
    private final int srcPort;
    private final String dstIp;
    private final int dstPort;
    private final LinkStatsInfo statsInfo;
    private final String protocol;

    // default constructor not intended for invoked outside of this class
    private DefaultLinkInfo(String linkId, String srcIp, int srcPort,
                            String dstIp, int dstPort,
                            LinkStatsInfo statsInfo, String protocol) {
        this.linkId = linkId;
        this.srcIp = srcIp;
        this.srcPort = srcPort;
        this.dstIp = dstIp;
        this.dstPort = dstPort;
        this.statsInfo = statsInfo;
        this.protocol = protocol;
    }

    @Override
    public String linkId() {
        return linkId;
    }

    @Override
    public String srcIp() {
        return srcIp;
    }

    @Override
    public int srcPort() {
        return srcPort;
    }

    @Override
    public String dstIp() {
        return dstIp;
    }

    @Override
    public int dstPort() {
        return dstPort;
    }

    @Override
    public LinkStatsInfo linkStats() {
        return statsInfo;
    }

    @Override
    public String protocol() {
        return protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultLinkInfo that = (DefaultLinkInfo) o;
        return srcPort == that.srcPort &&
                dstPort == that.dstPort &&
                Objects.equal(linkId, that.linkId) &&
                Objects.equal(srcIp, that.srcIp) &&
                Objects.equal(dstIp, that.dstIp) &&
                Objects.equal(statsInfo, that.statsInfo) &&
                Objects.equal(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(linkId, srcIp, srcPort, dstIp, dstPort, statsInfo, protocol);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("linkId", linkId)
                .add("srcIp", srcIp)
                .add("srcPort", srcPort)
                .add("dstIp", dstIp)
                .add("dstPort", dstPort)
                .add("statsInfo", statsInfo)
                .add("protocol", protocol)
                .toString();
    }

    /**
     * Obtains a default link info builder object.
     *
     * @return link info builder object
     */
    public static LinkInfo.Builder builder() {
        return new DefaultLinkInfo.DefaultBuilder();
    }

    /**
     * Builder class of LinkInfo.
     */
    public static final class DefaultBuilder implements LinkInfo.Builder {
        private String linkId;
        private String srcIp;
        private int srcPort;
        private String dstIp;
        private int dstPort;
        private LinkStatsInfo linkStats;
        private String protocol;

        private DefaultBuilder() {
        }

        @Override
        public Builder withLinkId(String linkId) {
            this.linkId = linkId;
            return this;
        }

        @Override
        public Builder withSrcIp(String srcIp) {
            this.srcIp = srcIp;
            return this;
        }

        @Override
        public Builder withSrcPort(int srcPort) {
            this.srcPort = srcPort;
            return this;
        }

        @Override
        public Builder withDstIp(String dstIp) {
            this.dstIp = dstIp;
            return this;
        }

        @Override
        public Builder withDstPort(int dstPort) {
            this.dstPort = dstPort;
            return this;
        }

        @Override
        public Builder withLinkStats(LinkStatsInfo linkStats) {
            this.linkStats = linkStats;
            return this;
        }

        @Override
        public Builder withProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        @Override
        public DefaultLinkInfo build() {
            return new DefaultLinkInfo(linkId, srcIp, srcPort, dstIp,
                    dstPort, linkStats, protocol);
        }
    }
}
