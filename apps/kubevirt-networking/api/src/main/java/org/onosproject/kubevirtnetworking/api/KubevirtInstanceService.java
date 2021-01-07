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

import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Service for interacting with the inventory of kubevirt instance.
 */
public interface KubevirtInstanceService
        extends ListenerService<KubevirtInstanceEvent, KubevirtInstanceListener> {

    /**
     * Returns the kubevirt instance with the supplied instance UID.
     *
     * @param uid instance UID
     * @return kubevirt instance
     */
    KubevirtInstance instance(String uid);

    /**
     * Returns all kubevirt instances registered.
     *
     * @return set of kubevirt instances
     */
    Set<KubevirtInstance> instances();
}
