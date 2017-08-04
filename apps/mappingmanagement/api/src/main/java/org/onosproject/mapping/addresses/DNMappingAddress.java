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
package org.onosproject.mapping.addresses;

import java.util.Objects;

/**
 * Implementation of distinguished name mapping address.
 */
public final class DNMappingAddress implements MappingAddress {

    private final String name;

    /**
     * Default constructor of DNMappingAddress.
     *
     * @param name distinguished name
     */
    DNMappingAddress(String name) {
        this.name = name;
    }

    @Override
    public Type type() {
        return Type.DN;
    }

    /**
     * Obtains distinguished name.
     *
     * @return distinguished name
     */
    public String name() {
        return this.name;
    }

    @Override
    public String toString() {
        return type().toString() + TYPE_SEPARATOR + name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DNMappingAddress) {
            DNMappingAddress that = (DNMappingAddress) obj;
            return Objects.equals(name, that.name);
        }
        return false;
    }
}
