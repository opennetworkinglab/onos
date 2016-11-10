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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.tetopology.management.api.link.ElementType;
import org.onosproject.tetopology.management.api.link.TePathAttributes;
import org.onosproject.tetopology.management.api.link.UnderlayAbstractPath;

import java.util.BitSet;
import java.util.List;

/**
 * Represents node's switching limitations.
 */
public class ConnectivityMatrix extends AbstractConnectivity {
    /**
     * Indicates that switching is disallowed.
     */
    public static final short BIT_DISALLOWED = 0;

    /**
     * Indicates that an alternative switching connection path
     * is available.
     */
    public static final short BIT_ALTERNATIVE_PATH_AVAILABLE = 1;

    /**
     * Indicates that switching in this node is disabled.
     */
    public static final short BIT_DISABLED = 2;

    private final long key;
    private final ElementType from;
    // list of elements that can be merged with the "from" element
    private final List<ElementType> mergingList;

    /**
     * Creates a connectivity matrix instance.
     *
     * @param key                  the connectivity matrix key
     * @param from                 the "from" element (e.g. TE link id or
     *                             label) in the matrix
     * @param mergingList          the list of elements that can be merged
     *                             with the "from" element
     * @param constrainingElements the list of elements that can be constrained
     *                             or connected to the "from" element
     * @param flags                the indicator whether this connectivity
     *                             matrix is usable
     * @param teAttributes         the connectivity TE attributes of this matrix
     * @param underlayPath         the underlay path of the matrix
     */
    public ConnectivityMatrix(long key,
                              ElementType from,
                              List<ElementType> mergingList,
                              List<ElementType> constrainingElements,
                              BitSet flags,
                              TePathAttributes teAttributes,
                              UnderlayAbstractPath underlayPath) {
        super(constrainingElements, flags, teAttributes, underlayPath);
        this.key = key;
        this.from = from;
        this.mergingList = mergingList != null ?
                Lists.newArrayList(mergingList) : null;
    }

    /**
     * Returns the key.
     *
     * @return connectivity matrix key
     */
    public long key() {
        return key;
    }

    /**
     * Returns the "from" element of a connectivity matrix.
     *
     * @return the "from" of the connectivity matrix
     */
    public ElementType from() {
        return from;
    }

    /**
     * Returns the "merging list" can be merged with the "from" element.
     *
     * @return the "merging list" of the connectivity matrix
     */
    public List<ElementType> mergingList() {
        if (mergingList == null) {
            return null;
        }
        return ImmutableList.copyOf(mergingList);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, from, mergingList, super.hashCode());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof ConnectivityMatrix) {
            if (!super.equals(object)) {
                return false;
            }
            ConnectivityMatrix that = (ConnectivityMatrix) object;
            return Objects.equal(this.key, that.key) &&
                    Objects.equal(this.from, that.from) &&
                    Objects.equal(this.mergingList, that.mergingList);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("key", key)
                .add("from", from)
                .add("mergingList", mergingList)
                .add("constrainingElements", constrainingElements())
                .add("flags", flags())
                .add("teAttributes", teAttributes())
                .toString();
    }
}
