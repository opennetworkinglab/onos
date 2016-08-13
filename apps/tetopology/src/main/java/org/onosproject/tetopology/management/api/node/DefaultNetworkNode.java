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
 * NetworkNode implementation.
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
public class DefaultNetworkNode implements NetworkNode {
    private final KeyId id;
    private List<NetworkNodeKey> supportingNodeIds;
    private TeNode te;
    private List<TerminationPoint> tps;

    /**
     * Creates an instance of DefaultNetworkNode using Id.
     *
     * @param id network node identifier
     */
    public DefaultNetworkNode(KeyId id) {
        this.id = id;
    }

    /**
     * Creates an instance of DefaultNetworkNode.
     *
     * @param id network node identifier
     * @param nodeIds support node identifiers
     * @param te te parameter of the node
     */
    public DefaultNetworkNode(KeyId id, List<NetworkNodeKey> nodeIds, TeNode te) {
        this.id = id;
        this.supportingNodeIds = nodeIds;
        this.te = te;
    }

    /**
     * Sets the list of supporting node ids.
     *
     * @param ids the supporting node ids to set
     */
    public void setSupportingNodeIds(List<NetworkNodeKey> ids) {
        this.supportingNodeIds = ids;
    }

    /**
     * Sets the te attribute.
     *
     * @param te the te to set
     */
    public void setTe(TeNode te) {
        this.te = te;
    }

    /**
     * Sets the TerminationPoints.
     *
     * @param tps the tps to set
     */
    public void setTerminationPoints(List<TerminationPoint> tps) {
        this.tps = tps;
    }

    /**
     * Returns the node identifier.
     *
     * @return node identifier
     */
    @Override
    public KeyId nodeId() {
        return id;
    }

    /**
     * Returns the supportingNodeIds.
     *
     * @return list of supporting node identifiers for this node
     */
    @Override
    public List<NetworkNodeKey> getSupportingNodeIds() {
        return supportingNodeIds;
    }

    /**
     * Returns the te attribute value.
     *
     * @return TE attributes of this node
     */
    @Override
    public TeNode getTe() {
        return te;
    }

    /**
     * Returns the list of termination points.
     *
     * @return a list of termination points associated with this node
     */
    @Override
    public List<TerminationPoint> getTerminationPoints() {
        return tps;
    }

    /**
     * Returns the termination point.
     *
     * @return the termination point
     */
    @Override
    public TerminationPoint getTerminationPoint(KeyId tpId) {

        for (TerminationPoint tp : tps) {
           if (tp.id().equals(tpId)) {
               return tp;
           }
        }

        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, supportingNodeIds, te, tps);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultNetworkNode) {
            DefaultNetworkNode that = (DefaultNetworkNode) object;
            return Objects.equal(this.id, that.id) &&
                    Objects.equal(this.supportingNodeIds, that.supportingNodeIds) &&
                    Objects.equal(this.te, that.te) &&
                    Objects.equal(this.tps, that.tps);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("supportingNodeIds", supportingNodeIds)
                .add("te", te)
                .add("tps", tps)
                .toString();
    }

}
