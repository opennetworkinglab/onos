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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents Route type of the prefix in the OSPF domain.
 */
public class RouteType {
    private final Type routeType;

    /**
     * Enum to provide Route type.
     */
    public enum Type {
        Intra_Area(1), Inter_Area(2), External_1(3), External_2(4), NSSA_1(5), NSSA_2(6);
        int value;

        /**
         * Constructor to assign value.
         *
         * @param val route type
         */
        Type(int val) {
            value = val;
        }

        static Map<Integer, Type> map = new HashMap<>();

        static {
           for (Type type : Type.values()) {
              map.put(type.value, type);
           }
        }

        /**
         * A method that returns enum value.
         *
         * @param value route type
         * @return Enum value
         */
        public static Type getEnumType(int value) {
            return map.get(value);
         }

        /**
         * Provides route type.
         *
         * @return route type
         */
        public byte type() {
            return (byte) value;
        }
    }

    /**
     * Constructor to initialize routeType.
     *
     * @param routeType  Route type
     */
    public RouteType(Type routeType) {
        this.routeType = routeType;
    }

    /**
     * Provides Route type of the prefix.
     *
     * @return  Route type
     */
    public Type routeType() {
        return routeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof RouteType) {
            RouteType other = (RouteType) obj;
            return Objects.equals(routeType, other.routeType);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("routeType", routeType)
                .toString();
    }
}