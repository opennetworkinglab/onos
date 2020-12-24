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

/**
 * Service for administering inventory of KubeVirt API configs.
 */
public interface KubevirtApiConfigAdminService extends KubevirtApiConfigService {

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
}
