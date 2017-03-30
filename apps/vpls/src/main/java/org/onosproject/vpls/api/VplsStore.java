/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.vpls.api;

import org.onosproject.store.Store;
import org.onosproject.store.StoreDelegate;
import org.onosproject.vpls.store.VplsStoreEvent;

import java.util.Collection;

/**
 * Definition of the operations regarding the management of the VPLS elements.
 */
public interface VplsStore extends Store<VplsStoreEvent, StoreDelegate<VplsStoreEvent>> {
    /**
     * Adds a VPLS to the configuration.
     *
     * @param vplsData the VPLS to add
     */
    void addVpls(VplsData vplsData);

    /**
     * Removes a VPLS from the configuration.
     *
     * @param vplsData the VPLS to remove
     */
    void removeVpls(VplsData vplsData);

    /**
     * Updates a VPLS.
     *
     * @param vplsData the VPLS to update
     */
    void updateVpls(VplsData vplsData);

    /**
     * Gets a VPLS by name.
     *
     * @param vplsName the VPLS name
     * @return the VPLS instance if it exists; null otherwise
     */
    VplsData getVpls(String vplsName);

    /**
     * Gets all the VPLSs.
     *
     * @return all the VPLSs
     */
    Collection<VplsData> getAllVpls();
}
