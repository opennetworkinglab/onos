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
package org.onosproject.net.behaviour;

import java.util.Collection;

import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Behaviour for handling various drivers for tunnel configuration.
 */
public interface TunnelConfig extends HandlerBehaviour {

    /**
     * Create a tunnel.
     *
     * @param tunnel tunnel entity
     */
    void createTunnel(TunnelDescription tunnel);

    /**
     * Remove a tunnel.
     *
     * @param tunnel tunnel entity
     */
    void removeTunnel(TunnelDescription tunnel);

    /**
     * Update a tunnel.
     *
     * @param tunnel tunnel entity
     */
    void updateTunnel(TunnelDescription tunnel);

    /**
     * Gets tunnels.
     *
     * return collection of tunnel
     */
    Collection<TunnelDescription> getTunnels();

}
