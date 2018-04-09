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

/**
 * Represents a description of which types of traffic need to
 * be forwarded through the device. A forwarding objective may
 * result in multiple rules at the device. There are two main types
 * of forwarding objectives:
 *
 *  - Versatile
 *  - Specific
 *
 * A versatile forwarding objective represents a composite rule that matches
 * two or more header fields. The use of versatile usually indicates that this
 * rule should be inserted in its entirety into the ACL table. Although,
 * drivers for some devices are free to implement this differently.
 *
 * A specific forwarding objective represents a specific rule matching one or
 * more header fields. The installation of this rule may result in several rules
 * at the device. For example, one per table type.
 *
 * There is one additional type of forwarding objective:
 *
 *   - Egress
 *
 * An egress forwarding objecrive represents a flow rule that is inserted into
 * egress tables, only if they exist in the device.
 *
 */
@Beta
public interface ForwardingObjective extends Objective {

    /**
     * Represents whether this objective is monolithic or
     * may be broken down into parts.
     */
    enum Flag {
        /**
         * A decomposable objective.
         */
        SPECIFIC,

        /**
         * A monolithic objective.
         */
        VERSATILE,

        /**
         * An objective to program egress pipeline.
         */
        EGRESS
    }

    /**
     * Obtain the selector for this objective.
     *
     * @return a traffic selector
     */
    TrafficSelector selector();

    /**
     * Obtain the traffic treatment for this objective. Mutually exclusive with
     * 'treatment'.
     *
     * @return an integer
     */
    Integer nextId();

    /**
     * A traffic treatment for this forwarding objective. Mutually exclusive
     * with a nextId.
     *
     * @return a traffic treatment
     */
    TrafficTreatment treatment();

    /**
     * Obtain the type of this objective.
     *
     * @return a flag type
     */
    Flag flag();

    /**
     * Auxiliary optional information provided to the device driver. Typically
     * conveys information about selectors (matches) that are intended to
     * use this Forwarding Objective.
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
     * A forwarding objective builder.
     */
    interface Builder extends Objective.Builder {

        /**
         * Assigns a selector to the forwarding objective.
         *
         * @param selector a traffic selector
         * @return a forwarding objective builder
         */
        Builder withSelector(TrafficSelector selector);

        /**
         * Assigns a next step to the forwarding objective.
         *
         * @param nextId a next objective id.
         * @return a forwarding objective builder
         */
        Builder nextStep(int nextId);

        /**
         * Assigns the treatment for this forwarding objective.
         *
         * @param treatment a traffic treatment
         * @return a forwarding objective
         */
        Builder withTreatment(TrafficTreatment treatment);

        /**
         * Assigns the flag to the forwarding objective.
         *
         * @param flag a flag
         * @return a forwarding objective builder
         */
        Builder withFlag(Flag flag);

        /**
         * Set meta information related to this forwarding objective.
         *
         * @param selector match conditions
         * @return an objective builder
         */
        Builder withMeta(TrafficSelector selector);

        /**
         * Assigns an application id.
         *
         * @param appId an application id
         * @return a filtering builder
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
         * Makes the filtering objective permanent.
         *
         * @return an objective builder
         */
        @Override
        Builder makePermanent();

        /**
         * Builds the forwarding objective that will be added.
         *
         * @return a forwarding objective
         */
        @Override
        ForwardingObjective add();

        /**
         * Builds the forwarding objective that will be removed.
         *
         * @return a forwarding objective.
         */
        @Override
        ForwardingObjective remove();

        /**
         * Builds the forwarding objective that will be added.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a forwarding objective
         */
        @Override
        ForwardingObjective add(ObjectiveContext context);

        /**
         * Builds the forwarding objective that will be removed.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a forwarding objective
         */
        @Override
        ForwardingObjective remove(ObjectiveContext context);
    }
}
