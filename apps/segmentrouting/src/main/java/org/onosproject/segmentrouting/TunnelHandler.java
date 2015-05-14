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
package org.onosproject.segmentrouting;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Tunnel Handler.
 */
public class TunnelHandler {
    protected final Logger log = getLogger(getClass());

    private final HashMap<String, Tunnel> tunnelMap;

    public TunnelHandler() {
        tunnelMap = new HashMap<>();
    }

    /**
     * Creates a tunnel.
     *
     * @param tunnel tunnel reference to create a tunnel
     */
    public void createTunnel(Tunnel tunnel) {
        tunnel.create();
        tunnelMap.put(tunnel.id(), tunnel);
    }

    /**
     * Removes the tunnel with the tunnel ID given.
     *
     * @param tunnelInfo tunnel information to delete tunnels
     */
    public void removeTunnel(Tunnel tunnelInfo) {

        Tunnel tunnel = tunnelMap.get(tunnelInfo.id());
        if (tunnel != null) {
            tunnel.remove();
            tunnelMap.remove(tunnel.id());
        } else {
            log.warn("No tunnel found for tunnel ID {}", tunnelInfo.id());
        }
    }

    /**
     * Returns the tunnel with the tunnel ID given.
     *
     * @param tid Tunnel ID
     * @return Tunnel reference
     */
    public Tunnel getTunnel(String tid) {
        return tunnelMap.get(tid);
    }

    /**
     * Returns all tunnels.
     *
     * @return list of Tunnels
     */
    public List<Tunnel> getTunnels() {
        List<Tunnel> tunnels = new ArrayList<>();
        tunnelMap.values().forEach(tunnel -> tunnels.add(
                new DefaultTunnel((DefaultTunnel) tunnel)));

        return tunnels;
    }
}
