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
package org.onosproject.tetopology.management.api;

import java.util.List;

import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.node.NetworkNode;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Default Network implementation.
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
public class DefaultNetwork implements Network {
    private KeyId networkId;
    private List<KeyId> supportingNetworkIds;
    private List<NetworkNode> nodes;
    private List<NetworkLink> links;
    private TeTopologyId teTopologyId;
    private boolean serverProvided;

    /**
     * Constructor with all fields.
     *
     * @param networkId network identifier
     * @param supportingNetworkIds supporting network identifier
     * @param nodes list of nodes within the network
     * @param links list of links within the network
     * @param teTopologyId TE topology identifier
     * @param serverProvided whether the network is received from server
     */
    public DefaultNetwork(KeyId networkId, List<KeyId> supportingNetworkIds,
            List<NetworkNode> nodes, List<NetworkLink> links, TeTopologyId teTopologyId,
            boolean serverProvided) {
        this.networkId = networkId;
        this.supportingNetworkIds = supportingNetworkIds;
        this.nodes = nodes;
        this.links = links;
        this.teTopologyId = teTopologyId;
        this.serverProvided = serverProvided;
    }

    /**
     * Constructor with key only.
     *
     * @param networkId network identifier
     */
    public DefaultNetwork(KeyId networkId) {
        this.networkId = networkId;
    }

    /**
     * Creates an instance of DefaultNetwork from an existing Network object.
     *
     * @param network network identifier
     */
    public DefaultNetwork(Network network) {
        this.networkId = network.networkId();
        this.supportingNetworkIds = network.getSupportingNetworkIds();
        this.nodes = network.getNodes();
        this.links = network.getLinks();
        this.teTopologyId =  network.getTeTopologyId();
        this.serverProvided =  network.isServerProvided();
    }

    @Override
    public KeyId networkId() {
        return networkId;
    }

    @Override
    public List<KeyId> getSupportingNetworkIds() {
        return supportingNetworkIds;
    }

    @Override
    public List<NetworkNode> getNodes() {
        return nodes;
    }

    @Override
    public NetworkNode getNode(KeyId nodeId) {

        for (NetworkNode node : nodes) {
           if (node.nodeId().equals(nodeId)) {
               return node;
           }
        }
        return null;
    }

    @Override
    public List<NetworkLink> getLinks() {
        return links;
    }

    @Override
    public NetworkLink getLink(KeyId linkId) {

       for (NetworkLink link : links) {
           if (link.linkId().equals(linkId)) {
               return link;
           }
       }
       return null;
    }

    @Override
    public boolean isServerProvided() {
        return serverProvided;
    }

    @Override
    public TeTopologyId getTeTopologyId() {
        return teTopologyId;
    }


    /**
     * Sets the supporting network keys.
     *
     * @param supportingNetworkIds the supportingNetworkIds to set
     */
    public void setSupportingNetworkIds(List<KeyId> supportingNetworkIds) {
        this.supportingNetworkIds = supportingNetworkIds;
    }

    /**
     * Sets the list of nodes .
     *
     * @param nodes the nodes to set
     */
    public void setNodes(List<NetworkNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * Sets the links.
     *
     * @param links the links to set
     */
    public void setLinks(List<NetworkLink> links) {
        this.links = links;
    }

    /**
     * Sets the attribute serverProvided.
     *
     * @param serverProvided the attribute to set
     */
    public void setServerProvided(boolean serverProvided) {
        this.serverProvided = serverProvided;
    }

    /**
     * Sets the TE Topology Id.
     *
     * @param teTopologyId the teTopologyId to set
     */
    public void setTeTopologyId(TeTopologyId teTopologyId) {
        this.teTopologyId = teTopologyId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkId, supportingNetworkIds,
                nodes, links, serverProvided, teTopologyId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultNetwork) {
            DefaultNetwork that = (DefaultNetwork) object;
            return Objects.equal(this.networkId, that.networkId) &&
                    Objects.equal(this.supportingNetworkIds, that.supportingNetworkIds) &&
                    Objects.equal(this.nodes, that.nodes) &&
                    Objects.equal(this.links, that.links) &&
                    Objects.equal(this.serverProvided, that.serverProvided) &&
                    Objects.equal(this.teTopologyId, that.teTopologyId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networkId", networkId)
                .add("supportingNetworkIds", supportingNetworkIds)
                .add("nodes", nodes)
                .add("links", links)
                .add("serverProvided", serverProvided)
                .add("teTopologyId", teTopologyId)
                .toString();
    }

}
