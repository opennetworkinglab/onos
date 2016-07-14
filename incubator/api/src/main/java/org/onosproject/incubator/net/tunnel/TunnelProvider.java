/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.net.provider.Provider;

/**
 * Abstraction of an entity providing tunnel setup/release services to the core.
 */
@Beta
public interface TunnelProvider extends Provider {

    /**
     * Instructs the provider to setup a tunnel. It's used by consumers.
     *
     * @param tunnel Tunnel
     * @param path explicit route or null for the tunnel
     */
    void setupTunnel(Tunnel tunnel, Path path);

    /**
     * Instructs the provider to setup a tunnel given the respective device.
     * It's used by consumers.
     *
     * @param srcElement device
     * @param tunnel Tunnel
     * @param path explicit route (not null) for the tunnel
     */
    void setupTunnel(ElementId srcElement, Tunnel tunnel, Path path);

    /**
     * Instructs the provider to release a tunnel. It's used by consumers.
     *
     * @param tunnel Tunnel
     */
    void releaseTunnel(Tunnel tunnel);

    /**
     * Instructs the provider to release a tunnel given the respective device.
     * It's used by consumers.
     *
     * @param srcElement device
     * @param tunnel Tunnel
     */
    void releaseTunnel(ElementId srcElement, Tunnel tunnel);

    /**
     * Instructs the provider to update a tunnel. It's used by consumers. Maybe
     * some consumers enable to update a tunnel.
     *
     * @param tunnel Tunnel
     * @param path explicit route (path changed) or null (path not changed) for
     *            the tunnel
     */
    void updateTunnel(Tunnel tunnel, Path path);

    /**
     * Instructs the provider to update a tunnel given the respective device.
     * It's used by consumers. Maybe some consumers enable to update a tunnel.
     *
     * @param srcElement device
     * @param tunnel Tunnel
     * @param path explicit route (path changed) or null (path not changed) for
     *            the tunnel
     */
    void updateTunnel(ElementId srcElement, Tunnel tunnel, Path path);

    /**
     * Signals that the provider has added a tunnel. It's used by producers.
     *
     * @param tunnel tunnel information
     * @return tunnel identity
     */
    TunnelId tunnelAdded(TunnelDescription tunnel);

    /**
     * Signals that the provider has removed a tunnel. It's used by producers.
     *
     * @param tunnel tunnel information
     */
    void tunnelRemoved(TunnelDescription tunnel);

    /**
     * Signals that the a tunnel was changed (e.g., sensing changes of
     * tunnel).It's used by producers.
     *
     * @param tunnel tunnel information
     */
    void tunnelUpdated(TunnelDescription tunnel);

    /**
     * Signals that the a tunnel was queried.
     * It's used by producers.
     * @param tunnelId tunnel identity
     * @return tunnel entity
     */
    Tunnel tunnelQueryById(TunnelId tunnelId);
}
