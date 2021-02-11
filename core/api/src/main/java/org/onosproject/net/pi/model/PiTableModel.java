/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.pi.model;

import com.google.common.annotations.Beta;

import java.util.Collection;
import java.util.Optional;

/**
 * Model of a match+action table in a protocol-independent pipeline.
 */
@Beta
public interface PiTableModel {

    /**
     * Returns the ID of this table.
     *
     * @return a string value
     */
    PiTableId id();

    /**
     * Returns the type of this table.
     *
     * @return table type
     */
    PiTableType tableType();

    /**
     * Returns the model of the action profile that implements this table.
     * Meaningful if this table is of type {@link PiTableType#INDIRECT},
     * otherwise returns null.
     *
     * @return action profile ID
     */
    PiActionProfileModel actionProfile();

    /**
     * Returns the maximum number of entries supported by this table.
     *
     * @return an integer value
     */
    long maxSize();

    /**
     * Returns a collection of direct counters associated to this table.
     *
     * @return collection of direct counters
     */
    Collection<PiCounterModel> counters();

    /**
     * Returns a collection of direct meters associated to this table.
     *
     * @return collection of direct meters
     */
    Collection<PiMeterModel> meters();

    /**
     * Returns true if this table supports aging, false otherwise.
     *
     * @return a boolean value
     */
    boolean supportsAging();

    /**
     * Returns the collection of match fields supported by this table.
     *
     * @return a collection of match field models
     */
    Collection<PiMatchFieldModel> matchFields();

    /**
     * Returns the actions supported by this table.
     *
     * @return a collection of action models
     */
    Collection<PiActionModel> actions();

    /**
     * Returns the model of the constant default action associated with this
     * table, if any.
     *
     * @return optional default action model
     */
    Optional<PiActionModel> constDefaultAction();

    /**
     * Returns true if the table is populated with static entries that cannot be
     * modified by the control plane at runtime.
     *
     * @return true if table is populated with static entries, false otherwise
     */
    boolean isConstantTable();

    /**
     * Returns true if the table supports one shot only when associated to an
     * action profile.
     *
     * @return true if table support one shot only, false otherwise
     */
    boolean oneShotOnly();

    /**
     * Returns the action model associated with the given ID, if present. If not
     * present, it means that this table does not support such an action.
     *
     * @param actionId action ID
     * @return optional action model
     */
    Optional<PiActionModel> action(PiActionId actionId);

    /**
     * Returns the match field model associated with the given ID, if present.
     * If not present, it means that this table does not support such a match
     * field.
     *
     * @param matchFieldId match field ID
     * @return optional match field model
     */
    Optional<PiMatchFieldModel> matchField(PiMatchFieldId matchFieldId);

}
