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

package org.onosproject.openstacknode.api;

/**
 * Representation of openstack neutron config information.
 */
public interface NeutronConfig {

    /**
     * Returns whether to use metadata proxy service.
     * Note that SONA will behave as a metadata proxy server
     *
     * @return true if metadata proxy service is enabled, false otherwise
     */
    boolean useMetadataProxy();

    /**
     * Returns metadata proxy secret.
     *
     * @return metadata proxy secret
     */
    String metadataProxySecret();

    /**
     * Returns NOVA metadata IP address.
     *
     * @return NOVA metadata IP address
     */
    String novaMetadataIp();

    /**
     * Returns NOVA metadata port number.
     *
     * @return NOVA metadata port number
     */
    Integer novaMetadataPort();

    /**
     * Builder of neutron config.
     */
    interface Builder {

        /**
         * Builds an immutable neutron config instance.
         *
         * @return neutron config instance
         */
        NeutronConfig build();

        /**
         * Returns neutron config with supplied useMetadataProxy flag.
         *
         * @param useMetadataProxy useMetadataProxy flag
         * @return neutron config builder
         */
        Builder useMetadataProxy(boolean useMetadataProxy);

        /**
         * Returns neutron config with supplied metadataProxySecret.
         *
         * @param metadataProxySecret metadata proxy secret
         * @return neutron config builder
         */
        Builder metadataProxySecret(String metadataProxySecret);

        /**
         * Returns neutron config with supplied NOVA metadata IP address.
         *
         * @param novaMetadataIp NOVA metadata IP address
         * @return neutron config builder
         */
        Builder novaMetadataIp(String novaMetadataIp);

        /**
         * Returns neutron config with supplied NOVA metadata port number.
         *
         * @param novaMetadataPort NOVA metadata port number
         * @return neutron config builder
         */
        Builder novaMetadataPort(Integer novaMetadataPort);
    }
}
