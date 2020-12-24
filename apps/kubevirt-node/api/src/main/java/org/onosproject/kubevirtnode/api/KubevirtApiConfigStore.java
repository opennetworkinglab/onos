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

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of KubeVirt API config; not intended for direct use.
 */
public interface KubevirtApiConfigStore
        extends Store<KubevirtApiConfigEvent, KubevirtApiConfigStoreDelegate> {

    /**
     * Creates a API config.
     *
     * @param config KubeVirt API server config
     */
    void createApiConfig(KubevirtApiConfig config);

    /**
     * Updates the API config.
     *
     * @param config KubeVirt API server config
     */
    void updateApiConfig(KubevirtApiConfig config);

    /**
     * Removes the KubeVirt API config with the given endpoint.
     * Endpoint comprises of scheme (HTTP), IP address and port
     *
     * @param endpoint KubeVirt API endpoint
     * @return removed KubeVirt API server config; null if no config
     *         associated with the endpoint
     */
    KubevirtApiConfig removeApiConfig(String endpoint);

    /**
     * Returns all registered KubeVirt API configs.
     *
     * @return set of KubeVirt API configs
     */
    Set<KubevirtApiConfig> apiConfigs();

    /**
     * Returns the API config.
     *
     * @param endpoint endpoint
     * @return KubeVirt API config
     */
    KubevirtApiConfig apiConfig(String endpoint);
}
