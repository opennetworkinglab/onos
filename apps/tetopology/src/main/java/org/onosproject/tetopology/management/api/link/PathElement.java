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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Represent a path element.
 */
public class PathElement {
    private final long pathElementId;
    private final ElementType type;

    /**
     * Creates an instance of PathElement.
     *
     * @param pathElementId path element identifier
     * @param type path element type
     */
    public PathElement(long pathElementId, ElementType type) {
        this.pathElementId = pathElementId;
        this.type = type;
    }

    /**
     * Returns the path element identifier.
     *
     * @return path element id
     */
    public long pathElementId() {
        return pathElementId;
    }

    /**
     * Returns the path element type.
     *
     * @return path element type
     */
    public ElementType type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pathElementId, type);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof PathElement) {
            PathElement that = (PathElement) object;
            return Objects.equal(this.pathElementId, that.pathElementId) &&
                    Objects.equal(this.type, that.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pathElementId", pathElementId)
                .add("type", type)
                .toString();
    }

}
