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

import java.util.List;

import org.onosproject.tetopology.management.api.KeyId;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Represent a termination point.
 * <p>
 * The Set/Get methods below are defined to accept and pass references because
 * the object class is treated as a "composite" object class that holds
 * references to various member objects and their relationships, forming a
 * data tree. Internal routines of the TE topology manager may use the
 * following example methods to construct and manipulate any piece of data in
 * the data tree:
 *<pre>
 * newNode.getTe().setAdminStatus(), or
 * newNode.getSupportingNodeIds().add(nodeId), etc.
 *</pre>
 * Same for constructors where, for example, a child list may be constructed
 * first and passed in by reference to its parent object constructor.
 */
public class DefaultTerminationPoint implements TerminationPoint {
    private KeyId id;
    private List<TerminationPointKey> supportingTpIds;
    private TeTerminationPoint te;

    /**
     * Creates an instance of DefaultTerminationPoint.
     *
     * @param id termination point identifier
     * @param tps support termination point identifier
     * @param te te parameters of the terminiation point
     */
    public DefaultTerminationPoint(KeyId id, List<TerminationPointKey> tps,
                                              TeTerminationPoint te) {
        this.id = id;
        this.supportingTpIds = tps;
        this.te = te;
    }

    /**
     * Creates an instance of DefaultTerminationPoint with teTpId only.
     *
     * @param id termination point identifier
     */
    public DefaultTerminationPoint(KeyId id) {
        this.id = id;
    }

    @Override
    public KeyId id() {
        return id;
    }

    @Override
    public List<TerminationPointKey> getSupportingTpIds() {
        return supportingTpIds;
    }

    @Override
    public TeTerminationPoint getTe() {
        return te;
    }

    /**
     * Sets the Id.
     *
     * @param id the id to set
     */
    public void setId(KeyId id) {
        this.id = id;
    }

    /**
     * Sets the supportingTpIds.
     *
     * @param tps the supportingTpIds to set
     */
    public void setSupportingTpIds(List<TerminationPointKey> tps) {
        this.supportingTpIds = tps;
    }

    /**
     * Sets the te extension.
     *
     * @param te the te to set
     */
    public void setTe(TeTerminationPoint te) {
        this.te = te;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, supportingTpIds, te);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultTerminationPoint) {
            DefaultTerminationPoint that = (DefaultTerminationPoint) object;
            return Objects.equal(this.id, that.id) &&
                    Objects.equal(this.supportingTpIds, that.supportingTpIds) &&
                    Objects.equal(this.te, that.te);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("supportingTpIds", supportingTpIds)
                .add("te", te)
                .toString();
    }

}
