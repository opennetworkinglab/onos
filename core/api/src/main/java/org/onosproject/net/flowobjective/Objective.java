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

import java.util.Optional;

/**
 * Base representation of a flow-objective description.
 */
@Beta
public interface Objective {

    boolean DEFAULT_PERMANENT = true;
    int DEFAULT_TIMEOUT = 0;
    int DEFAULT_PRIORITY = 32768;
    int MIN_PRIORITY = 0;
    int MAX_PRIORITY = 65535;

    /**
     * Type of operation.
     */
    enum Operation {
        /**
         * Adds the objective. Can be used for any flow objective. For forwarding
         * and filtering objectives, existing objectives with identical selector
         * and priority fields (but different treatments or next) will be replaced.
         * For next objectives, if modification is desired, ADD will not
         * do anything - use ADD_TO_EXISTING.
         */
        ADD,

        /**
         * Removes the objective. Can be used for any flow objective.
         */
        REMOVE,

        /**
         * Add to an existing Next Objective. Should not be used for any other
         * objective.
         */
        ADD_TO_EXISTING,

        /**
         * Remove from an existing Next Objective. Should not be used for any
         * other objective.
         */
        REMOVE_FROM_EXISTING,

        /**
         * Modify an existing Next Objective. Can be used to modify group buckets.
         */
        MODIFY,

        /**
         * Verifies that an existing Next Objective's collection of treatments
         * are correctly represented by the underlying implementation of the objective.
         * Corrective action is taken if discrepancies are found during verification.
         * For example, if the next objective defines 3 sets of treatments, which
         * are meant to be implemented as 3 buckets in a group, but verification
         * finds less or more buckets, then the appropriate buckets are added or
         * removed to match the objective.
         *
         * Should not be used for any other objective.
         */
        VERIFY
    }

    /**
     * An identifier for this objective.
     *
     * @return an integer
     */
    int id();

    /**
     * The priority for this objective.
     *
     * @return an integer
     */
    int priority();

    /**
     * The application which applied this objective.
     *
     * @return an application id
     */
    ApplicationId appId();

    /**
     * The timeout for this objective.
     *
     * @return an integer
     */
    int timeout();

    /**
     * Whether this objective is permanent.
     *
     * @return a boolean
     */
    boolean permanent();

    /**
     * The type of operation for this objective.
     *
     * @return an operation
     */
    Operation op();

    /**
     * Obtains an optional context.
     *
     * @return optional; which will be empty if there is no context.
     * Otherwise it will return the context.
     */
    Optional<ObjectiveContext> context();

    /**
     * Returns a new builder set to create a copy of this objective.
     *
     * @return new builder
     */
    Objective.Builder copy();

    /**
     * An objective builder.
     */
    interface Builder {
        /**
         * Makes the filtering objective temporary.
         *
         * @param timeout a timeout
         * @return an objective builder
         */
        Builder makeTemporary(int timeout);

        /**
         * Makes the filtering objective permanent.
         *
         * @return an objective builder
         */
        Builder makePermanent();

        /**
         * Specifies the application which applied the filter.
         *
         * @param appId an application id
         * @return an objective builder
         */
        Builder fromApp(ApplicationId appId);

        /**
         * Sets the priority for this objective.
         *
         * @param priority an integer
         * @return an objective builder
         */
        Builder withPriority(int priority);

        /**
         * Builds the objective that will be added.
         *
         * @return an objective
         */
        Objective add();

        /**
         * Builds the objective that will be removed.
         *
         * @return an objective.
         */
        Objective remove();

        /**
         * Builds the objective that will be added.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return an objective
         */
        Objective add(ObjectiveContext context);

        /**
         * Builds the objective that will be removed.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return an objective
         */
        Objective remove(ObjectiveContext context);
    }
}
