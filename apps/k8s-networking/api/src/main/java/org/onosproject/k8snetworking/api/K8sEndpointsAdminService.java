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
package org.onosproject.k8snetworking.api;

import io.fabric8.kubernetes.api.model.Endpoints;

/**
 * Service for administering the inventory of kubernetes endpoint.
 */
public interface K8sEndpointsAdminService extends K8sEndpointsService {

    /**
     * Creates kubernetes endpoints with the given information.
     *
     * @param endpoints the new kubernetes endpoints
     */
    void createEndpoints(Endpoints endpoints);

    /**
     * Updates the kubernetes endpoints with the given information.
     *
     * @param endpoints the updated kubernetes endpoints
     */
    void updateEndpoints(Endpoints endpoints);

    /**
     * Removes the kubernetes endpoints.
     *
     * @param uid kubernetes endpoint UID
     */
    void removeEndpoints(String uid);

    /**
     * Clears the existing endpoints.
     */
    void clear();
}
