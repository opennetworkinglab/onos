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
package org.onosproject.provider.pcep.tunnel.impl;

import java.util.Set;

import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderRegistry;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.net.provider.ProviderId;

public class TunnelProviderRegistryAdapter implements TunnelProviderRegistry {
    TunnelProvider provider;

    @Override
    public TunnelProviderService register(TunnelProvider provider) {
        this.provider = provider;
        return new TestProviderService();
    }

    @Override
    public void unregister(TunnelProvider provider) {
    }

    @Override
    public Set<ProviderId> getProviders() {
        return null;
    }

    private class TestProviderService implements TunnelProviderService {

        @Override
        public TunnelProvider provider() {
            return null;
        }

        @Override
        public TunnelId tunnelAdded(TunnelDescription tunnel) {
            return null;
        }

        @Override
        public TunnelId tunnelAdded(TunnelDescription tunnel, State state) {
            return null;
        }

        @Override
        public void tunnelRemoved(TunnelDescription tunnel) {
        }

        @Override
        public void tunnelUpdated(TunnelDescription tunnel) {
        }

        @Override
        public void tunnelUpdated(TunnelDescription tunnel, State state) {
        }

        @Override
        public Tunnel tunnelQueryById(TunnelId tunnelId) {
            return null;
        }
    }
}
