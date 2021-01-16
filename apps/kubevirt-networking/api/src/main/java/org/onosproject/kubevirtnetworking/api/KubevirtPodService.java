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
import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Service for interacting with the inventory of kubevirt service.
 */
public interface KubevirtPodService
        extends ListenerService<KubevirtPodEvent, KubevirtPodListener> {

    /**
     * Returns the kubevirt pod with the supplied pod UID.
     *
     * @param uid pod UID
     * @return kubevirt pod
     */
    Pod pod(String uid);

    /**
     * Returns all kubevirt pods registered.
     *
     * @return set of kubevirt pods
     */
    Set<Pod> pods();
}
