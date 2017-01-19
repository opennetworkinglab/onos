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

package org.onosproject.tetunnel.api;

import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetunnel.api.lsp.TeLsp;
import org.onosproject.tetunnel.api.lsp.TeLspKey;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnelKey;

import java.util.Collection;

/**
 * Manages TE tunnel attributes.
 * <p>
 * Please note that this service works with the existing Tunnel subsystem
 * together, just as an extension to the tunnel subsystem, and only focus on TE
 * Tunnel attributes management.
 */
public interface TeTunnelStore {

    /**
     * Creates a TE Tunnel with the supplied attributes, and returns true on
     * success, or false on failure.
     *
     * @param teTunnel TE Tunnel attributes
     * @return true on success, or false on failure
     */
    boolean addTeTunnel(TeTunnel teTunnel);

    /**
     * Sets the corresponding Tunnel identifier of the TE Tunnel specified
     * by the given key.
     *
     * @param teTunnelKey TE Tunnel key
     * @param tunnelId corresponding tunnel identifier
     */
    void setTunnelId(TeTunnelKey teTunnelKey, TunnelId tunnelId);

    /**
     * Returns the corresponding Tunnel identifier of the TE tunnel.
     *
     * @param teTunnelKey TE Tunnel key
     * @return corresponding Tunnel identifier
     */
    TunnelId getTunnelId(TeTunnelKey teTunnelKey);

    /**
     * Updates TE Tunnel attributes with supplied information, the old
     * attributes will be totally overwrote by the new attributes.
     *
     * @param teTunnel new TE Tunnel attributes
     */
    void updateTeTunnel(TeTunnel teTunnel);

    /**
     * Removes a TE Tunnel specified by the given key.
     *
     * @param teTunnelKey TE Tunnel key
     */
    void removeTeTunnel(TeTunnelKey teTunnelKey);

    /**
     * Returns the TE Tunnel with the specified key.
     *
     * @param teTunnelKey TE Tunnel key
     * @return TeTunnel or null if one with the given key is not known
     */
    TeTunnel getTeTunnel(TeTunnelKey teTunnelKey);

    /**
     * Returns the TE Tunnel with the specified identifier.
     *
     * @param tunnelId corresponding tunnel identifier
     * @return TeTunnel or null if one with the given identifier is not known
     */
    TeTunnel getTeTunnel(TunnelId tunnelId);


    /**
     * Returns a collection of currently known TE Tunnels.
     *
     * @return collection of TeTunnels
     */
    Collection<TeTunnel> getTeTunnels();

    /**
     * Returns a collection of currently known TE Tunnels filtered by the
     * specified TE tunnel type.
     *
     * @param type TE tunnel type to filter by
     * @return filtered collection of TE tunnels
     */
    Collection<TeTunnel> getTeTunnels(TeTunnel.Type type);

    /**
     * Returns a collection of currently known TE tunnels filtered by specified
     * TE topology key.
     *
     * @param teTopologyKey TE topology key to filter by
     * @return filtered collection of TE tunnels
     */
    Collection<TeTunnel> getTeTunnels(TeTopologyKey teTopologyKey);

    /**
     * Adds a TE LSP.
     *
     * @param lsp TE LSP attributes
     * @return true when success
     */
    boolean addTeLsp(TeLsp lsp);

    /**
     * Updates TE LSP attributes.
     *
     * @param lsp new TE LSP attributes
     */
    void updateTeLsp(TeLsp lsp);

    /**
     * Removes a TE LSP.
     *
     * @param key TE LSP key
     */
    void removeTeLsp(TeLspKey key);

    /**
     * Returns the TE LSP with the specified key.
     *
     * @param key TE LSP key
     * @return TeLsp or null if one with the given key is not known
     */
    TeLsp getTeLsp(TeLspKey key);

    /**
     * Returns a collection of currently known TE LSP.
     *
     * @return collection of TeLsp
     */
    Collection<TeLsp> getTeLsps();
}
