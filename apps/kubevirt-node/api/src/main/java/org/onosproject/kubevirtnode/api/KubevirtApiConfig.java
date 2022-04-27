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

import org.onlab.packet.IpAddress;

/**
 * Representation of configuration used in KubeVirt API server.
 */
public interface KubevirtApiConfig {

    /**
     * Lists of authentication schemes.
     */
    enum Scheme {
        /**
         * Signifies that this is a HTTP authentication scheme.
         */
        HTTP,

        /**
         * Signifies that this is a HTTPS authentication scheme.
         */
        HTTPS,
    }

    /**
     * Lists of API server connectivity states.
     */
    enum State {
        /**
         * Signifies that client is connected to KubeVirt API server.
         */
        CONNECTED,

        /**
         * Signifies that client is dis-connected from KubeVirt API server.
         */
        DISCONNECTED,
    }

    /**
     * Returns the authentication scheme.
     *
     * @return authentication scheme
     */
    Scheme scheme();

    /**
     * Returns the IP address of KubeVirt API server.
     *
     * @return IP address of KubeVirt API server
     */
    IpAddress ipAddress();

    /**
     * Returns the port number of KubeVirt API server.
     *
     * @return port number of KubeVirt API server
     */
    int port();

    /**
     * Returns the connectivity state to KubeVirt API server.
     *
     * @return connectivity state to KubeVirt API server
     */
    State state();

    /**
     * Returns new KubeVirt API config instance with given state.
     *
     * @param newState updated state
     * @return updated kubernetes API config
     */
    KubevirtApiConfig updateState(State newState);

    /**
     * Returns the new KubeVirt API config instance with the given IP address.
     *
     * @param ipAddress updated IP address
     * @return updated kubernetes API config
     */
    KubevirtApiConfig updateIpAddress(IpAddress ipAddress);

    /**
     * Returns the new KubeVirt API config instance with the given port.
     *
     * @param port updated port
     * @return updated kubernetes API config
     */
    KubevirtApiConfig updatePort(int port);

    /**
     * Returns the token used for authenticating to API server.
     *
     * @return token value
     */
    String token();

    /**
     * Returns the CA certificate data.
     *
     * @return CA certificate data
     */
    String caCertData();

    /**
     * Returns the client certificate data.
     *
     * @return client certificate data
     */
    String clientCertData();

    /**
     * Returns the client key data.
     *
     * @return client key data
     */
    String clientKeyData();

    /**
     * Returns the Fully Qualified Domain Name (FQDN) of the service.
     *
     * @return service FQDN
     */
    String serviceFqdn();

    /**
     * Returns the Fully Qualified Domain Name (FQDN) of the API server.
     *
     * @return API server FQDN
     */
    String apiServerFqdn();

    /**
     * Returns the controller IP address.
     *
     * @return controller IP address
     */
    IpAddress controllerIp();

    /**
     * Returns the datacenter id.
     *
     * @return datacenter id.
     */
    String datacenterId();

    /**
     * Returns the cluster id.
     *
     * @return cluster id.
     */
    String clusterId();

    /**
     * Builder of new API config entity.
     */
    interface Builder {

        /**
         * Builds an immutable KubeVirt API config instance.
         *
         * @return KubeVirt API config instance
         */
        KubevirtApiConfig build();

        /**
         * Returns KubeVirt API server config builder with supplied scheme.
         *
         * @param scheme scheme of authentication
         * @return KubeVirt API config builder
         */
        Builder scheme(Scheme scheme);

        /**
         * Returns KubeVirt API server config builder with supplied IP address.
         *
         * @param ipAddress IP address of KubeVirt API server
         * @return KubeVirt API config builder
         */
        Builder ipAddress(IpAddress ipAddress);

        /**
         * Returns KubeVirt API server config builder with supplied port number.
         *
         * @param port port number of KubeVirt API server
         * @return KubeVirt API config builder
         */
        Builder port(int port);

        /**
         * Returns KubeVirt API server config builder with supplied state.
         *
         * @param state connectivity state
         * @return KubeVirt API config builder
         */
        Builder state(State state);

        /**
         * Returns KubeVirt API server config builder with supplied token.
         *
         * @param token token for authentication
         * @return KubeVirt API config builder
         */
        Builder token(String token);

        /**
         * Returns KubeVirt API server config builder with supplied CA certificate data.
         *
         * @param caCertData CA certificate data
         * @return KubeVirt API config builder
         */
        Builder caCertData(String caCertData);

        /**
         * Returns KubeVirt API server config builder with supplied client certificate data.
         *
         * @param clientCertData client certificate data
         * @return KubeVirt API config builder
         */
        Builder clientCertData(String clientCertData);

        /**
         * Returns KubeVirt API server config builder with supplied client key data.
         *
         * @param clientKeyData client key data
         * @return KubeVirt API config builder
         */
        Builder clientKeyData(String clientKeyData);

        /**
         * Returns Kubevirt API server config builder with supplied service Fqdn.
         *
         * @param serviceFqdn service FQDN
         * @return KubeVirt API config builder
         */
        Builder serviceFqdn(String serviceFqdn);

        /**
         * Returns Kubevirt API server config builder with the supplied API server Fqdn.
         *
         * @param apiServerFqdn API server FQDN
         * @return Kubevirt API config builder
         */
        Builder apiServerFqdn(String apiServerFqdn);

        /**
         * Returns kubevirt API server config builder with the supplied controller IP.
         *
         * @param controllerIp controller IP address
         * @return Kubevirt API config builder
         */
        Builder controllerIp(IpAddress controllerIp);

        /**
         * Returns kubevirt API server config builder with the supplied datacenter id.
         *
         * @param datacenteId datacenter id
         * @return Kubevirt API config builder
         */
        Builder datacenterId(String datacenteId);

        /**
         * Returns kubevirt API server config builder with the supplied cluster id.
         *
         * @param clusterId cluster id
         * @return Kubevirt API config builder
         */
        Builder clusterId(String clusterId);
    }
}
