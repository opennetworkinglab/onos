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

/**
 * Model of an action runtime parameter in a protocol-independent pipeline.
 */
@Beta
public interface PiActionParamModel {

    /**
     * Returns the ID of this action parameter.
     *
     * @return action parameter ID
     */
    PiActionParamId id();

    /**
     * Return the size in bits of this action parameter.
     * It returns -1 if the bit width of the action parameters is not predefined.
     *
     * @return size in bits, -1 if not predefined
     */
    int bitWidth();

    /**
     * Return true is the action parameters has a predefined bit width.
     * It returns false if it can have arbitrary bit width.
     *
     * @return True if the action parameter has predefined bit width, false otherwise
     */
    boolean hasBitWidth();
}
