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

import com.google.common.base.MoreObjects;

/**
 * Implementation of ODU Tributary Slot simply designated by an index number of slot.
 */
public class TributarySlot {

    private final long index;

    /**
     * Creates an instance representing the TributarySlot specified by the given index number.
     *
     * @param index index number of wavelength
     */
    public TributarySlot(long index) {
        this.index = index;
    }

    public static TributarySlot of(long index) {
        return new TributarySlot(index);
    }

    /**
     * Returns the index number of TributarySlot.
     *
     * @return the index number of TributarySlot
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
        if (!(obj instanceof TributarySlot)) {
            return false;
        }

        final TributarySlot that = (TributarySlot) obj;
        return this.index == that.index;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("index", index)
                .toString();
    }
}
