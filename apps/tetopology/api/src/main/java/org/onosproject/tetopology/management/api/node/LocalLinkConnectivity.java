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

import org.onosproject.tetopology.management.api.link.ElementType;
import org.onosproject.tetopology.management.api.link.TePathAttributes;
import org.onosproject.tetopology.management.api.link.UnderlayAbstractPath;

import java.util.BitSet;
import java.util.List;

/**
 * The connectivity between tunnel termination point and link termination
 * points.
 */
public class LocalLinkConnectivity extends AbstractConnectivity {
    /**
     * Indicates that the link connectivity is disabled.
     */
    public static final short BIT_DISABLED = 0;

    /**
     * Indicates that an alternative path of the link connection is
     * available.
     */
    public static final short BIT_ALTERNATIVE_PATH_AVAILABLE = 1;

    /**
     * Creates a local link connectivity instance.
     *
     * @param constrainingElements list of elements that can be constrained
     *                             or connected to
     * @param flags                indicate whether this connectivity is usable
     * @param teAttributes         the connectivity path TE attributes
     * @param underlayPath         the underlay path
     */
    public LocalLinkConnectivity(List<ElementType> constrainingElements, BitSet flags,
                                 TePathAttributes teAttributes,
                                 UnderlayAbstractPath underlayPath) {
        super(constrainingElements, flags, teAttributes, underlayPath);
    }

}
