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

import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnelKey;

import java.util.List;

/**
 * Service for administering the TE Tunnel attributes.
 * <p>
 * Please note that this service works with the existing Tunnel subsystem
 * together, just as an extension to the tunnel subsystem, and only focuses
 * on TE Tunnel attributes management.
 */
public interface TeTunnelAdminService extends TeTunnelService {

    /**
     * Creates a TE Tunnel with the supplied attributes, and returns an
     * identifier for the tunnel on success, or null on failure.
     *
     * @param teTunnel TE Tunnel attributes
     * @return created tunnel identifier or null if failed
     */
    TunnelId createTeTunnel(TeTunnel teTunnel);

    /**
     * Sets the corresponding Tunnel identifier of the TE Tunnel specified
     * by the given key.
     *
     * @param teTunnelKey TE Tunnel key
     * @param tunnelId corresponding tunnel identifier
     */
    void setTunnelId(TeTunnelKey teTunnelKey, TunnelId tunnelId);

    /**
     * Updates TE Tunnel attributes with supplied information, the old
     * attributes will be totally overwrote by the new attributes.
     *
     * @param teTunnel new TE Tunnel attributes
     */
    void updateTeTunnel(TeTunnel teTunnel);

    /**
     * Updates state of a TE tunnel specified by the given key.
     *
     * @param key TE tunnel key
     * @param state new state of the tunnel
     */
    void updateTunnelState(TeTunnelKey key, Tunnel.State state);

    /**
     * Removes a TE Tunnel specified by the given key.
     *
     * @param teTunnelKey TE Tunnel key
     */
    void removeTeTunnel(TeTunnelKey teTunnelKey);

    /**
     * Sets segment tunnels of a E2E cross-domain tunnel.
     *
     * @param e2eTunnelKey key of the E2E tunnel
     * @param segmentTunnels list of segment tunnels
     */
    void setSegmentTunnel(TeTunnelKey e2eTunnelKey,
                          List<TeTunnelKey> segmentTunnels);

    //TODO: add interfaces for teGlobal and teLspState
}
