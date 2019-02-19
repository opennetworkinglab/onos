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

/**
 * Service for administering the inventory of kubernetes service.
 */
public interface K8sServiceAdminService extends K8sServiceService {

    /**
     * Creates a kubernetes service with the given information.
     *
     * @param service the new kubernetes service
     */
    void createService(Service service);

    /**
     * Updates the kubernetes service with the given information.
     *
     * @param service the updated kubernetes service
     */
    void updateService(Service service);

    /**
     * Removes the kubernetes service.
     *
     * @param uid kubernetes service UID
     */
    void removeService(String uid);

    /**
     * Clears the existing services.
     */
    void clear();
}
