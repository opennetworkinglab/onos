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
package org.onosproject.net.flowobjective;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.criteria.Criterion;

import java.util.Collection;

/**
 * Represents a filtering flow objective. Each filter is mapping
 * from a criterion to a collection of criteria. The mapping will
 * be used by a device driver to construct the actual flow rules to
 * be installed on the device.
 */
public interface FilteringObjective extends Objective {

    enum Type {
        /**
         * Enables the filtering condition.
         */
        PERMIT,

        /**
         * Disables the filtering condition.
         */
        DENY
    }

    /**
     * Obtain the key for this filter.
     *
     * @return a criterion
     */
    public Criterion key();

    /**
     * Obtain this filtering type.
     * @return the type
     */
    public Type type();

    /**
     * The set of conditions the filter must provision at the device.
     *
     * @return a collection of criteria
     */
    Collection<Criterion> conditions();

    /**
     * Builder of Filtering objective entities.
     */
    public interface Builder extends Objective.Builder {

        /**
         * Specify the key for the filter.
         *
         * @param key a criterion
         * @return a filter objective builder
         */
        public Builder withKey(Criterion key);

        /**
         * Add a filtering condition.
         *
         * @param criterion new criterion
         * @return a filtering builder
         */
        public Builder addCondition(Criterion criterion);

        /**
         * Permit this filtering condition set.
         * @return a filtering builder
         */
        public Builder permit();

        /**
         * Deny this filtering condition set.
         * @return a filtering builder
         */
        public Builder deny();

        /**
         * Assigns an application id.
         * @param appId an application id
         * @return a filtering builder
         */
        public Builder fromApp(ApplicationId appId);

        /**
         * Builds the filtering objective that will be added.
         *
         * @return a filtering objective
         */
        public FilteringObjective add();

        /**
         * Builds the filtering objective that will be removed.
         *
         * @return a filtering objective.
         */
        public FilteringObjective remove();


    }

}
