/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import io.fabric8.kubernetes.api.model.Pod;

/**
 * Service for administering the inventory of kubevirt service.
 */
public interface KubevirtPodAdminService extends KubevirtPodService {

    /**
     * Creates a kubevirt service with the given information.
     *
     * @param pod the new kubevirt pod
     */
    void createPod(Pod pod);

    /**
     * Updates the kubevirt pod with the given information.
     *
     * @param pod the updated kubevirt pod
     */
    void updatePod(Pod pod);

    /**
     * Removes the kubevirt pod.
     *
     * @param uid kubevirt pod UID
     */
    void removePod(String uid);

    /**
     * Clears the existing pods.
     */
    void clear();
}
