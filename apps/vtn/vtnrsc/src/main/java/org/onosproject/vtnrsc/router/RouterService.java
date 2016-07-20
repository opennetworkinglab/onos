/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.router;

import java.util.Collection;

import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterId;

/**
 * Service for interacting with the inventory of Routers.
 */
public interface RouterService {
    /**
     * Returns exists or not of specific router identifier.
     *
     * @param routerId router identifier
     * @return true or false
     */
    boolean exists(RouterId routerId);

    /**
     * Returns a collection of the currently known Routers.
     *
     * @return collection of Routers
     */
    Collection<Router> getRouters();

    /**
     * Returns the Router with the specified identifier.
     *
     * @param routerId Router identifier
     * @return Router or null if one with the given identifier is not known
     */
    Router getRouter(RouterId routerId);

    /**
     * Creates new Routers.
     *
     * @param routers the collection of Routers
     * @return true if the identifier Router has been created right.
     *         false if the identifier Router is failed to store
     */
    boolean createRouters(Collection<Router> routers);

    /**
     * Updates existing Routers.
     *
     * @param routers the collection of Routers
     * @return true if Routers were updated successfully.
     *         false if Routers were updated failed
     */
    boolean updateRouters(Collection<Router> routers);

    /**
     * Removes the specified Routers from the store.
     *
     * @param routerIds the collection of Routers identifier
     * @return true if remove identifier Routers successfully. false if remove
     *         identifier Routers failed
     */
    boolean removeRouters(Collection<RouterId> routerIds);

    /**
     * Adds the specified listener to Router manager.
     *
     * @param listener Router listener
     */
    void addListener(RouterListener listener);

    /**
     * Removes the specified listener to Router manager.
     *
     * @param listener Router listener
     */
    void removeListener(RouterListener listener);
}
