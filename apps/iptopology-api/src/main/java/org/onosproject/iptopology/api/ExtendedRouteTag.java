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
 * Represents the extended igp administrative tags of the prefix.
 */
public class ExtendedRouteTag {
    private final long extRouteTag;

    /**
     * Constructor to initialize its parameter.
     *
     * @param extRouteTag extended ISIS route tag
     */
    public ExtendedRouteTag(long extRouteTag) {
        this.extRouteTag = extRouteTag;
    }

    /**
     * Obtains extended igp administrative tags.
     *
     * @return extended igp administrative tags
     */
    public long extRouteTag() {
        return extRouteTag;
    }

    @Override
    public int hashCode() {
        return Objects.hash(extRouteTag);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof ExtendedRouteTag) {
            ExtendedRouteTag other = (ExtendedRouteTag) obj;
            return Objects.equals(extRouteTag, other.extRouteTag);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("extRouteTag", extRouteTag)
                .toString();
    }
}