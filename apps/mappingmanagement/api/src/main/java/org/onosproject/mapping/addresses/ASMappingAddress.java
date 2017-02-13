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
package org.onosproject.mapping.addresses;

import java.util.Objects;

/**
 * Implementation of AS mapping address.
 */
public final class ASMappingAddress implements MappingAddress {

    private final String asNumber;

    /**
     * Default constructor of ASMappingAddress.
     *
     * @param asNumber AS number
     */
    ASMappingAddress(String asNumber) {
        this.asNumber = asNumber;
    }

    @Override
    public Type type() {
        return Type.AS;
    }

    /**
     * Obtains AS number.
     *
     * @return AS number
     */
    public String asNumber() {
        return this.asNumber;
    }

    @Override
    public String toString() {
        return type().toString() + TYPE_SEPARATOR + asNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), asNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ASMappingAddress) {
            ASMappingAddress that = (ASMappingAddress) obj;
            return Objects.equals(asNumber, that.asNumber);
        }
        return false;
    }
}
