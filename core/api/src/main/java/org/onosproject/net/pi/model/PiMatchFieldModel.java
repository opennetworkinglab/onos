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
 * Model of a table match field in a protocol-independent pipeline.
 */
@Beta
public interface PiMatchFieldModel {

    /**
     * Returns the ID of this match field.
     *
     * @return match field ID
     */
    PiMatchFieldId id();

    /**
     * Returns the number of bits matched by this field.
     * It returns -1 if the bit width of the match field is not predefined.
     *
     * @return number of bits, -1 in case it is not predefined
     */
    int bitWidth();

    /**
     * Return true is the match field has a predefined bit width.
     * It returns false if it can have arbitrary bit width.
     *
     * @return True if the match field has predefined bit width, false otherwise
     */
    boolean hasBitWidth();

    /**
     * Returns the type of match applied to this field.
     *
     * @return a match type
     */
    PiMatchType matchType();
}
