/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.tunnel;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.Store;

/**
 * Manages inventory of tunnels.
 */
public interface TunnelStore extends Store<TunnelEvent, TunnelStoreDelegate> {

    /**
     * Returns the number of tunnels in the store.
     *
     * @return number of tunnels
     */
    int getTunnelCount();

    /**
     * Returns an iterable collection of all tunnel in the inventory.
     *
     * @return collection of all tunnels
     */
    Iterable<Tunnel> getTunnels();

    /**
     * Returns all tunnels egressing from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device tunnels
     */
    Iterable<Tunnel> getDeviceEgressTunnels(DeviceId deviceId);

    /**
     * Returns all tunnels ingressing from the specified device.
     *
     * @param deviceId device identifier
     * @return set of device tunnels
     */
    Iterable<Tunnel> getDeviceIngressTunnels(DeviceId deviceId);

    /**
     * Returns the tunnel between the two end-points and the tunnel type.
     *
     * @param src source connection point
     * @param dst destination connection point
     * @param type tunnel type
     * @return tunnels or null if one not found between the end-points
     */
    Iterable<Tunnel> getTunnel(ConnectPoint src, ConnectPoint dst, Tunnel.Type type);

    /**
     * Returns all tunnels egressing from the specified connection point.
     *
     * @param src source connection point
     * @return set of connection point tunnels
     */
    Iterable<Tunnel> getEgressTunnels(ConnectPoint src);

    /**
     * Returns all tunnels ingressing to the specified connection point.
     *
     * @param dst destination connection point
     * @return set of connection point tunnels
     */
    Iterable<Tunnel> getIngressTunnels(ConnectPoint dst);

    /**
     * Creates a new tunnel based on the given information.
     *
     * @param providerId    provider identity (e.g., PCEP provider)
     * @param tunnel tunnel information
     * @return create tunnel event
     */
    TunnelEvent addTunnel(ProviderId providerId,
                                        Tunnel tunnel);

    /**
     * Updates a new tunnel based on the given information.
     *
     * @param providerId      provider identity (e.g., PCEP provider)
     * @param tunnel tunnel
     * @return update tunnel event
     */
    TunnelEvent updateTunnel(ProviderId providerId,
                                        Tunnel tunnel);

    /**
     * Removes a new tunnel based on the given information.
     *
     * @param providerId      provider identity (e.g., PCEP provider)
     * @param tunnel tunnel
     * @return remove tunnel event
     */
    TunnelEvent removeTunnel(ProviderId providerId,
                             Tunnel tunnel);

}
