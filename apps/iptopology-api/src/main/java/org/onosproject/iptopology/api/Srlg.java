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
 * Represents Shared Risk Link Group information.
 */
public class Srlg {
    private final int srlgGroup;

    /**
     * Constructor to initialize its parameter.
     *
     * @param srlgGroup list of Shared Risk Link Group value
     */
    public Srlg(int srlgGroup) {
        this.srlgGroup = srlgGroup;
    }

    /**
     * Provides Shared Risk link group.
     *
     * @return Shared Risk link group value
     */
    public int srlgGroup() {
        return srlgGroup;
    }

    @Override
    public int hashCode() {
        return Objects.hash(srlgGroup);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Srlg) {
            Srlg other = (Srlg) obj;
            return Objects.equals(srlgGroup, other.srlgGroup);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("srlgGroup", srlgGroup)
                .toString();
    }
}