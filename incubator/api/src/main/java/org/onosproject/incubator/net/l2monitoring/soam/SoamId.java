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
package org.onosproject.incubator.net.l2monitoring.soam;

import org.onlab.util.Identifier;

/**
 * Identifier for SOAM objects.
 */
public class SoamId extends Identifier<Integer> {
    protected SoamId(int id) {
        super(id);
    }

    /**
     * Creates a dm ID from a int value.
     *
     * @param id int value
     * @return dm ID
     */
    public static SoamId valueOf(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("SOAM Value must be unsigned."
                    + "Rejecting: " + id);
        }
        return new SoamId(id);
    }

    /**
     * Gets the dm ID value.
     *
     * @return dm ID value as int
     */
    public int value() {
        return this.identifier;
    }

    @Override
    public String toString() {
        return String.valueOf(identifier);
    }
}
