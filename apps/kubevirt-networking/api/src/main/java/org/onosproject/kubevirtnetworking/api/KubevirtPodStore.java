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
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubevirt pod; not intended for direct use.
 */
public interface KubevirtPodStore extends Store<KubevirtPodEvent, KubevirtPodStoreDelegate> {

    /**
     * Creates the new kubevirt pod.
     *
     * @param pod kubevirt pod
     */
    void createPod(Pod pod);

    /**
     * Updates the kubevirt pod.
     *
     * @param pod kubevirt pod
     */
    void updatePod(Pod pod);

    /**
     * Removes the kubevirt pod with the given pod UID.
     *
     * @param uid kubevirt pod UID
     * @return removed kubevirt pod; null if failed
     */
    Pod removePod(String uid);

    /**
     * Returns the kubevirt pod with the given pod UID.
     *
     * @param uid kubevirt pod UID
     * @return kubevirt pod; null if not found
     */
    Pod pod(String uid);

    /**
     * Returns all kubevirt pods.
     *
     * @return set of kubevirt pods
     */
    Set<Pod> pods();

    /**
     * Removes all kubevirt pods.
     */
    void clear();
}
