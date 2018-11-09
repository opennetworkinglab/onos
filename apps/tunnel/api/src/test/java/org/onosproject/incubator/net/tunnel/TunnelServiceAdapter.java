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

import org.onosproject.core.ApplicationId;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;

import java.util.Collection;
import java.util.Collections;

public class TunnelServiceAdapter implements TunnelService {
    @Override
    public Tunnel borrowTunnel(ApplicationId consumerId, TunnelId tunnelId, Annotations... annotations) {
        return null;
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelName tunnelName,
                                           Annotations... annotations) {
        return null;
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                           Annotations... annotations) {
        return null;
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                           Tunnel.Type type, Annotations... annotations) {
        return null;
    }

    @Override
    public TunnelId setupTunnel(ApplicationId producerId, ElementId srcElementId, Tunnel tunnel, Path path) {
        return null;
    }

    @Override
    public boolean downTunnel(ApplicationId producerId, TunnelId tunnelId) {
        return false;
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId, TunnelId tunnelId, Annotations... annotations) {
        return false;
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId, TunnelName tunnelName, Annotations... annotations) {
        return false;
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                Tunnel.Type type, Annotations... annotations) {
        return false;
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId, TunnelEndPoint src, TunnelEndPoint dst,
                                Annotations... annotations) {
        return false;
    }

    @Override
    public Tunnel queryTunnel(TunnelId tunnelId) {
        return null;
    }

    @Override
    public Collection<TunnelSubscription> queryTunnelSubscription(ApplicationId consumerId) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Tunnel> queryTunnel(Tunnel.Type type) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Tunnel> queryTunnel(TunnelEndPoint src, TunnelEndPoint dst) {
        return Collections.emptySet();
    }

    @Override
    public Collection<Tunnel> queryAllTunnels() {
        return Collections.emptyList();
    }

    @Override
    public int tunnelCount() {
        return 0;
    }

    @Override
    public Iterable<Tunnel> getTunnels(DeviceId deviceId) {
        return null;
    }

    @Override
    public void addListener(TunnelListener listener) {

    }

    @Override
    public void removeListener(TunnelListener listener) {

    }
}
