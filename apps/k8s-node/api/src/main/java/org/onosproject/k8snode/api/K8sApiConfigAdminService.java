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

/**
 * Service for administering inventory of kubernetes API configs.
 */
public interface K8sApiConfigAdminService extends K8sApiConfigService {

    /**
     * Creates an API config.
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
     * Removes the API config.
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
    K8sApiConfig removeApiConfig(K8sApiConfig.Scheme scheme, IpAddress ipAddress, int port);
}
