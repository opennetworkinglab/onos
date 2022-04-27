/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.kubevirtnode.api.KubevirtApiConfig.Scheme.HTTPS;

/**
 * Default implementation of KubeVirt API configuration.
 */
public final class DefaultKubevirtApiConfig implements KubevirtApiConfig {

    private static final String NOT_NULL_MSG = "API Config % cannot be null";

    private final Scheme scheme;
    private final IpAddress ipAddress;
    private final int port;
    private final State state;
    private final String token;
    private final String caCertData;
    private final String clientCertData;
    private final String clientKeyData;
    private final String serviceFqdn;
    private final String apiServerFqdn;
    private final IpAddress controllerIp;
    private final String datacenterId;
    private final String clusterId;

    /**
     * Default constructor for Kubevirt API config.
     *
     * @param scheme            authentication scheme
     * @param ipAddress         IP address of API server
     * @param port              port number of API server
     * @param state             API server connectivity state
     * @param token             token used for authenticating against API server
     * @param caCertData        CA certificate data
     * @param clientCertData    client certificate data
     * @param clientKeyData     client key data
     * @param serviceFqdn       service FQDN
     * @param apiServerFqdn     API server FQDN
     * @param controllerIp      controller IP address
     * @param datacenterId      datacenter Id
     * @param clusterId         cluster Id
     */
    private DefaultKubevirtApiConfig(Scheme scheme, IpAddress ipAddress,
                                     int port, State state,
                                     String token, String caCertData,
                                     String clientCertData, String clientKeyData,
                                     String serviceFqdn, String apiServerFqdn,
                                     IpAddress controllerIp, String datacenterId, String clusterId) {
        this.scheme = scheme;
        this.ipAddress = ipAddress;
        this.port = port;
        this.state = state;
        this.token = token;
        this.caCertData = caCertData;
        this.clientCertData = clientCertData;
        this.clientKeyData = clientKeyData;
        this.serviceFqdn = serviceFqdn;
        this.apiServerFqdn = apiServerFqdn;
        this.controllerIp = controllerIp;
        this.datacenterId = datacenterId;
        this.clusterId = clusterId;
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
    public KubevirtApiConfig updateState(State newState) {
        return new Builder()
                .scheme(scheme)
                .ipAddress(ipAddress)
                .port(port)
                .state(newState)
                .token(token)
                .caCertData(caCertData)
                .clientCertData(clientCertData)
                .clientKeyData(clientKeyData)
                .serviceFqdn(serviceFqdn)
                .apiServerFqdn(apiServerFqdn)
                .controllerIp(controllerIp)
                .datacenterId(datacenterId)
                .clusterId(clusterId)
                .build();
    }

    @Override
    public KubevirtApiConfig updateIpAddress(IpAddress newIpAddress) {
        return new Builder()
                .scheme(scheme)
                .ipAddress(newIpAddress)
                .port(port)
                .state(state)
                .token(token)
                .caCertData(caCertData)
                .clientCertData(clientCertData)
                .clientKeyData(clientKeyData)
                .serviceFqdn(serviceFqdn)
                .apiServerFqdn(apiServerFqdn)
                .controllerIp(controllerIp)
                .datacenterId(datacenterId)
                .clusterId(clusterId)
                .build();
    }

    @Override
    public KubevirtApiConfig updatePort(int newPort) {
        return new Builder()
                .scheme(scheme)
                .ipAddress(ipAddress)
                .port(newPort)
                .state(state)
                .token(token)
                .caCertData(caCertData)
                .clientCertData(clientCertData)
                .clientKeyData(clientKeyData)
                .serviceFqdn(serviceFqdn)
                .apiServerFqdn(apiServerFqdn)
                .controllerIp(controllerIp)
                .datacenterId(datacenterId)
                .clusterId(clusterId)
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
    public String serviceFqdn() {
        return serviceFqdn;
    }

    @Override
    public String apiServerFqdn() {
        return apiServerFqdn;
    }

    @Override
    public IpAddress controllerIp() {
        return controllerIp;
    }

    @Override
    public String datacenterId() {
        return datacenterId;
    }

    @Override
    public String clusterId() {
        return clusterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtApiConfig that = (DefaultKubevirtApiConfig) o;
        return port == that.port && scheme == that.scheme &&
                ipAddress.equals(that.ipAddress) && state == that.state &&
                token.equals(that.token) && caCertData.equals(that.caCertData) &&
                clientCertData.equals(that.clientCertData) &&
                clientKeyData.equals(that.clientKeyData) &&
                Objects.equals(serviceFqdn, that.serviceFqdn) &&
                Objects.equals(apiServerFqdn, that.apiServerFqdn) &&
                Objects.equals(controllerIp, that.controllerIp) &&
                Objects.equals(datacenterId, that.datacenterId) &&
                Objects.equals(clusterId, that.clusterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, ipAddress, port, state, token,
                caCertData, clientCertData, clientKeyData, serviceFqdn,
                apiServerFqdn, controllerIp, datacenterId, clusterId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("scheme", scheme)
                .add("ipAddress", ipAddress)
                .add("port", port)
                .add("state", state)
                .add("token", token)
                .add("caCertData", caCertData)
                .add("clientCertData", clientCertData)
                .add("clientKeyData", clientKeyData)
                .add("serviceFqdn", serviceFqdn)
                .add("apiServerFqdn", apiServerFqdn)
                .add("controllerIp", controllerIp)
                .add("datacenterId", datacenterId)
                .add("clusterId", clusterId)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return KubeVirt API server config builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements KubevirtApiConfig.Builder {

        private Scheme scheme;
        private IpAddress ipAddress;
        private int port;
        private State state;
        private String token;
        private String caCertData;
        private String clientCertData;
        private String clientKeyData;
        private String serviceFqdn;
        private String apiServerFqdn;
        private IpAddress controllerIp;
        private String datacenterId;
        private String clusterId;

        @Override
        public KubevirtApiConfig build() {
            checkArgument(scheme != null, NOT_NULL_MSG, "scheme");
            checkArgument(ipAddress != null, NOT_NULL_MSG, "ipAddress");
            checkArgument(state != null, NOT_NULL_MSG, "state");

            if (scheme == HTTPS) {
                checkArgument(caCertData != null, NOT_NULL_MSG, "caCertData");
                checkArgument(clientCertData != null, NOT_NULL_MSG, "clientCertData");
                checkArgument(clientKeyData != null, NOT_NULL_MSG, "clientKeyData");
            }

            return new DefaultKubevirtApiConfig(scheme, ipAddress, port, state,
                    token, caCertData, clientCertData, clientKeyData, serviceFqdn,
                    apiServerFqdn, controllerIp, datacenterId, clusterId);
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
        public Builder serviceFqdn(String serviceFqdn) {
            this.serviceFqdn = serviceFqdn;
            return this;
        }

        @Override
        public Builder apiServerFqdn(String apiServerFqdn) {
            this.apiServerFqdn = apiServerFqdn;
            return this;
        }

        @Override
        public Builder controllerIp(IpAddress controllerIp) {
            this.controllerIp = controllerIp;
            return this;
        }

        @Override
        public Builder datacenterId(String datacenterId) {
            this.datacenterId = datacenterId;
            return this;
        }

        @Override
        public Builder clusterId(String clusterId) {
            this.clusterId = clusterId;
            return this;
        }
    }
}
