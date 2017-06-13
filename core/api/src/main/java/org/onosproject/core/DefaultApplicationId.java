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
package org.onosproject.core;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Application identifier.
 */
public class DefaultApplicationId implements ApplicationId {

    private static final int NAME_MAX_LENGTH = 1024;
    private final short id;
    private final String name;

    /**
     * Creates a new application ID.
     *
     * @param id   application identifier
     * @param name application name
     */
    public DefaultApplicationId(int id, String name) {
        checkArgument(0 <= id && id <= Short.MAX_VALUE, "id is outside range");
        if (name != null) {
            checkArgument(name.length() <= NAME_MAX_LENGTH, "name exceeds maximum length " + NAME_MAX_LENGTH);
        }
        this.id = (short) id;
        this.name = name;
    }

    // Constructor for serializers.
    private DefaultApplicationId() {
        this.id = 0;
        this.name = null;
    }

    @Override
    public short id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultApplicationId) {
            DefaultApplicationId other = (DefaultApplicationId) obj;
            return Objects.equals(this.name, other.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).add("name", name).toString();
    }

}
