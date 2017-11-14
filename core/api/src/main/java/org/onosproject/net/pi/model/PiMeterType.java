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
 * Types of meter in protocol-independent pipeline.
 */
@Beta
public enum PiMeterType {

    /**
     * Identifies a meter associated to a match-action table, where meter cells are directly associated to table
     * entries.
     */
    DIRECT,

    /**
     * Identifies a meter not associated with any other resource.
     */
    INDIRECT
}
