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
import org.onosproject.tetopology.management.api.KeyId;

import java.util.List;

/**
 * The default implementation of TE termination point.
 */
public class DefaultTerminationPoint implements TerminationPoint {
    private final KeyId tpId;
    private final List<TerminationPointKey> supportingTpIds;
    private final Long teTpId;

    /**
     * Creates a termination point.
     *
     * @param tpId   termination point identifier
     * @param tps    support termination point identifier
     * @param teTpId TE termination point identifier
     */
    public DefaultTerminationPoint(KeyId tpId,
                                   List<TerminationPointKey> tps,
                                   Long teTpId) {
        this.tpId = tpId;
        this.supportingTpIds = tps != null ? Lists.newArrayList(tps) : null;
        this.teTpId = teTpId;
    }

    @Override
    public KeyId tpId() {
        return tpId;
    }

    @Override
    public Long teTpId() {
        return teTpId;
    }

    @Override
    public List<TerminationPointKey> supportingTpIds() {
        if (supportingTpIds == null) {
            return null;
        }
        return ImmutableList.copyOf(supportingTpIds);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tpId, supportingTpIds, teTpId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultTerminationPoint) {
            DefaultTerminationPoint that = (DefaultTerminationPoint) object;
            return Objects.equal(tpId, that.tpId) &&
                    Objects.equal(supportingTpIds, that.supportingTpIds) &&
                    Objects.equal(teTpId, that.teTpId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tpId", tpId)
                .add("supportingTpIds", supportingTpIds)
                .add("teTpId", teTpId)
                .toString();
    }


}
