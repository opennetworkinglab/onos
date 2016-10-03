/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.incubator.elasticcfg;

import java.util.Set;

/**
 * Abstraction for Filters that can be used while traversing the PropConfig stores.
 * This abstraction allows to select entries of interest based on various criteria
 * defined by this interface.
 * Only criteria based on {@code ConfigNodePath} are supported currently.
 * Filters can be used with "GET" methods of {@code ProprietaryConfigService}
 */
public interface ConfigFilter {
    /**
     * Builder for ConfigFilter.
     */
    interface Builder {
        /**
         * Adds new ConfigNodePath filtering criteria to a ConfigFilter object.
         * If the same ConfigNodePath is already part of the criteria
         * for the object, it will not be added again, but will not throw any exceptions.
         * This will not check for the validity of the ConfigNodePath.
         *
         * @param add new criteria
         * @return a ConfigFilter builder
         */
        Builder addCriteria(Set<ConfigNodePath> add);

        /**
         * Removes the given ConfigNodePath filtering criteria from a ConfigFilter object.
         * If the ConfigNodePath was NOT already part of the criteria for
         * the object, it will not be removed, but will not throw any exceptions.
         * This will not check for the validity of the PropCfgInstancePaths.
         *
         * @param remove criteria to be removed
         * @return a ConfigFilter builder
         */
        Builder removeCriteria(Set<ConfigNodePath> remove);

        /**
         * Builds an immutable ConfigFilter entity.
         *
         * @return ConfigFilter
         */
        ConfigFilter build();
    }

    /**
     * Method to list all the ConfigNodePath criteria that are in place for a ConfigFilter.
     *
     * @return Set of ConfigNodePath criteria for this entity
     */
    Set<ConfigNodePath> getCriteria();

    /**
     * Method to create a filter that include all entries rejected by the criteria.
     *
     * @param original filter object with a criteria set
     * @return ConfigFilter  object with negated criteria set
     * @throws InvalidFilterException if the received ConfigFilter object
     * was null or if it had an empty criteria set
     */
    ConfigFilter negateFilter(ConfigFilter original);

    /**
     * Method to check if the ConfigFilter has an empty criteria set.
     *
     * @return {@code true} if criteria set is empty, {@code true} otherwise.
     */
    boolean isEmptyFilter();
}