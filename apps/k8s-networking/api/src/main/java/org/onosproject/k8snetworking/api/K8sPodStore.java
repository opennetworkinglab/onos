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
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubernetes pod; not intended for direct use.
 */
public interface K8sPodStore extends Store<K8sPodEvent, K8sPodStoreDelegate> {

    /**
     * Creates the new kubernetes pod.
     *
     * @param pod kubernetes pod
     */
    void createPod(Pod pod);

    /**
     * Updates the kubernetes pod.
     *
     * @param pod kubernetes pod
     */
    void updatePod(Pod pod);

    /**
     * Removes the kubernetes pod with the given pod UID.
     *
     * @param uid kubernetes pod UID
     * @return removed kubernetes pod; null if failed
     */
    Pod removePod(String uid);

    /**
     * Returns the kubernetes pod with the given pod UID.
     *
     * @param uid kubernetes pod UID
     * @return kubernetes pod; null if not found
     */
    Pod pod(String uid);

    /**
     * Returns all kubernetes pods.
     *
     * @return set of kubernetes pods
     */
    Set<Pod> pods();

    /**
     * Removes all kubernetes pods.
     */
    void clear();
}
