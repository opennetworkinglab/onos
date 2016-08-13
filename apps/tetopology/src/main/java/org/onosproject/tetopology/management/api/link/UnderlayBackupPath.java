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

import java.util.Objects;
import com.google.common.base.MoreObjects;

/**
 * Represents a list of backup service paths on the underlay topology that
 * protect the underlay primary path.
 */
public class UnderlayBackupPath extends UnderlayAbstractPath {
    private long index;

    /**
     * Sets the index.
     *
     * @param index the index to set
     */
    public void setIndex(long index) {
        this.index = index;
    }

    /**
     * Returns the index.
     *
     * @return path index
     */
    public long index() {
        return index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), index);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UnderlayBackupPath) {
            if (!super.equals(obj)) {
                return false;
            }
            UnderlayBackupPath that = (UnderlayBackupPath) obj;
            return this.index == that.index;
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("index", index)
            .add("ref", ref())
            .add("pathElements", pathElements())
            .toString();
    }
}
