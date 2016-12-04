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
import org.onosproject.tetunnel.api.lsp.TeLsp;
import org.onosproject.tetunnel.api.lsp.TeLspKey;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnelKey;

/**
 * Service through which tunnel providers can inject TE Tunnel attributes
 * into the system.
 * <p>
 * Please note that this service works with the existing Tunnel subsystem
 * together, just as an extension to the tunnel subsystem, and only focus on TE
 * Tunnel attributes management.
 */
public interface TeTunnelProviderService {

    /**
     * Signals that a TE Tunnel is created with supplied attributes.
     *
     * @param teTunnel new created TE Tunnel attributes
     * @return created tunnel identifier or null if failed
     */
    TunnelId teTunnelAdded(TeTunnel teTunnel);

    /**
     * Signals that a TE Tunnel with specified attributes is removed.
     *
     * @param teTunnel removed TE Tunnel
     */
    void teTunnelRemoved(TeTunnel teTunnel);

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
     * Signifies that a TE LSP is created.
     *
     * @param lsp new created TE LSP attributes
     * @return key of the TE LSP or null if failed
     */
    TeLspKey teLspAdded(TeLsp lsp);

    /**
     * Signifies that a TE LSP is removed.
     *
     * @param lsp removed TE LSP
     */
    void teLspRemoved(TeLsp lsp);

    /**
     * Updates TE LSP attributes.
     *
     * @param lsp new TE LSP attributes
     */
    void updateTeLsp(TeLsp lsp);

    //TODO: add interfaces for teGlobal and teLspState
}
