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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 * Represents the common definition of an underlay path that supports a TE link.
 */
public class UnderlayAbstractPath {
    private final List<PathElement> pathElements;
    private final Boolean loose;

    /**
     * Creates a underlay abstract path.
     *
     * @param pathElements the list of elements along the path
     * @param loose        loose if true, or otherwise strict
     */
    public UnderlayAbstractPath(List<PathElement> pathElements, Boolean loose) {
        this.pathElements = Lists.newArrayList(pathElements);
        this.loose = loose;
    }

    /**
     * Returns the loose flag, indicating whether the path is loose or strict.
     *
     * @return true if the path is loose, false if it is strict.
     */
    public Boolean loose() {
        return loose;
    }

    /**
     * Returns the list of path elements.
     *
     * @return list of path elements
     */
    public List<PathElement> pathElements() {
        return Collections.unmodifiableList(pathElements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathElements, loose);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UnderlayAbstractPath) {
            UnderlayAbstractPath other = (UnderlayAbstractPath) obj;
            return Objects.equals(pathElements, other.pathElements) &&
                    Objects.equals(loose, other.loose);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("pathElements", pathElements)
                .add("loose", loose)
                .toString();
    }
}
