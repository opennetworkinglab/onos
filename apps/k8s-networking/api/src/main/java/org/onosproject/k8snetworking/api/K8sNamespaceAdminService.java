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

/**
 * Service for administering the inventory of kubernetes namespace.
 */
public interface K8sNamespaceAdminService extends K8sNamespaceService {

    /**
     * Creates a kubernetes namespace with the given information.
     *
     * @param namespace the new kubernetes namespace
     */
    void createNamespace(Namespace namespace);

    /**
     * Updates the kubernetes namespace with the given information.
     *
     * @param namespace the updated kubernetes namespace
     */
    void updateNamespace(Namespace namespace);

    /**
     * Removes the kubernetes namespace.
     *
     * @param uid kubernetes namespace UID
     */
    void removeNamespace(String uid);

    /**
     * Clears the existing namespaces.
     */
    void clear();
}
