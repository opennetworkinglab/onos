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
 * Represents Position of device in the network.
 */
public class Position {
    private final Boolean asbr;
    private final Boolean abr;

    /**
     * Constructor to set position of device.
     *
     * @param asbr autonomous system boundary router
     * @param abr area boundary router
     */
    public Position(Boolean asbr, Boolean abr) {
        this.asbr = asbr;
        this.abr = abr;
    }

    /**
     * obtain whether the device is autonomous system boundary router or not.
     *
     * @return autonomous system boundary router or not
     */
    Boolean asbr() {
        return asbr;
    }

    /**
     * obtain whether the device is area boundary router or not.
     *
     * @return area boundary router or not
     */
    Boolean abr() {
        return abr;
    }

    @Override
    public int hashCode() {
        return Objects.hash(abr, asbr);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Position) {
            Position other = (Position) obj;
            return Objects.equals(abr, other.abr) && Objects.equals(asbr, other.asbr);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("abrBit", abr)
                .add("asbrBit", asbr)
                .toString();
    }
}