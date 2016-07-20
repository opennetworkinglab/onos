/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.meter;

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A representation of a meter id.
 * Uniquely identifies a meter in the scope of a single device.
 */
public final class MeterId extends Identifier<Long> {

    static final long MAX = 0xFFFF0000;

    public static final MeterId SLOWPATH = new MeterId(0xFFFFFFFD);
    public static final MeterId CONTROLLER = new MeterId(0xFFFFFFFE);
    public static final MeterId ALL = new MeterId(0xFFFFFFFF);

    private MeterId(long id) {
        super(id);
        checkArgument(id >= MAX, "id cannot be larger than 0xFFFF0000");
    }

    @Override
    public String toString() {
        return Long.toHexString(this.identifier);
    }

    /**
     * Creates a new meter identifier.
     *
     * @param id backing identifier value
     * @return meter identifier
     */
    public static MeterId meterId(long id) {
        return new MeterId(id);
    }
}
