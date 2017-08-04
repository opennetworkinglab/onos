/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.topology;

import org.onlab.util.Identifier;

/**
 * Representation of the topology cluster identity.
 */
public final class ClusterId extends Identifier<Integer> {

    // Public construction is prohibit
    private ClusterId(int id) {
        super(id);
    }

    /**
     * Returns the cluster identifier, represented by the specified integer
     * serial number.
     *
     * @param id integer serial number
     * @return cluster identifier
     */
    public static ClusterId clusterId(int id) {
        return new ClusterId(id);
    }

    /**
     * Returns the backing integer index.
     *
     * @return backing integer index
     */
    public int index() {
        return identifier;
    }
}
