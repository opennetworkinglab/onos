/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.tunnel;

import com.google.common.annotations.Beta;
import org.onosproject.net.Path;
import org.onosproject.net.provider.ProviderId;

/**
 * Service for administering the inventory of provisioned tunnels.
 */
@Beta
public interface TunnelAdminService {

    /**
     * Removes the provisioned tunnel.
     *
     * @param tunnelId tunnel ID
     */
    void removeTunnel(TunnelId tunnelId);

    /**
     * Removes the provisioned tunnel leading to and from the
     * specified labels.
     *
     * @param src source label
     * @param dst destination label
     * @param producerName producer name
     */
    void removeTunnels(TunnelEndPoint src, TunnelEndPoint dst, ProviderId producerName);

    /**
     * Removes all provisioned tunnels leading to and from the
     * specified connection point.
     *
     * @param src source connection point
     * @param dst destination connection point
     * @param type tunnel type
     * @param producerName producer name
     */
    void removeTunnels(TunnelEndPoint src, TunnelEndPoint dst, Tunnel.Type type, ProviderId producerName);

    /**
     * Invokes the core to update a tunnel based on specified tunnel parameters.
     *
     * @param tunnel Tunnel
     * @param path explicit route (path changed) or null (path not changed) for the tunnel
     */
    void updateTunnel(Tunnel tunnel, Path path);

    /**
     * Updates the state of a tunnel.
     *
     * @param tunnel tunnel to be changed
     * @param state new state of the tunnel
     */
    void updateTunnelState(Tunnel tunnel, Tunnel.State state);
}
