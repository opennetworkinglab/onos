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
package org.onosproject.net.flow.criteria;

import java.util.Objects;

/**
 * Implementation of Metadata criterion.
 */
public final class MetadataCriterion implements Criterion {
    private final long metadata;

    /**
     * Constructor.
     *
     * @param metadata the metadata to match (64 bits data)
     */
    MetadataCriterion(long metadata) {
        this.metadata = metadata;
    }

    @Override
    public Type type() {
        return Type.METADATA;
    }

    /**
     * Gets the metadata to match.
     *
     * @return the metadata to match (64 bits data)
     */
    public long metadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + Long.toHexString(metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), metadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MetadataCriterion) {
            MetadataCriterion that = (MetadataCriterion) obj;
            return Objects.equals(metadata, that.metadata) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
