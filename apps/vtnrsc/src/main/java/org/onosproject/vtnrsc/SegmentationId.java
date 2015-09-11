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
package org.onosproject.vtnrsc;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a Segmentation identifier.
 */
public final class SegmentationId {

    private final String segmentationId;

    // Public construction is prohibited
    private SegmentationId(String segmentationId) {
        checkNotNull(segmentationId, "SegmentationId cannot be null");
        this.segmentationId = segmentationId;
    }

    /**
     * Creates a  SegmentationId object.
     *
     * @param segmentationId segmentation identifier
     * @return SegmentationId
     */
    public static SegmentationId segmentationId(String segmentationId) {
        return new SegmentationId(segmentationId);
    }

    /**
     * Returns the segmentation identifier.
     *
     * @return segmentationId
     */
    public String segmentationId() {
        return segmentationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(segmentationId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SegmentationId) {
            final SegmentationId that = (SegmentationId) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.segmentationId, that.segmentationId);
        }
        return false;
    }

    @Override
    public String toString() {
        return segmentationId;
    }

}
