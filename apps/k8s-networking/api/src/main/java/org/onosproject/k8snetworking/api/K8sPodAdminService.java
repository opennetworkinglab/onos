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

import io.fabric8.kubernetes.api.model.Pod;

/**
 * Service for administering the inventory of kubernetes service.
 */
public interface K8sPodAdminService extends K8sPodService {

    /**
     * Creates a kubernetes service with the given information.
     *
     * @param pod the new kubernetes pod
     */
    void createPod(Pod pod);

    /**
     * Updates the kubernetes pod with the given information.
     *
     * @param pod the updated kubernetes pod
     */
    void updatePod(Pod pod);

    /**
     * Removes the kubernetes pod.
     *
     * @param uid kubernetes pod UID
     */
    void removePod(String uid);

    /**
     * Clears the existing pods.
     */
    void clear();
}
