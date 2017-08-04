/*
 * Copyright 2015-present Open Networking Foundation
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

import java.util.Objects;

/**
 * The class representing a port number.
 * This class is immutable.
 */
public final class OvsdbPortNumber {

    private final long value;

    /**
     * Constructor from a long value.
     *
     * @param value the port number to use
     */
    public OvsdbPortNumber(long value) {
        this.value = value;
    }

    /**
     * Gets the value of port number.
     *
     * @return the value of port number
     */
    public long value() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbPortNumber) {
            final OvsdbPortNumber ovsdbPortNumber = (OvsdbPortNumber) obj;
            return Objects.equals(this.value, ovsdbPortNumber.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("value", value).toString();
    }
}
