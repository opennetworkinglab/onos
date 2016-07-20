/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.region;

import org.onlab.util.Identifier;

/**
 * Region identifier backed by a string value.
 */
public final class RegionId extends Identifier<String> {

    /**
     * Constructor for serialization.
     */
    private RegionId() {
        super();
    }

    /**
     * Constructs the ID corresponding to a given string value.
     *
     * @param value the underlying value of this ID
     */
    private RegionId(String value) {
        super(value);
    }

    /**
     * Creates a new region identifier.
     *
     * @param id backing identifier value
     * @return region identifier
     */
    public static RegionId regionId(String id) {
        return new RegionId(id);
    }

}
