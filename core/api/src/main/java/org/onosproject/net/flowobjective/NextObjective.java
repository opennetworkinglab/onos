/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import java.util.Collection;

/**
 * Represents a nexthop which will be translated by a driver
 * into the appropriate group or actions needed to implement
 * the egress function.
 *
 * A next objective is made up of a collection of traffic treatments
 * associated with a type. These types are:
 *
 * - Hashed
 * - Broadcast
 * - Failover
 * - Simple
 *
 * These types will indicate to the driver what the intended behavior is.
 * For example, a broadcast next objective with a collection of output
 * treatments will indicate to a driver that all output actions are expected
 * to be executed simultaneously. The driver is then free to implement this
 * as a group or a simple action list.
 */
@Beta
public interface NextObjective extends Objective {

    /**
     * Represents the type of next phase to build.
     */
    enum Type {
        /**
         * A hashed packet processing.
         */
        HASHED,

        /**
         * Broadcast packet process.
         */
        BROADCAST,

        /**
         * Failover handling.
         */
        FAILOVER,

        /**
         * Simple processing. Could be a group or a treatment.
         */
        SIMPLE
    }

    /**
     * The collection of treatments that need to be applied to a set of traffic.
     *
     * @return a collection of traffic treatments
     * @deprecated in 1.14.2, replaced by {@link #nextTreatments}
     */
    @Deprecated
    Collection<TrafficTreatment> next();

    /**
     * The collection of next treatments that need to be applied to a set of traffic.
     *
     * @return a collection of next treatments
     */
    Collection<NextTreatment> nextTreatments();

    /**
     * The type of operation that will be applied to the traffic using the collection
     * of treatments.
     *
     * @return a type
     */
    Type type();

    /**
     * Auxiliary optional information provided to the device driver. Typically
     * conveys information about selectors (matches) that are intended to
     * use this Next Objective.
     *
     * @return a selector intended to pass meta information to the device driver.
     *         Value may be null if no meta information is provided.
     */
    TrafficSelector meta();

    /**
     * Returns a new builder set to create a copy of this objective.
     *
     * @return new builder
     */
    @Override
    Builder copy();

    /**
     * A next step builder.
     */
    interface Builder extends Objective.Builder {

        /**
         * Specifies the id for this next objective.
         *
         * @param nextId an integer
         * @return a next objective builder
         */
        Builder withId(int nextId);

        /**
         * Sets the type of next step.
         *
         * @param type a type
         * @return a next step builder
         */
        Builder withType(Type type);

        /**
         * Adds a treatment to this next step.
         *
         * @param treatment a traffic treatment
         * @return a next step builder
         * @deprecated in 1.14.2, replaced by {@link #addTreatment(NextTreatment)}
         */
        @Deprecated
        Builder addTreatment(TrafficTreatment treatment);

        /**
         * Adds a next treatment to this next step.
         *
         * @param nextTreatment a next treatment
         * @return a next step builder
         */
        Builder addTreatment(NextTreatment nextTreatment);

        /**
         * Specifies the application which applied the filter.
         *
         * @param appId an application id
         * @return an objective builder
         */
        @Override
        Builder fromApp(ApplicationId appId);

        /**
         * Sets the priority for this objective.
         *
         * @param priority an integer
         * @return an objective builder
         */
        @Override
        Builder withPriority(int priority);

        /**
         * Set meta information related to this next objective.
         *
         * @param selector match conditions
         * @return an objective builder
         */
        Builder withMeta(TrafficSelector selector);

        /**
         * Builds the next objective that will be added.
         *
         * @return a next objective
         */
        @Override
        NextObjective add();

        /**
         * Builds the next objective that will be removed.
         *
         * @return a next objective.
         */
        @Override
        NextObjective remove();

        /**
         * Builds the next objective that will be added.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a next objective
         */
        @Override
        NextObjective add(ObjectiveContext context);

        /**
         * Builds the next objective that will be removed.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a next objective
         */
        @Override
        NextObjective remove(ObjectiveContext context);

        /**
         * Build the next objective that will be added, with {@link Operation}
         * ADD_TO_EXISTING.
         *
         * @return a next objective
         */
        NextObjective addToExisting();

        /**
         * Build the next objective that will be removed, with {@link Operation}
         * REMOVE_FROM_EXISTING.
         *
         * @return a next objective
         */
        NextObjective removeFromExisting();

        /**
         * Builds the next objective that will be added, with {@link Operation}
         * ADD_TO_EXISTING. The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a next objective
         */
        NextObjective addToExisting(ObjectiveContext context);

        /**
         * Builds the next objective that will be removed, with {@link Operation}
         * REMOVE_FROM_EXISTING. The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a next objective
         */
        NextObjective removeFromExisting(ObjectiveContext context);

        /**
         * Build the next objective that will be modified with {@link Operation}
         * MODIFY.
         *
         * @return a next objective
         */

        NextObjective modify();
        /**
         * Build the next objective that will be modified, with {@link Operation}
         * MODIFY. The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a next objective
         */
        NextObjective modify(ObjectiveContext context);

        /**
         * Builds the next objective that needs to be verified.
         *
         * @return a next objective with {@link Operation} VERIFY
         */
        NextObjective verify();

        /**
         * Builds the next objective that needs to be verified. The context will
         * be used to notify the calling application.
         *
         * @param context an objective context
         * @return a next objective with {@link Operation} VERIFY
         */
        NextObjective verify(ObjectiveContext context);

    }

}
