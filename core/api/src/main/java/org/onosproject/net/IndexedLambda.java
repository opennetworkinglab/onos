/*
 * Copyright 2015 Open Networking Laboratory
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

import com.google.common.base.MoreObjects;

/**
 * Implementation of Lambda simply designated by an index number of wavelength.
 *
 * @deprecated in Emu (ONOS 1.4).
 */
@Deprecated
public class IndexedLambda implements Lambda {

    private final long index;

    /**
     * Creates an instance representing the wavelength specified by the given index number.
     * It is recommended to use {@link Lambda#indexedLambda(long)} unless you want to use the
     * concrete type, IndexedLambda, directly.
     *
     * @param index index number of wavelength
     */
    public IndexedLambda(long index) {
        this.index = index;
    }

    /**
     * Returns the index number of lambda.
     *
     * @return the index number of lambda
     */
    public long index() {
        return index;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(index);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IndexedLambda)) {
            return false;
        }

        final IndexedLambda that = (IndexedLambda) obj;
        return this.index == that.index;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("lambda", index)
                .toString();
    }
}
