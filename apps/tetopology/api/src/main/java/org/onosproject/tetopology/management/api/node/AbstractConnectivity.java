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
package org.onosproject.tetopology.management.api.node;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.onosproject.tetopology.management.api.link.ElementType;
import org.onosproject.tetopology.management.api.link.TePathAttributes;
import org.onosproject.tetopology.management.api.link.UnderlayAbstractPath;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The abstraction of a TE node internal connectivity to link
 * termination points.
 */
public class AbstractConnectivity {
    // list of elements that can be constrained/connected to
    private final List<ElementType> constrainingElements;
    private final BitSet flags;
    private final TePathAttributes teAttributes;
    private final UnderlayAbstractPath underlayPath;

    /**
     * Creates an abstract connectivity instance.
     *
     * @param constrainingElements list of elements that can be constrained
     *                             or connected to
     * @param flags                indicate whether this connectivity is usable
     * @param teAttributes         the connectivity path TE attributes
     * @param underlayPath         the underlay path
     */
    public AbstractConnectivity(List<ElementType> constrainingElements,
                                BitSet flags,
                                TePathAttributes teAttributes,
                                UnderlayAbstractPath underlayPath) {
        this.constrainingElements = Lists.newArrayList(constrainingElements);
        this.flags = flags;
        this.teAttributes = teAttributes;
        this.underlayPath = underlayPath;
    }

    /**
     * Returns the "constraining elements" that can be constrained
     * or connected to "from" element.
     *
     * @return the "constraining elements" of the connectivity
     */
    public List<ElementType> constrainingElements() {
        return Collections.unmodifiableList(constrainingElements);
    }

    /**
     * Returns the flags indicating if the connectivity is usable.
     *
     * @return flags of the connectivity
     */
    public BitSet flags() {
        return flags;
    }

    /**
     * Returns the TE attributes of the connectivity.
     *
     * @return the TE attributes
     */
    public TePathAttributes teAttributes() {
        return teAttributes;
    }

    /**
     * Returns the underlay path.
     *
     * @return the underlay path
     */
    public UnderlayAbstractPath underlayPath() {
        return underlayPath;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(constrainingElements, flags,
                                teAttributes, underlayPath);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof AbstractConnectivity) {
            AbstractConnectivity that = (AbstractConnectivity) object;
            return Objects.equal(constrainingElements, that.constrainingElements) &&
                    Objects.equal(flags, that.flags) &&
                    Objects.equal(teAttributes, that.teAttributes) &&
                    Objects.equal(underlayPath, that.underlayPath);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("constrainingElements", constrainingElements)
                .add("flags", flags)
                .add("teAttributes", teAttributes)
                .add("underlayPath", underlayPath)
                .toString();
    }
}
