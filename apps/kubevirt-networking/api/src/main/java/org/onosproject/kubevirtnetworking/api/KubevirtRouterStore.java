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
 * Manages inventory of kubevirt router states; not intended for direct use.
 */
public interface KubevirtRouterStore
        extends Store<KubevirtRouterEvent, KubevirtRouterStoreDelegate> {

    /**
     * Creates a new kubevirt router.
     *
     * @param router kubevirt router
     */
    void createRouter(KubevirtRouter router);

    /**
     * Updates the kubevirt router.
     *
     * @param router kubevirt router
     */
    void updateRouter(KubevirtRouter router);

    /**
     * Removes the kubevirt router with the given router identifier.
     *
     * @param name router name
     * @return remove kubevirt router; null if failed
     */
    KubevirtRouter removeRouter(String name);

    /**
     * Returns the kubevirt router with the given router name.
     *
     * @param name router name
     * @return removed kubevirt router; null if failed
     */
    KubevirtRouter router(String name);

    /**
     * Returns all kubevirt routes.
     *
     * @return set of kubevirt routers
     */
    Set<KubevirtRouter> routers();

    /**
     * Removes all kubevirt routers.
     */
    void clear();
}
