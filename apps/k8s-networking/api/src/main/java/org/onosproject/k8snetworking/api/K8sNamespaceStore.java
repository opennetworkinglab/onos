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
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubernetes namespace; not intended for direct use.
 */
public interface K8sNamespaceStore
        extends Store<K8sNamespaceEvent, K8sNamespaceStoreDelegate> {

    /**
     * Creates the new kubernetes namespace.
     *
     * @param namespace kubernetes namespace
     */
    void createNamespace(Namespace namespace);

    /**
     * Updates the kubernetes namespace.
     *
     * @param namespace kubernetes namespace
     */
    void updateNamespace(Namespace namespace);

    /**
     * Removes the kubernetes namespace with the given namespace UID.
     *
     * @param uid kubernetes namespace UID
     * @return removed kubernetes namespace; null if failed
     */
    Namespace removeNamespace(String uid);

    /**
     * Returns the kubernetes namespace with the given namespace UID.
     *
     * @param uid kubernetes namespace UID
     * @return kubernetes namespace; null if not found
     */
    Namespace namespace(String uid);

    /**
     * Returns all kubernetes namespaces.
     *
     * @return set of kubernetes namespaces
     */
    Set<Namespace> namespaces();

    /**
     * Removes all kubernetes namespaces.
     */
    void clear();
}
