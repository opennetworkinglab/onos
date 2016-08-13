/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Implementation of Automous System (AS) number as an ElementType.
 */
public class AsNumber implements ElementType {
    private final int asNumber;

    /**
     * Creates an instance of AsNumber.
     *
     * @param asNumber value of autonomous system number
     */
    public AsNumber(int asNumber) {
        this.asNumber = asNumber;
    }

    /**
     * Returns the asNumber.
     *
     * @return value of the autonomous system number
     */
    public int getAsNumber() {
        return asNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(asNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AsNumber) {
            AsNumber other = (AsNumber) obj;
            return Objects.equals(asNumber, other.asNumber);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("asNumber", asNumber)
            .toString();
    }

}
