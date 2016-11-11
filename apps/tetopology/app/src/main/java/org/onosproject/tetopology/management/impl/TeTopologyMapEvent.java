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

import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.TeTopologyEvent.Type;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNodeKey;

import com.google.common.base.MoreObjects;

public class TeTopologyMapEvent {
    private final Type type;
    private TeTopologyKey teTopologyKey;
    private TeNodeKey teNodeKey;
    private TeLinkTpGlobalKey teLinkKey;
    private KeyId networkKey;
    private NetworkNodeKey networkNodeKey;
    private NetworkLinkKey networkLinkKey;

    /**
     * Creates an instance of TeTopologyMapEvent.
     *
     * @param type the map event type
     */
    public TeTopologyMapEvent(Type type) {
        this.type = type;
    }

    /**
     * Returns the map event type.
     *
     * @return the type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the TE topology key of the event.
     *
     * @return the teTopologyKey
     */
    public TeTopologyKey teTopologyKey() {
        return teTopologyKey;
    }

    /**
     * Sets the TE topology key of the event.
     *
     * @param teTopologyKey the teTopologyKey to set
     */
    public void setTeTopologyKey(TeTopologyKey teTopologyKey) {
        this.teTopologyKey = teTopologyKey;
    }

    /**
     * Returns the TE node key of the event.
     *
     * @return the teNodeKey
     */
    public TeNodeKey teNodeKey() {
        return teNodeKey;
    }

    /**
     * Sets the TE node key of the event.
     *
     * @param teNodeKey the teNodeKey to set
     */
    public void setTeNodeKey(TeNodeKey teNodeKey) {
        this.teNodeKey = teNodeKey;
    }

    /**
     * Returns the TE link key of the event.
     *
     * @return the teLinkKey
     */
    public TeLinkTpGlobalKey teLinkKey() {
        return teLinkKey;
    }

    /**
     * Sets the TE link key of the event.
     *
     * @param teLinkKey the teLinkKey to set
     */
    public void setTeLinkKey(TeLinkTpGlobalKey teLinkKey) {
        this.teLinkKey = teLinkKey;
    }

    /**
     * Returns the network key of the event.
     *
     * @return the networkKey
     */
    public KeyId networkKey() {
        return networkKey;
    }

    /**
     * Sets the network key of the event.
     *
     * @param networkKey the networkKey to set
     */
    public void setNetworkKey(KeyId networkKey) {
        this.networkKey = networkKey;
    }

    /**
     * Returns the network node key of the event.
     *
     * @return the networkNodeKey
     */
    public NetworkNodeKey networkNodeKey() {
        return networkNodeKey;
    }

    /**
     * Sets the network node key of the event.
     *
     * @param networkNodeKey the networkNodeKey to set
     */
    public void setNetworkNodeKey(NetworkNodeKey networkNodeKey) {
        this.networkNodeKey = networkNodeKey;
    }

    /**
     * Returns the network link key of the event.
     *
     * @return the networkLinkKey
     */
    public NetworkLinkKey networkLinkKey() {
        return networkLinkKey;
    }

    /**
     * Sets the network link key of the event.
     *
     * @param networkLinkKey the networkLinkKey to set
     */
    public void setNetworkLinkKey(NetworkLinkKey networkLinkKey) {
        this.networkLinkKey = networkLinkKey;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", type)
                .add("teTopologyKey", teTopologyKey)
                .add("teNodeKey", teNodeKey)
                .add("teLinkKey", teLinkKey)
                .add("networkKey", networkKey)
                .add("networkNodeKey", networkNodeKey)
                .add("networkLinkKey", networkLinkKey)
                .toString();
    }

}
