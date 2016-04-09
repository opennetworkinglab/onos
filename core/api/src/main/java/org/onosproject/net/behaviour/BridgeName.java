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
package org.onosproject.net.behaviour;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Represents for a bridge name.
 */
public final class BridgeName {

    private final String name;

    // Public construction is prohibited
    private BridgeName(String name) {
        this.name = name;
    }

    /**
     * Creates a bridge name using the supplied string.
     *
     * @param name bridge name
     * @return BridgeName
     */
    public static BridgeName bridgeName(String name) {
        return new BridgeName(name);
    }

    /**
     * Returns the bridge name string.
     *
     * @return name string
     */
    public String name() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BridgeName) {
            final BridgeName that = (BridgeName) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.name, that.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .toString();
    }

}
