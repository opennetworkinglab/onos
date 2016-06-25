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
 * Autonomous system Number class (32 Bit ASNumber).
 */
public class AsNumber {
    private final int asNum;

    /**
     * Constructor to set As number.
     *
     * @param asNum As number
     */
    public AsNumber(int asNum) {
        this.asNum = asNum;
    }

    /**
     * Obtain autonomous system number.
     *
     * @return autonomous system number
     */
    public int asNum() {
        return asNum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(asNum);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AsNumber) {
            AsNumber other = (AsNumber) obj;
            return Objects.equals(asNum, other.asNum);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("asNum", asNum)
                .toString();
    }
}