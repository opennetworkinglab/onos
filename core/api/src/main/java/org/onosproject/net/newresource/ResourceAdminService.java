/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.newresource;

import com.google.common.annotations.Beta;

import java.util.Arrays;
import java.util.List;

/**
 * Service for administering resource service behavior.
 */
@Beta
public interface ResourceAdminService {
    /**
     * Register resources as the children of the parent resource path.
     *
     * @param parent parent resource path under which the resource are registered
     * @param children resources to be registered as the children of the parent
     * @param <T> type of resources
     * @return true if registration is successfully done, false otherwise. Registration
     * succeeds when each resource is not registered or unallocated.
     */
    default <T> boolean registerResources(ResourcePath parent, T... children) {
        return registerResources(parent, Arrays.asList(children));
    }

    /**
     * Register resources as the children of the parent resource path.
     *
     * @param parent parent resource path under which the resource are registered
     * @param children resources to be registered as the children of the parent
     * @param <T> type of resources
     * @return true if registration is successfully done, false otherwise. Registration
     * succeeds when each resource is not registered or unallocated.
     */
    <T> boolean registerResources(ResourcePath parent, List<T> children);

    /**
     * Unregister resources as the children of the parent resource path.
     *
     * @param parent parent resource path under which the resource are unregistered
     * @param children resources to be unregistered as the children of the parent
     * @param <T> type of resources
     * @return true if unregistration is successfully done, false otherwise. Unregistration
     * succeeds when each resource is not registered or unallocated.
     */
    default <T> boolean unregisterResources(ResourcePath parent, T... children) {
        return unregisterResources(parent, Arrays.asList(children));
    }

    /**
     * Unregister resources as the children of the parent resource path.
     *
     * @param parent parent resource path under which the resource are unregistered
     * @param children resources to be unregistered as the children of the parent
     * @param <T> type of resources
     * @return true if unregistration is successfully done, false otherwise. Unregistration
     * succeeds when each resource is not registered or unallocated.
     */
    <T> boolean unregisterResources(ResourcePath parent, List<T> children);
}
