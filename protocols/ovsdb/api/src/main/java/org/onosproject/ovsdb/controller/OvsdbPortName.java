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
package org.onosproject.ovsdb.controller;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * The class representing a port number.
 * This class is immutable.
 */
public final class OvsdbPortName {

    private final String value;

    /**
     * Constructor from a String.
     *
     * @param value the port name to use
     */
    public OvsdbPortName(String value) {
        checkNotNull(value, "value is not null");
        this.value = value;
    }

    /**
     * Gets the value of port name.
     *
     * @return the value of port name
     */
    public String value() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbPortName) {
            final OvsdbPortName otherOvsdbPortName = (OvsdbPortName) obj;
            return Objects.equals(this.value, otherOvsdbPortName.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("value", value).toString();
    }
}
