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
package org.onosproject.k8snode.api;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.k8snode.api.Constants.DEFAULT_CLUSTER_NAME;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode.NORMAL;
import static org.onosproject.k8snode.api.K8sApiConfig.Scheme.HTTPS;

/**
 * Default implementation of kubernetes API configuration.
 */
public final class DefaultK8sApiConfig implements K8sApiConfig {

    private static final String NOT_NULL_MSG = "API Config % cannot be null";

    private static final int SHORT_NAME_LENGTH = 10;

    private final String clusterName;
    private final int segmentId;
    private final IpPrefix extNetworkCidr;
    private final Scheme scheme;
    private final Mode mode;
    private final IpAddress ipAddress;
    private final int port;
    private final State state;
    private final String token;
    private final String caCertData;
    private final String clientCertData;
    private final String clientKeyData;
    private final Set<HostNodesInfo> infos;
    private final boolean dvr;

    private DefaultK8sApiConfig(String clusterName, int segmentId, IpPrefix extNetworkCidr,
                                Scheme scheme, IpAddress ipAddress, int port,
                                Mode mode, State state, String token, String caCertData,
                                String clientCertData, String clientKeyData,
                                Set<HostNodesInfo> infos, boolean dvr) {
        this.clusterName = clusterName;
        this.segmentId = segmentId;
        this.extNetworkCidr = extNetworkCidr;
        this.scheme = scheme;
        this.ipAddress = ipAddress;
        this.port = port;
        this.mode = mode;
        this.state = state;
        this.token = token;
        this.caCertData = caCertData;
        this.clientCertData = clientCertData;
        this.clientKeyData = clientKeyData;
        this.infos = infos;
        this.dvr = dvr;
    }

    @Override
    public String clusterName() {
        return clusterName;
    }

    @Override
    public String clusterShortName() {
        return StringUtils.substring(clusterName, 0, SHORT_NAME_LENGTH);
    }

    @Override
    public int segmentId() {
        return segmentId;
    }

    @Override
    public IpPrefix extNetworkCidr() {
        return extNetworkCidr;
    }

    @Override
    public Scheme scheme() {
        return scheme;
    }

    @Override
    public IpAddress ipAddress() {
        return ipAddress;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public Mode mode() {
        return mode;
    }

    @Override
    public K8sApiConfig updateState(State newState) {
        return new Builder()
                .clusterName(clusterName)
                .segmentId(segmentId)
                .extNetworkCidr(extNetworkCidr)
                .scheme(scheme)
                .ipAddress(ipAddress)
                .port(port)
                .state(newState)
                .mode(mode)
                .token(token)
                .caCertData(caCertData)
                .clientCertData(clientCertData)
                .clientKeyData(clientKeyData)
                .infos(infos)
                .dvr(dvr)
                .build();
    }

    @Override
    public String token() {
        return token;
    }

    @Override
    public String caCertData() {
        return caCertData;
    }

    @Override
    public String clientCertData() {
        return clientCertData;
    }

    @Override
    public String clientKeyData() {
        return clientKeyData;
    }

    @Override
    public Set<HostNodesInfo> infos() {
        return ImmutableSet.copyOf(infos);
    }

    @Override
    public boolean dvr() {
        return dvr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultK8sApiConfig that = (DefaultK8sApiConfig) o;
        return port == that.port &&
                scheme == that.scheme &&
                clusterName.equals(that.clusterName) &&
                segmentId == that.segmentId &&
                extNetworkCidr == that.extNetworkCidr &&
                ipAddress.equals(that.ipAddress) &&
                mode == that.mode &&
                state == that.state &&
                token.equals(that.token) &&
                caCertData.equals(that.caCertData) &&
                clientCertData.equals(that.clientCertData) &&
                clientKeyData.equals(that.clientKeyData) &&
                infos.equals(that.infos) &&
                dvr == that.dvr;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterName, segmentId, extNetworkCidr, scheme, ipAddress, port,
                mode, state, token, caCertData, clientCertData, clientKeyData, infos, dvr);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("clusterName", clusterName)
                .add("segmentID", segmentId)
                .add("extNetworkCIDR", extNetworkCidr)
                .add("scheme", scheme)
                .add("ipAddress", ipAddress)
                .add("port", port)
                .add("mode", mode)
                .add("state", state)
                .add("token", token)
                .add("caCertData", caCertData)
                .add("clientCertData", clientCertData)
                .add("clientKeyData", clientKeyData)
                .add("infos", infos)
                .add("dvr", dvr)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubernetes API server config builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements K8sApiConfig.Builder {

        private String clusterName;
        private int segmentId;
        private IpPrefix extNetworkCidr;
        private Scheme scheme;
        private Mode mode;
        private IpAddress ipAddress;
        private int port;
        private State state;
        private String token;
        private String caCertData;
        private String clientCertData;
        private String clientKeyData;
        private Set<HostNodesInfo> infos;
        private boolean dvr;

        @Override
        public K8sApiConfig build() {
            checkArgument(scheme != null, NOT_NULL_MSG, "scheme");
            checkArgument(ipAddress != null, NOT_NULL_MSG, "ipAddress");
            checkArgument(state != null, NOT_NULL_MSG, "state");

            if (scheme == HTTPS) {
                checkArgument(caCertData != null, NOT_NULL_MSG, "caCertData");
                checkArgument(clientCertData != null, NOT_NULL_MSG, "clientCertData");
                checkArgument(clientKeyData != null, NOT_NULL_MSG, "clientKeyData");
            }

            if (StringUtils.isEmpty(clusterName)) {
                clusterName = DEFAULT_CLUSTER_NAME;
            }

            if (mode == null) {
                mode = NORMAL;
            }

            if (infos == null) {
                infos = ImmutableSet.of();
            }

            return new DefaultK8sApiConfig(clusterName, segmentId, extNetworkCidr, scheme, ipAddress,
                    port, mode, state, token, caCertData, clientCertData, clientKeyData, infos, dvr);
        }

        @Override
        public Builder clusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        @Override
        public Builder segmentId(int segmentId) {
            this.segmentId = segmentId;
            return this;
        }

        @Override
        public K8sApiConfig.Builder extNetworkCidr(IpPrefix extNetworkCidr) {
            this.extNetworkCidr = extNetworkCidr;
            return this;
        }

        @Override
        public Builder scheme(Scheme scheme) {
            this.scheme = scheme;
            return this;
        }

        @Override
        public Builder ipAddress(IpAddress ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        @Override
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        @Override
        public Builder state(State state) {
            this.state = state;
            return this;
        }

        @Override
        public K8sApiConfig.Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        @Override
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        @Override
        public Builder caCertData(String caCertData) {
            this.caCertData = caCertData;
            return this;
        }

        @Override
        public Builder clientCertData(String clientCertData) {
            this.clientCertData = clientCertData;
            return this;
        }

        @Override
        public Builder clientKeyData(String clientKeyData) {
            this.clientKeyData = clientKeyData;
            return this;
        }

        @Override
        public K8sApiConfig.Builder infos(Set<HostNodesInfo> infos) {
            this.infos = infos;
            return this;
        }

        @Override
        public K8sApiConfig.Builder dvr(boolean dvr) {
            this.dvr = dvr;
            return this;
        }
    }
}
