/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.server.devices.cpu;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_LEVEL_NULL;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_TYPE_NULL;

/**
 * Represents a CPU cache ID.
 * This class is immutable.
 */
public final class CpuCacheId {

    private final CpuCacheLevel level;
    private final CpuCacheType type;

    /**
     * Constructor from an integer value.
     *
     * @param value the value to use
     */
    private CpuCacheId(CpuCacheLevel level, CpuCacheType type) {
        checkNotNull(level, MSG_CPU_CACHE_LEVEL_NULL);
        checkNotNull(type, MSG_CPU_CACHE_TYPE_NULL);
        this.level = level;
        this.type = type;
    }

    /**
     * Creates a builder for CpuCacheId object.
     *
     * @return builder object for CpuCacheId object
     */
    public static CpuCacheId.Builder builder() {
        return new Builder();
    }

    /**
     * Get the level of the CPU cache.
     *
     * @return CPU cache level
     */
    public CpuCacheLevel level() {
        return level;
    }

    /**
     * Get the type of the CPU cache.
     *
     * @return CPU cache type
     */
    public CpuCacheType type() {
        return type;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("level", level())
                .add("type", type())
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CpuCacheId)) {
            return false;
        }
        CpuCacheId cacheId = (CpuCacheId) obj;
        return  this.level() ==  cacheId.level() &&
                this.type() == cacheId.type();
    }

    public static final class Builder {

        CpuCacheLevel level;
        CpuCacheType type;

        private Builder() {
        }

        /**
         * Sets CPU cache level.
         *
         * @param levelStr CPU cache level as a string
         * @return builder object
         */
        public Builder setLevel(String levelStr) {
            if (!Strings.isNullOrEmpty(levelStr)) {
                this.level = CpuCacheLevel.getByName(levelStr);
            }

            return this;
        }

        /**
         * Sets CPU cache type.
         *
         * @param typeStr CPU cache type as a string
         * @return builder object
         */
        public Builder setType(String typeStr) {
            if (!Strings.isNullOrEmpty(typeStr)) {
                this.type = CpuCacheType.getByName(typeStr);
            }

            return this;
        }

        /**
         * Creates a CpuCacheId object.
         *
         * @return CpuCacheId object
         */
        public CpuCacheId build() {
            return new CpuCacheId(level, type);
        }

    }

}
