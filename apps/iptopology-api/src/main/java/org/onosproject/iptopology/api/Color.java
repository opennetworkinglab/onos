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

import java.util.Objects;

/**
 * Represents administrative group color.
 * bit mask - least significant bit is referred to as 'group 0',
 * and the most significant bit is referred to as 'group 31'
 */
public class Color {
    private final int color;

    /**
     * Constructor to initialize its parameter.
     *
     * @param color assigned by the network administrator
     */
    public Color(int color) {
        this.color = color;
    }

    /**
     * Obtains administrative group.
     *
     * @return administrative group
     */
    public int color() {
        return color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Color) {
            Color other = (Color) obj;
            return Objects.equals(color, other.color);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("color", color)
                .toString();
    }
}