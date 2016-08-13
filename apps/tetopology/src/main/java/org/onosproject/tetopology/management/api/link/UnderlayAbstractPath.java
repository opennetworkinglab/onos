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

import java.util.List;
import java.util.Objects;

import org.onosproject.tetopology.management.api.node.TeNetworkTopologyId;

import com.google.common.base.MoreObjects;

/**
 *  Represents the common defintion of an underlay path that supports a TE link.
 */
public class UnderlayAbstractPath {
    private TeNetworkTopologyId ref;
    private List<PathElement> pathElements;

    /**
     * Creates an instance of UnderlayAbstractPath.
     */
    public UnderlayAbstractPath() {
    }

    /**
     * Sets the TE Topology reference.
     *
     * @param ref the ref to set
     */
    public void setRef(TeNetworkTopologyId ref) {
        this.ref = ref;
    }

    /**
     * Sets the list of path elements.
     *
     * @param pathElements the pathElement to set
     */
    public void setPathElement(List<PathElement> pathElements) {
        this.pathElements = pathElements;
    }

    /**
     * Returns the TE Topology reference.
     *
     * @return value of TE network topology identifier
     */
    public TeNetworkTopologyId ref() {
        return ref;
    }

    /**
     * Returns the list of path elements.
     *
     * @return list of path elements
     */
    public List<PathElement> pathElements() {
        return pathElements;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, pathElements);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UnderlayAbstractPath) {
            UnderlayAbstractPath other = (UnderlayAbstractPath) obj;
            return Objects.equals(ref, other.ref) &&
                 Objects.equals(pathElements, other.pathElements);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("ref", ref)
            .add("pathElements", pathElements)
            .toString();
    }
}
