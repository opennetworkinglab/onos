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

import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.net.provider.Provider;

/**
 * Abstraction of an entity providing tunnel setup/release services to the core.
 */
public interface TunnelProvider extends Provider {

    /**
     * Instructs the provider to setup a tunnel.
     *
     * @param tunnel Tunnel
     * @param path explicit route or null for the tunnel
     */
    void setupTunnel(Tunnel tunnel, Path path);

    /**
     * Instructs the provider to setup a tunnel given the respective device.
     *
     * @param srcElement device
     * @param tunnel Tunnel
     * @param path explicit route (not null) for the tunnel
     */
    void setupTunnel(ElementId srcElement, Tunnel tunnel, Path path);

    /**
     * Instructs the provider to release a tunnel.
     *
     * @param tunnel Tunnel
     */
    void releaseTunnel(Tunnel tunnel);

    /**
     * Instructs the provider to release a tunnel given the respective device.
     *
     * @param srcElement device
     * @param tunnel Tunnel
     */
    void releaseTunnel(ElementId srcElement, Tunnel tunnel);

    /**
     * Instructs the provider to update a tunnel.
     *
     * @param tunnel Tunnel
     * @param path explicit route (path changed) or null (path not changed) for the tunnel
     */
    void updateTunnel(Tunnel tunnel, Path path);

    /**
     * Instructs the provider to update a tunnel given the respective device.
     *
     * @param srcElement device
     * @param tunnel Tunnel
     * @param path explicit route (path changed) or null (path not changed) for the tunnel
     */
    void updateTunnel(ElementId srcElement, Tunnel tunnel, Path path);

}
