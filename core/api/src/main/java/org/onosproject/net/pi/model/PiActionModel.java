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
 * Model of an action with runtime parameters in a protocol-independent pipeline.
 */
@Beta
public interface PiActionModel {

    /**
     * Returns the ID of the action.
     *
     * @return action ID
     */
    PiActionId id();

    /**
     * Returns the model of the action's parameter defined by the given ID, if present.
     *
     * @param paramId parameter ID
     * @return action parameter model
     */
    Optional<PiActionParamModel> param(PiActionParamId paramId);

    /**
     * Returns the collection of all parameter models for the action, or an empty collection if this action has no
     * parameters.
     *
     * @return collection of action parameter models
     */
    Collection<PiActionParamModel> params();
}
