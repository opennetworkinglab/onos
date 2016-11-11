/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.tetopology.management.impl;

import java.util.List;

import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.node.TerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * The TerminationPoint representation in store.
 */
public class InternalTerminationPoint {
    private TeLinkTpGlobalKey teTpKey;
    private List<TerminationPointKey> supportingTpIds;

    /**
     * Creates an instance of InternalTerminationPoint.
     *
     * @param tp the termination point
     */
    public InternalTerminationPoint(TerminationPoint tp) {
        this.supportingTpIds = tp
                .supportingTpIds() == null ? null
                                           : Lists.newArrayList(tp
                                                   .supportingTpIds());
    }

    /**
     * Returns the TE termination point key.
     *
     * @return the teTpKey
    */
    public TeLinkTpGlobalKey teTpKey() {
        return teTpKey;
    }

    /**
     * Returns the supporting termination point Ids.
     *
     * @return the supportingTpIds
     */
    public List<TerminationPointKey> supportingTpIds() {
        return supportingTpIds == null ? null
                                       : ImmutableList.copyOf(supportingTpIds);
    }

    /**
     * Sets the TE termination point key.
     *
     * @param teTpKey the teTpKey to set
     */
    public void setTeTpKey(TeLinkTpGlobalKey teTpKey) {
        this.teTpKey = teTpKey;
    }

    /**
     * Sets the supporting termination point Ids.
     *
     * @param supportingTpIds the supportingTpIds to set
     */
    public void setSupportingTpIds(List<TerminationPointKey> supportingTpIds) {
        this.supportingTpIds = supportingTpIds == null ? null
                                                       : Lists.newArrayList(supportingTpIds);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(supportingTpIds, teTpKey);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof InternalTerminationPoint) {
            InternalTerminationPoint that = (InternalTerminationPoint) object;
            return Objects.equal(supportingTpIds, that.supportingTpIds)
                    && Objects.equal(teTpKey, that.teTpKey);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("supportingTpIds", supportingTpIds)
                .add("teTpKey", teTpKey)
                .toString();
    }

}
