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
import org.onosproject.event.ListenerService;
import org.onosproject.k8snode.api.K8sApiConfig.Scheme;

import java.util.Set;

/**
 * Service for interfacing with the inventory of kubernetes API server config.
 */
public interface K8sApiConfigService
        extends ListenerService<K8sApiConfigEvent, K8sApiConfigListener> {
    String APP_ID = "org.onosproject.k8snode";

    /**
     * Returns all registered API configs.
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
