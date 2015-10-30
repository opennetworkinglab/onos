/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Default implementation of {@link GroupId}.
 */
public class DefaultGroupId implements GroupId {

    private final int id;

    public DefaultGroupId(int id) {
        this.id = id;
    }

    // Constructor for serialization
    private DefaultGroupId() {
        this.id = 0;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DefaultGroupId)) {
            return false;
        }
        final DefaultGroupId other = (DefaultGroupId) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
