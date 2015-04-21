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

import java.util.Collection;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Path;
import org.onosproject.net.resource.Bandwidth;

/**
 * Service for interacting with the tunnel inventory.
 */
public interface TunnelService {

    /**
     * Invokes the core to create a tunnel based on specified parameters.
     *
     * @param src sourcePoint
     * @param dst destinationPoint
     * @param bw  bandwidth
     * @param path explicit path or null
     */
    void requestTunnel(ConnectPoint src, ConnectPoint dst, Bandwidth bw, Path path);

    /**
     * Invokes the core to create a tunnel based on specified parameters with a tunnel type.
     *
     * @param src sourcePoint
     * @param dst destinationPoint
     * @param type  tunnelType
     * @param bw  bandwidth
     * @param path explicit path or null
     */
    void requestTunnel(ConnectPoint src, ConnectPoint dst, Tunnel.Type type, Bandwidth bw, Path path);

    /**
     * Returns the count of all known tunnels in the dataStore.
     *
     * @return number of tunnels
     */
    int getTunnelCount();

    /**
     * Returns a collection of all known tunnel based on the type.
     *
     *@param type  tunnelType
     * @return all tunnels for a specific type
     */
    Collection<Tunnel> getTunnels(Tunnel.Type type);

    /**
     * Returns set of all tunnels from the specified connectpoint.
     *
     * @param connectPoint device/portnumber
     * @param type  tunnelType
     * @return set of tunnels
     */
    Collection<Tunnel> getTunnels(ConnectPoint connectPoint, Tunnel.Type type);

    /**
     * Returns set of all tunnels from the
     * specified source connectpoint and destination connectpoint.
     *
     * @param src sourcePoint
     * @param dst destinationPoint
     * @param type tunnel type
     * @return set of tunnels
     */
    Collection<Tunnel> getTunnels(ConnectPoint src, ConnectPoint dst, Tunnel.Type type);

    /**
     * Returns the tunnel between the specified source
     * and destination connection points.
     *
     * @param src source label
     * @param dst destination label
     * @return tunnel from source to destination; null if none found
     */
    Tunnel getTunnel(Label src, Label dst);

    /**
     * Returns the tunnel based on the Id.
     *
     * @param id tunnelId
     * @return tunnel with specified Id
     */
    Tunnel getTunnel(TunnelId id);

    /**
     * Adds the specified tunnel listener.
     *
     * @param listener tunnel listener
     */
    void addListener(TunnelListener listener);

    /**
     * Removes the specified tunnel listener.
     *
     * @param listener tunnel listener
     */
    void removeListener(TunnelListener listener);

}
