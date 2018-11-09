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

import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.net.provider.ProviderService;

/**
 * APIs for tunnel provider to notify the tunnel subSystem.
 */
@Beta
public interface TunnelProviderService extends ProviderService<TunnelProvider> {

    /**
     * Signals that the provider has added a tunnel.
     *
     * @param tunnel tunnel information
     * @return tunnel identity
     */
    TunnelId tunnelAdded(TunnelDescription tunnel);

    /**
     * Signals that the provider has added a tunnel with a status which may not
     * be default, hence is provided as an input.
     *
     * @param tunnel tunnel information
     * @param state tunnel working status
     * @return tunnel identity
     */
    TunnelId tunnelAdded(TunnelDescription tunnel, State state);

    /**
     * Signals that the provider has removed a tunnel.
     *
     * @param tunnel tunnel information
     */
    void tunnelRemoved(TunnelDescription tunnel);

    /**
     * Signals that the a tunnel was changed (e.g., sensing changes of tunnel).
     *
     * @param tunnel tunnel information
     */
    void tunnelUpdated(TunnelDescription tunnel);

    /**
     * Signals that the tunnel was changed with tunnel status change.
     *
     * @param tunnel tunnel information
     * @param state tunnel working status
     */
    void tunnelUpdated(TunnelDescription tunnel, State state);

    /**
     * Signals that the a tunnel was queried.
     *
     * @param tunnelId tunnel identity
     * @return tunnel entity
     */
    Tunnel tunnelQueryById(TunnelId tunnelId);

}
