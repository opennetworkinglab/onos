/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.util.List;
import java.util.Optional;

/**
 * Model of an action with runtime parameters in a protocol-independent pipeline.
 */
@Beta
public interface PiActionModel {
    /**
     * Returns the name of this action.
     *
     * @return a string value
     */
    String name();

    /**
     * Returns the model of this action's parameter defined by the given name, if present.
     *
     * @param name action name
     * @return action parameter model
     */
    Optional<PiActionParamModel> param(String name);

    /**
     * Returns the list of action parameter models, ordered according to the same action parameters
     * defined in the pipeline model.
     *
     * @return list of action parameter models
     */
    List<PiActionParamModel> params();
}
