/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.meter;

import com.google.common.base.MoreObjects;

/**
 * Representation of an operation on the meter table.
 */
public class MeterOperation {

    /**
     * Tyoe of meter operation.
     */
    public enum Type {
        ADD,
        REMOVE,
        MODIFY
    }

    private final Meter meter;
    private final Type type;


    public MeterOperation(Meter meter, Type type) {
        this.meter = meter;
        this.type = type;
    }

    /**
     * Returns the type of operation.
     *
     * @return type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the meter.
     *
     * @return a meter
     */
    public Meter meter() {
        return meter;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("meter", meter)
                .add("type", type)
                .toString();
    }
}
