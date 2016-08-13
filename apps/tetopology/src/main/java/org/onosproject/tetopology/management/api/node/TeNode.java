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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * TE Node representation.
 * <p>
 * The Set/Get methods below are defined to accept and pass references because
 * the object class is treated as a "composite" object class that holds
 * references to various member objects and their relationships, forming a
 * data tree. Internal routines of the TE topology manager may use the
 * following example methods to construct and manipulate any piece of data in
 * the data tree:
 * <pre>
 * newNode.getTe().setAdminStatus(), or
 * newNode.getSupportingNodeIds().add(nodeId), etc.
 * </pre>
 * Same for constructors where, for example, a child list may be constructed
 * first and passed in by reference to its parent object constructor.
 */
public class TeNode {
    // See org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.
    // topology.rev20160708.ietftetopology
    // .augmentednwnode.te.config.DefaultTeNodeAttributes for reference
    private String teNodeId;
    private String name;
    private TeStatus adminStatus;
    private TeStatus opStatus;
    private boolean isAbstract;
    private List<ConnectivityMatrix> connMatrices;
    private TeNetworkTopologyId underlayTopology;
    private List<TunnelTerminationPoint> tunnelTerminationPoints;

    /**
     * Creates an instance of TeNode.
     *
     * @param teNodeId TE node identifier
     */
    public TeNode(String teNodeId) {
        this.teNodeId = teNodeId;
    }

    /**
     * Sets the node name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the node administrative status.
     *
     * @param adminStatus the adminStatus to set
     */
    public void setAdminStatus(TeStatus adminStatus) {
        this.adminStatus = adminStatus;
    }

    /**
     * Sets the node operational status.
     *
     * @param opStatus the opStatus to set
     */
    public void setOpStatus(TeStatus opStatus) {
        this.opStatus = opStatus;
    }

    /**
     * Sets the node is an abstract node or not.
     *
     * @param isAbstract the isAbstract to set
     */
    public void setAbstract(boolean isAbstract) {
        this.isAbstract = isAbstract;
    }

    /**
     * Set connectivity matrix table.
     *
     * @param connMatrices connectivity matrix table
     */
    public void setConnectivityMatrices(List<ConnectivityMatrix> connMatrices) {
          this.connMatrices = connMatrices;
    }

    /**
     * Sets the node underlay TE topology.
     *
     * @param topo the underlayTopology to set
     */
    public void setUnderlayTopology(TeNetworkTopologyId topo) {
        this.underlayTopology = topo;
    }

    /**
     * Sets the list of tunnel termination points.
     *
     * @param ttps the tunnelTerminationPoints to set
     */
    public void setTunnelTerminationPoints(List<TunnelTerminationPoint> ttps) {
        this.tunnelTerminationPoints = ttps;
    }

    /**
     * Returns the teNodeId.
     *
     * @return TE node id
     */
    public String teNodeId() {
        return teNodeId;
    }

    /**
     * Returns the name.
     *
     * @return TE node name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the adminStatus.
     *
     * @return TE node admin status
     */
    public TeStatus adminStatus() {
        return adminStatus;
    }

    /**
     * Returns the opStatus.
     *
     * @return TE node operational status
     */
    public TeStatus opStatus() {
        return opStatus;
    }

    /**
     * Returns the isAbstract.
     *
     * @return true or false if the TE node is abstract
     */
    public boolean isAbstract() {
        return isAbstract;
    }

    /**
     * Returns the connectivity matrix table.
     *
     * @return node connectivity matrix table
     */
    public List<ConnectivityMatrix> connectivityMatrices() {
        return connMatrices;
    }

    /**
     * Returns the underlay topology.
     *
     * @return node underlay topology
     */
    public TeNetworkTopologyId underlayTopology() {
        return underlayTopology;
    }

    /**
     * Returns the tunnelTerminationPoints.
     *
     * @return list of tunnel terminational points
     */
    public List<TunnelTerminationPoint> tunnelTerminationPoints() {
        return tunnelTerminationPoints;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(teNodeId, name, adminStatus, opStatus, isAbstract,
                connMatrices, underlayTopology, tunnelTerminationPoints);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TeNode) {
            TeNode that = (TeNode) object;
            return Objects.equal(this.teNodeId, that.teNodeId) &&
                    Objects.equal(this.name, that.name) &&
                    Objects.equal(this.adminStatus, that.adminStatus) &&
                    Objects.equal(this.opStatus, that.opStatus) &&
                    Objects.equal(this.isAbstract, that.isAbstract) &&
                    Objects.equal(this.connMatrices, that.connMatrices) &&
                    Objects.equal(this.underlayTopology, that.underlayTopology) &&
                    Objects.equal(this.tunnelTerminationPoints, that.tunnelTerminationPoints);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("teNodeId", teNodeId)
                .add("name", name)
                .add("adminStatus", adminStatus)
                .add("opStatus", opStatus)
                .add("isAbstract", isAbstract)
                .add("connMatrices", connMatrices)
                .add("underlayTopology", underlayTopology)
                .add("tunnelTerminationPoints", tunnelTerminationPoints)
                .toString();
    }

}
