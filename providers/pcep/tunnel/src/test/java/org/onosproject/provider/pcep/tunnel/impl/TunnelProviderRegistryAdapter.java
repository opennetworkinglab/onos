package org.onosproject.provider.pcep.tunnel.impl;

import java.util.Set;

import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderRegistry;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
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
        public void tunnelRemoved(TunnelDescription tunnel) {
        }

        @Override
        public void tunnelUpdated(TunnelDescription tunnel) {
        }

        @Override
        public Tunnel tunnelQueryById(TunnelId tunnelId) {
            return null;
        }
    }
}
