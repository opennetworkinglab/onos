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
package org.onosproject.net.flowobjective;

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;

import java.util.Collection;

/**
 * Represents a filtering flow objective. Each filtering flow objective
 * is made up of a key (typically a PortCriterion) mapped to a set of criteria.
 * Using this information, a pipeline aware driver will decide how this objective
 * should be mapped to the device specific pipeline-tables in order to satisfy the
 * filtering condition. For example, consider the following PERMIT filtering
 * objective:
 * <p>
 * portX -&gt; {MAC1, VLAN1}
 * <p>
 * The driver could decide to pass packets to the MAC table or VLAN or PORT
 * tables to ensure that only those packets arriving with the correct dst MAC
 * and VLAN ids from Port X are allowed into the pipeline.
 * <p>
 * Filtering objectives of type PERMIT allow packets that match the key:criteria
 * to enter the pipeline. As a result, the implication is that packets that don't
 * match are automatically denied (dropped).
 * <p>
 * Filtering objectives of type DENY, are used to deny packets that would
 * otherwise be permitted and forwarded through the pipeline (ie. those packets
 * that make it through the PERMIT filters).
 */
@Beta
public interface FilteringObjective extends Objective {

    enum Type {
        /**
         * Permits packets that match the filtering condition to be processed
         * by the rest of the pipeline. Automatically denies packets that don't
         * match the criteria.
         */
        PERMIT,

        /**
         * Denies packets that make it through the permit filters.
         */
        DENY
    }

    /**
     * Obtain the key for this filter. The filter may or may not require a key.
     *
     * @return a criterion, which could be null if no key was provided.
     */
    Criterion key();

    /**
     * Obtain this filtering type.
     *
     * @return the type
     */
    Type type();

    /**
     * The set of conditions the filter must provision at the device.
     *
     * @return a collection of criteria
     */
    Collection<Criterion> conditions();

    /**
     * Auxiliary optional information provided to the device driver. Typically
     * conveys information about changes (treatments) to packets that are
     * permitted into the pipeline by the PERMIT filtering condition.
     *
     * @return a treatment on the packets that make it through the PERMIT filters.
     *         Value may be null if no meta information is provided.
     */
    TrafficTreatment meta();

    /**
     * Builder of Filtering objective entities.
     */
    interface Builder extends Objective.Builder {

        /**
         * Specify the key for the filter.
         *
         * @param key a criterion
         * @return a filter objective builder
         */
        Builder withKey(Criterion key);

        /**
         * Add a filtering condition.
         *
         * @param criterion new criterion
         * @return a filtering builder
         */
        Builder addCondition(Criterion criterion);

        /**
         * Permit this filtering condition set.
         *
         * @return a filtering builder
         */
        Builder permit();

        /**
         * Deny this filtering condition set.
         *
         * @return a filtering builder
         */
        Builder deny();

        /**
         * Set meta information about this filtering condition set.
         *
         * @param treatment traffic treatment to use
         * @return a filtering builder
         */
        Builder withMeta(TrafficTreatment treatment);

        /**
         * Assigns an application id.
         *
         * @param appId an application id
         * @return a filtering builder
         */
        @Override
        Builder fromApp(ApplicationId appId);

        /**
         * Builds the filtering objective that will be added.
         *
         * @return a filtering objective
         */
        @Override
        FilteringObjective add();

        /**
         * Builds the filtering objective that will be removed.
         *
         * @return a filtering objective.
         */
        @Override
        FilteringObjective remove();

        /**
         * Builds the filtering objective that will be added.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a filtering objective
         */
        @Override
        FilteringObjective add(ObjectiveContext context);

        /**
         * Builds the filtering objective that will be removed.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a filtering objective
         */
        @Override
        FilteringObjective remove(ObjectiveContext context);


    }

}
