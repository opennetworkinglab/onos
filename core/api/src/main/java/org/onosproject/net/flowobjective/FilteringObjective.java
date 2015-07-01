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
import org.onosproject.net.flow.criteria.Criterion;

import java.util.Collection;

/**
 * Represents a filtering flow objective. Each filtering flow objective
 * is made up of a key (criterion) to a set of criteria. Using this information
 * a pipeline aware driver will decide how this objective should be mapped
 * to the specific device pipeline. For example, consider the following
 * filtering objective:
 *
 * portX -&gt; {MAC1, IP1, MAC2}
 *
 * The driver could decide to pass L3 packet to the L3 table and L2 packets to
 * the L2 table for packets arriving on portX.
 *
 * Filtering objectives do not only represent what should be permitted into the
 * pipeline but can also be used to deny or drop unwanted packets by specifying
 * the appropriate type of filtering objective. It is also important to note
 * that submitting a filtering objective does not necessarily result in rules
 * programmed at the switch, the driver is free to decide when these rules are
 * programmed. For example, a filtering rule may only be programmed once a
 * corresponding forwarding objective has been received.
 */
@Beta
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
         * Assigns an application id.
         *
         * @param appId an application id
         * @return a filtering builder
         */
        Builder fromApp(ApplicationId appId);

        /**
         * Builds the filtering objective that will be added.
         *
         * @return a filtering objective
         */
        FilteringObjective add();

        /**
         * Builds the filtering objective that will be removed.
         *
         * @return a filtering objective.
         */
        FilteringObjective remove();

        /**
         * Builds the filtering objective that will be added.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a filtering objective
         */
        FilteringObjective add(ObjectiveContext context);

        /**
         * Builds the filtering objective that will be removed.
         * The context will be used to notify the calling application.
         *
         * @param context an objective context
         * @return a filtering objective
         */
        FilteringObjective remove(ObjectiveContext context);


    }

}
