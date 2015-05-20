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
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

/**
 * Represents a description of which types of traffic need to
 * be forwarded through the device. A forwarding objective may
 * in multiple rules at the device.
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
        VERSATILE
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
         * Builds the forwarding objective that will be added.
         *
         * @return a forwarding objective
         */
        ForwardingObjective add();

        /**
         * Builds the forwarding objective that will be removed.
         *
         * @return a forwarding objective.
         */
        ForwardingObjective remove();

        /**
         * Builds the forwarding objective that will be added.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a forwarding objective
         */
        ForwardingObjective add(ObjectiveContext context);

        /**
         * Builds the forwarding objective that will be removed.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a forwarding objective
         */
        ForwardingObjective remove(ObjectiveContext context);
    }
}
