/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.flow;

import org.onlab.util.Identifier;

/**
 * Representation of a Flow ID.
 */
public final class FlowId extends Identifier<Long> {

    private FlowId(long id) {
        super(id);
    }

    /**
     * Creates a flow ID from a long value.
     *
     * @param id long value
     * @return flow ID
     */
    public static FlowId valueOf(long id) {
        return new FlowId(id);
    }

    /**
     * Gets the flow ID value.
     *
     * @return flow ID value as long
     */
    public long value() {
        return this.identifier;
    }

    @Override
    public String toString() {
        return Long.toHexString(identifier);
    }
}
