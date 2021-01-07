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

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubevirt instance; not intended for direct use.
 */
public interface KubevirtInstanceStore
        extends Store<KubevirtInstanceEvent, KubevirtInstanceStoreDelegate> {

    /**
     * Creates a new kubevirt instance.
     *
     * @param instance kubevirt instance
     */
    void createInstance(KubevirtInstance instance);

    /**
     * Updates the kubevirt instance.
     *
     * @param instance kubevirt instance
     */
    void updateInstance(KubevirtInstance instance);

    /**
     * Removes the kubevirt instance with the given instance UID.
     *
     * @param uid kubevirt instance UID
     * @return remove kubevirt instance; null if failed
     */
    KubevirtInstance removeInstance(String uid);

    /**
     * Returns the kubevirt instance with the given instance UID.
     *
     * @param uid kubevirt instance UID
     * @return kubevirt instance; null if not found
     */
    KubevirtInstance instance(String uid);

    /**
     * Returns all kubevirt instances.
     *
     * @return set of kubevirt instance
     */
    Set<KubevirtInstance> instances();

    /**
     * Removes all kubevirt instances.
     */
    void clear();
}
