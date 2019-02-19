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

import io.fabric8.kubernetes.api.model.Service;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubernetes service; not intended for direct use.
 */
public interface K8sServiceStore
        extends Store<K8sServiceEvent, K8sServiceStoreDelegate> {

    /**
     * Creates the new kubernetes service.
     *
     * @param service kubernetes service
     */
    void createService(Service service);

    /**
     * Updates the kubernetes service.
     *
     * @param service kubernetes service
     */
    void updateService(Service service);

    /**
     * Removes the kubernetes service with the given service UID.
     *
     * @param uid kubernetes service UID
     * @return removed kubernetes service; null if failed
     */
    Service removeService(String uid);

    /**
     * Returns the kubernetes service with the given service UID.
     *
     * @param uid kubernetes service UID
     * @return kubernetes service; null if not found
     */
    Service service(String uid);

    /**
     * Returns all kubernetes services.
     *
     * @return set of kubernetes services
     */
    Set<Service> services();

    /**
     * Removes all kubernetes services.
     */
    void clear();
}
