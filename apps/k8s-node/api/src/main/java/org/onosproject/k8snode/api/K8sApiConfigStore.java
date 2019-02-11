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

import org.onlab.packet.IpAddress;
import org.onosproject.k8snode.api.K8sApiConfig.Scheme;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubernetes API config; not intended for direct use.
 */
public interface K8sApiConfigStore
        extends Store<K8sApiConfigEvent, K8sApiConfigStoreDelegate> {

    /**
     * Creates a API config.
     *
     * @param config kubernetes API server config
     */
    void createApiConfig(K8sApiConfig config);

    /**
     * Updates the API config.
     *
     * @param config kubernetes API server config
     */
    void updateApiConfig(K8sApiConfig config);

    /**
     * Removes the kubernetes API config with the given endpoint.
     * Endpoint comprises of scheme (HTTP), IP address and port
     *
     * @param endpoint kubernetes API endpoint
     * @return removed kubernetes API server config; null if no config
     *         associated with the endpoint
     */
    K8sApiConfig removeApiConfig(String endpoint);

    /**
     * Removes the kubernetes API config with the given scheme, IP and port.
     *
     * @param scheme        scheme (HTTP/HTTPS)
     * @param ipAddress     IP address of API server
     * @param port          port number of API server
     * @return removed kubernetes API server config; null if no config
     *         associated with the scheme, IP and port
     */
    K8sApiConfig removeApiConfig(Scheme scheme, IpAddress ipAddress, int port);

    /**
     * Returns all registered kubernetes API configs.
     *
     * @return set of kubernetes API configs
     */
    Set<K8sApiConfig> apiConfigs();

    /**
     * Returns the API config with the specified endpoint.
     *
     * @param endpoint endpoint
     * @return kubernetes API config
     */
    K8sApiConfig apiConfig(String endpoint);

    /**
     * Returns the API config with the specified scheme, IP and port.
     *
     * @param scheme        scheme (HTTP/HTTPS)
     * @param ipAddress     IP address of API server
     * @param port          port number of API server
     * @return kubernetes API config
     */
    K8sApiConfig apiConfig(Scheme scheme, IpAddress ipAddress, int port);
}
