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
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubernetes endpoints; not intended for direct use.
 */
public interface K8sEndpointsStore
        extends Store<K8sEndpointsEvent, K8sEndpointsStoreDelegate> {

    /**
     * Creates the new kubernetes endpoints.
     *
     * @param endpoints kubernetes endpoints
     */
    void createEndpoints(Endpoints endpoints);

    /**
     * Updates the kubernetes endpoints.
     *
     * @param endpoints kubernetes endpoints
     */
    void updateEndpoints(Endpoints endpoints);

    /**
     * Removes the kubernetes endpoints with the given endpoints UID.
     *
     * @param uid kubernetes endpoints UID
     * @return removed kubernetes endpoints; null if failed
     */
    Endpoints removeEndpoints(String uid);

    /**
     * Returns the kubernetes endpoints with the given endpoints UID.
     *
     * @param uid kubernetes endpoints UID
     * @return kubernetes endpoints; null if not found
     */
    Endpoints endpoints(String uid);

    /**
     * Returns all kubernetes endpoints.
     *
     * @return set of kubernetes endpoints
     */
    Set<Endpoints> endpointses();

    /**
     * Removes all kubernetes endpoints.
     */
    void clear();
}
