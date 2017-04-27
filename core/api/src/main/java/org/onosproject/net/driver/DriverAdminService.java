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
package org.onosproject.net.driver;

import java.util.Set;

/**
 * Service for managing drivers and driver behaviour implementations.
 */
public interface DriverAdminService
        extends DriverRegistry, BehaviourClassResolver {

    /**
     * Returns the set of driver providers currently registered.
     *
     * @return registered driver providers
     */
    Set<DriverProvider> getProviders();

    /**
     * Registers the specified driver provider.
     *
     * @param provider driver provider to register
     */
    void registerProvider(DriverProvider provider);

    /**
     * Unregisters the specified driver provider.
     *
     * @param provider driver provider to unregister
     */
    void unregisterProvider(DriverProvider provider);

}
