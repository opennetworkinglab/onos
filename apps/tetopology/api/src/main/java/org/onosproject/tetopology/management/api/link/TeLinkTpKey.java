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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of the TE link TP (i.e., TE termination point) Key in
 * the scope of a TE node.
 */
public final class TeLinkTpKey {
    private final long teNodeId;
    private final long teLinkTpId;

    /**
     * Creates a TE link TP key.
     *
     * @param teNodeId   TE Node identifier
     * @param teLinkTpId TE Link termination point identifier
     */
    public TeLinkTpKey(long teNodeId, long teLinkTpId) {
        this.teNodeId = teNodeId;
        this.teLinkTpId = teLinkTpId;
    }

    /**
     * Returns the TE Node identifier.
     *
     * @return the TE node id
     */
    public long teNodeId() {
        return teNodeId;
    }

    /**
     * Returns the TE link termination point identifier.
     *
     * @return the TE link TP id
     */
    public long teLinkTpId() {
        return teLinkTpId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(teNodeId, teLinkTpId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeLinkTpKey) {
            TeLinkTpKey that = (TeLinkTpKey) object;
            return Objects.equal(teNodeId, that.teNodeId) &&
                    Objects.equal(teLinkTpId, that.teLinkTpId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("teNodeId", teNodeId)
                .add("teLinkTpId", teLinkTpId)
                .toString();
    }
}
