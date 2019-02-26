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
import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Service for interacting with the inventory of kubernetes ingress.
 */
public interface K8sIngressService
        extends ListenerService<K8sIngressEvent, K8sIngressListener> {

    /**
     * Returns the kubernetes ingress with the supplied ingress UID.
     *
     * @param uid ingress UID
     * @return kubernetes ingress
     */
    Ingress ingress(String uid);

    /**
     * Returns all kubernetes ingresses registered.
     *
     * @return set of kubernetes ingresses
     */
    Set<Ingress> ingresses();
}
