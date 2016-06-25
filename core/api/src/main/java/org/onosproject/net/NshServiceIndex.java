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
package org.onosproject.net;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/*
 * Representation of NSH Service index
 */
public final class NshServiceIndex {
    private static final short MASK = 0xFF;
    private final short serviceIndex;

    /**
     * Default constructor.
     *
     * @param serviceIndex nsh service index
     */
    private NshServiceIndex(short serviceIndex) {
        this.serviceIndex = (short) (serviceIndex & MASK);
    }

    /**
     * Returns the NshServiceIndex by setting its value.
     *
     * @param serviceIndex nsh service index
     * @return NshServiceIndex
     */
    public static NshServiceIndex of(short serviceIndex) {
        return new NshServiceIndex(serviceIndex);
    }


    /**
     * Returns nsh service index value.
     *
     * @return the nsh service index
     */
    public short serviceIndex() {
        return serviceIndex;
    }


    @Override
    public int hashCode() {
        return Objects.hash(serviceIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NshServiceIndex)) {
            return false;
        }
        final NshServiceIndex other = (NshServiceIndex) obj;
        return   Objects.equals(this.serviceIndex, other.serviceIndex);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("serviceIndex", serviceIndex)
                .toString();
    }
}

