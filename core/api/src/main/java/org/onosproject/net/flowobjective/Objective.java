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

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;

import java.util.Optional;

/**
 * Base representation of an flow description.
 */
@Beta
public interface Objective {

    boolean DEFAULT_PERMANENT = true;
    int DEFAULT_TIMEOUT = 0;
    int DEFAULT_PRIORITY = 32768;

    /**
     * Type of operation.
     */
    enum Operation {
        /**
         * Adds the objective.
         */
        ADD,

        /**
         * Removes the objective.
         */
        REMOVE
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
    }

}
