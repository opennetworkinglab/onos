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
 * Representation of a path element.
 */
public class PathElement {
    private final long pathElementId;
    private final long teNodeId;
    private final ElementType type;
    private final boolean loose;

    /**
     * Creates a path element.
     *
     * @param pathElementId path element identifier
     * @param teNodeId      identifier of the TE node to which this
     *                      path element belongs
     * @param type          path element type
     * @param loose         loose if true; strict if false
     */
    public PathElement(long pathElementId, long teNodeId,
                       ElementType type, boolean loose) {
        this.pathElementId = pathElementId;
        this.teNodeId = teNodeId;
        this.type = type;
        this.loose = loose;
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
     * Returns the TE node identifier.
     *
     * @return te node id
     */
    public long teNodeId() {
        return teNodeId;
    }

    /**
     * Returns the path element type.
     *
     * @return path element type
     */
    public ElementType type() {
        return type;
    }

    /**
     * Returns the loose flag. true = loose; false = strict.
     *
     * @return loose value
     */
    public boolean loose() {
        return loose;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pathElementId, teNodeId, type, loose);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof PathElement) {
            PathElement that = (PathElement) object;
            return Objects.equal(pathElementId, that.pathElementId) &&
                    Objects.equal(teNodeId, that.teNodeId) &&
                    Objects.equal(type, that.type) &&
                    Objects.equal(loose, that.loose);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pathElementId", pathElementId)
                .add("teNodeId", teNodeId)
                .add("type", type)
                .add("loose", loose)
                .toString();
    }

}
