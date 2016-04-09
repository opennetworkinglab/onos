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
 * Represents protection capabilities of the link.
 */
public class ProtectionType {
    private final LinkProtectionType protectionType;

    /**
     * Enum to provide Link Protection type.
     */
    public enum LinkProtectionType {
        Extra_Traffic(1), Unprotected(2), Shared(4), Enhanced(0x20), Dedicated_OneIsToOne(8),
        Dedicated_OnePlusOne(0x10), Reserved(0x40);
        int value;

        /**
         * Constructor to assign value.
         *
         * @param val link protection type
         */
        LinkProtectionType(int val) {
            value = val;
        }

        static Map<Integer, LinkProtectionType> map = new HashMap<>();

        static {
           for (LinkProtectionType type : LinkProtectionType.values()) {
              map.put(type.value, type);
           }
        }

        /**
         * A method that returns enum value.
         *
         * @param value link protection type
         * @return Enum value
         */
        public static LinkProtectionType getEnumType(int value) {
            return map.get(value);
         }

        /**
         * Provides Link protection type.
         *
         * @return protection type
         */
        public byte type() {
            return (byte) value;
        }
    }

    /**
     * Constructor to initialize protection type.
     *
     * @param protectionType link protection type
     */
    public ProtectionType(LinkProtectionType protectionType) {
        this.protectionType = protectionType;
    }

    /**
     * Provides protection capabilities of the link.
     *
     * @return link protection type.
     */
    public LinkProtectionType protectionType() {
        return protectionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(protectionType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ProtectionType) {
            ProtectionType other = (ProtectionType) obj;
            return Objects.equals(protectionType, other.protectionType);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("protectionType", protectionType)
                .toString();
    }
}