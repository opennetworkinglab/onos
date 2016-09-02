/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pce.pceservice;

import org.onosproject.net.NetworkResource;

import com.google.common.annotations.Beta;

import java.util.Objects;

/**
 * Representation of explicit path info consists of contraints (strict / loose) to compute path.
 */
@Beta
public final class ExplicitPathInfo {

    private final Type type;

    //Can be Link or DeviceId
    private final NetworkResource value;

    public enum Type {
        /**
         * Signifies that path includes strict node or link.
         */
        STRICT(0),

        /**
         * Signifies that path includes loose node or link.
         */
        LOOSE(1);

        int value;

        /**
         * Assign val with the value as the type.
         *
         * @param val type
         */
        Type(int val) {
            value = val;
        }

        /**
         * Returns value of type.
         *
         * @return type
         */
        public byte type() {
            return (byte) value;
        }
    }

    /**
     * Creates instance of explicit path object.
     *
     * @param type specifies whether strict or loose node/link
     * @param value specifies deviceId or link
     */
    public ExplicitPathInfo(Type type, NetworkResource value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Returns explicit path type.
     *
     * @return explicit path type as strict/loose
     */
    public Type type() {
        return type;
    }

    /**
     * Returns deviceId or link.
     *
     * @return deviceId or link
     */
    public NetworkResource value() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ExplicitPathInfo) {
            final ExplicitPathInfo other = (ExplicitPathInfo) obj;
            return Objects.equals(this.type, other.type)
                    && Objects.equals(this.value, other.value);
        }
        return false;
    }
}
