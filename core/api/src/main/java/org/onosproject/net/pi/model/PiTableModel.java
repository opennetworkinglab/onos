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

import java.util.Collection;

/**
 * Model of a match+action table in a protocol-independent pipeline.
 */
@Beta
public interface PiTableModel {

    /**
     * Returns the name of this table.
     *
     * @return a string value
     */
    String name();

    /**
     * Returns the maximum number of entries supported by this table.
     *
     * @return an integer value
     */
    int maxSize();

    /**
     * Returns true if this table has counters, false otherwise.
     *
     * @return a boolean value
     */
    boolean hasCounters();

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
    Collection<PiTableMatchFieldModel> matchFields();

    /**
     * Returns the actions supported by this table.
     *
     * @return a collection of action models
     */
    Collection<PiActionModel> actions();
}
