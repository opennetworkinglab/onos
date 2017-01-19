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
 * Service for TE Tunnel attributes management.
 * <p>
 * Please note that this service works with the existing Tunnel subsystem
 * together, just as an extension to the tunnel subsystem, and only focus on TE
 * Tunnel attributes management.
 */
public interface TeTunnelService {

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
     * Returns the corresponding tunnel identifier of a TE tunnel with the
     * specified key.
     *
     * @param teTunnelKey TE Tunnel key
     * @return corresponding tunnel identifier or null if one with the given
     * key is not known
     */
    TunnelId getTunnelId(TeTunnelKey teTunnelKey);

    /**
     * Returns a collection of currently known TE tunnels.
     *
     * @return collection of TE tunnels
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
     * Returns the TE LSP with the specified key.
     *
     * @param key TE LSP key
     * @return TeLsp or null if one with the given key is not known
     */
    TeLsp getTeLsp(TeLspKey key);

    /**
     * Returns a collection of currently known TE LSPs.
     *
     * @return collection of TeLsp
     */
    Collection<TeLsp> getTeLsps();

    //TODO: add interfaces for teGlobal and teLspState
}
