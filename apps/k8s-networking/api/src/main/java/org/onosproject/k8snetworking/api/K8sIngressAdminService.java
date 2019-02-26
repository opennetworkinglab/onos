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

import io.fabric8.kubernetes.api.model.extensions.Ingress;

/**
 * Service for administering the inventory of kubernetes ingress.
 */
public interface K8sIngressAdminService extends K8sIngressService {

    /**
     * Creates kubernetes ingress with the given information.
     *
     * @param ingress the new kubernetes ingress
     */
    void createIngress(Ingress ingress);

    /**
     * Updates the kubernetes ingress with the given information.
     *
     * @param ingress the updated kubernetes ingress
     */
    void updateIngress(Ingress ingress);

    /**
     * Removes the kubernetes ingress.
     *
     * @param uid kubernetes ingress UID
     */
    void removeIngress(String uid);

    /**
     * Clears the existing ingresses.
     */
    void clear();
}
