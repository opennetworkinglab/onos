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
import org.onlab.packet.IpAddress;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.k8snode.api.K8sApiConfig.Scheme.HTTPS;

/**
 * Default implementation of kubernetes API configuration.
 */
public final class DefaultK8sApiConfig implements K8sApiConfig {

    private static final String NOT_NULL_MSG = "API Config % cannot be null";

    private final Scheme scheme;
    private final IpAddress ipAddress;
    private final int port;
    private final State state;
    private final String token;
    private final String caCertData;
    private final String clientCertData;
    private final String clientKeyData;

    private DefaultK8sApiConfig(Scheme scheme, IpAddress ipAddress, int port,
                                State state, String token, String caCertData,
                                String clientCertData, String clientKeyData) {
        this.scheme = scheme;
        this.ipAddress = ipAddress;
        this.port = port;
        this.state = state;
        this.token = token;
        this.caCertData = caCertData;
        this.clientCertData = clientCertData;
        this.clientKeyData = clientKeyData;
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
    public K8sApiConfig updateState(State newState) {
        return new Builder()
                .scheme(scheme)
                .ipAddress(ipAddress)
                .port(port)
                .state(newState)
                .token(token)
                .caCertData(caCertData)
                .clientCertData(clientCertData)
                .clientKeyData(clientKeyData)
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
                ipAddress.equals(that.ipAddress) &&
                state == that.state &&
                token.equals(that.token) &&
                caCertData.equals(that.caCertData) &&
                clientCertData.equals(that.clientCertData) &&
                clientKeyData.equals(that.clientKeyData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, ipAddress, port, state, token, caCertData,
                clientCertData, clientKeyData);
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

        private Scheme scheme;
        private IpAddress ipAddress;
        private int port;
        private State state;
        private String token;
        private String caCertData;
        private String clientCertData;
        private String clientKeyData;

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

            return new DefaultK8sApiConfig(scheme, ipAddress, port, state, token,
                    caCertData, clientCertData, clientKeyData);
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
    }
}
