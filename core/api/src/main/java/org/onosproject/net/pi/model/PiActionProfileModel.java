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

/**
 * Model of an action profile in a protocol-independent pipeline.
 */
@Beta
public interface PiActionProfileModel {

    /**
     * Returns the ID of this action profile.
     *
     * @return action profile ID
     */
    PiActionProfileId id();

    /**
     * Returns the collection of table IDs that use this action profile.
     *
     * @return collection of table models
     */
    Collection<PiTableId> tables();

    /**
     * Returns true if this action profile implements dynamic selection, false
     * otherwise.
     *
     * @return true if action profile implements dynamic selection, false
     * otherwise
     */
    boolean hasSelector();

    /**
     * Returns the maximum number of member entries that this action profile can
     * hold.
     *
     * @return maximum number of member entries
     */
    long size();

    /**
     * Returns the maximum number of members a group of this action profile can
     * hold. This method is meaningful only if the action profile implements
     * dynamic selection. 0 signifies that an explicit limit is not set.
     *
     * @return maximum number of members a group can hold
     */
    int maxGroupSize();
}
