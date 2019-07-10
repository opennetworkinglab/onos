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

import io.fabric8.kubernetes.api.model.Namespace;
import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Service for interacting with the inventory of kubernetes namespace.
 */
public interface K8sNamespaceService
        extends ListenerService<K8sNamespaceEvent, K8sNamespaceListener> {

    /**
     * Returns the kubernetes namespaces with the supplied namespace UID.
     *
     * @param uid namespace UID
     * @return kubernetes namespace
     */
    Namespace namespace(String uid);

    /**
     * Returns all kubernetes namespaces registered.
     *
     * @return set of kubernetes namespaces
     */
    Set<Namespace> namespaces();
}
