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
package org.onosproject.net.tunnel.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerRegistry;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Path;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.provider.AbstractProviderRegistry;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.BandwidthResource;
import org.onosproject.net.tunnel.Label;
import org.onosproject.net.tunnel.Tunnel;
import org.onosproject.net.tunnel.TunnelAdminService;
import org.onosproject.net.tunnel.TunnelDescription;
import org.onosproject.net.tunnel.TunnelEvent;
import org.onosproject.net.tunnel.TunnelId;
import org.onosproject.net.tunnel.TunnelListener;
import org.onosproject.net.tunnel.TunnelProvider;
import org.onosproject.net.tunnel.TunnelProviderRegistry;
import org.onosproject.net.tunnel.TunnelProviderService;
import org.onosproject.net.tunnel.TunnelService;
import org.onosproject.net.tunnel.TunnelStore;
import org.onosproject.net.tunnel.TunnelStoreDelegate;
import org.onosproject.net.tunnel.Tunnel.Type;
import org.slf4j.Logger;

/**
 * Provides implementation of the tunnel NB/SB APIs.
 */
@Component(immediate = true, enabled = true)
@Service
public class TunnelManager extends AbstractProviderRegistry<TunnelProvider, TunnelProviderService>
        implements TunnelService, TunnelAdminService, TunnelProviderRegistry {

    private static final String TUNNNEL_ID_NULL = "Tunnel ID cannot be null";

    private final Logger log = getLogger(getClass());

    protected final AbstractListenerRegistry<TunnelEvent, TunnelListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final TunnelStoreDelegate delegate = new InternalStoreDelegate();
    private final InternalTunnelListener tunnelListener = new InternalTunnelListener();
    private InternalLinkListener linkListener = new InternalLinkListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ExecutorService futureService;

    @Activate
    public void activate() {
        // TODO Auto-generated method stub
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        // TODO Auto-generated method stub
        log.info("Stopped");
    }

    @Override
    protected TunnelProviderService createProviderService(TunnelProvider provider) {
        // TODO Auto-generated method stub
        return new InternalTunnelProviderService(provider);
    }

    @Override
    public TunnelProviderService register(TunnelProvider provider) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void unregister(TunnelProvider provider) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<ProviderId> getProviders() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void removeTunnels(Label src, Label dst) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeTunnels(ConnectPoint connectPoint) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeTunnels(DeviceId deviceId) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getTunnelCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Collection<Tunnel> getTunnels(Type type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Tunnel> getTunnels(ConnectPoint connectPoint, Type type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tunnel getTunnel(Label src, Label dst) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tunnel getTunnel(TunnelId id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addListener(TunnelListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeListener(TunnelListener listener) {
        // TODO Auto-generated method stub

    }

    private class InternalTunnelListener implements TunnelListener {
        @Override
        public void event(TunnelEvent event) {
            // TODO Auto-generated method stub

        }
    }

    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            // TODO Auto-generated method stub

        }
    }

    private class InternalTunnelProviderService
            extends AbstractProviderService<TunnelProvider>
            implements TunnelProviderService {
        protected InternalTunnelProviderService(TunnelProvider provider) {
            super(provider);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void tunnelAdded(TunnelDescription tunnel) {
            // TODO Auto-generated method stub

        }

        @Override
        public void tunnelUpdated(TunnelDescription tunnel) {
            // TODO Auto-generated method stub

        }

        @Override
        public void tunnelRemoved(TunnelDescription tunnel) {
            // TODO Auto-generated method stub

        }

    }

    private class InternalStoreDelegate implements TunnelStoreDelegate {
        @Override
        public void notify(TunnelEvent event) {
            // TODO Auto-generated method stub
            if (event != null) {
                eventDispatcher.post(event);
            }
        }
    }

    @Override
    public void requestTunnel(ConnectPoint src, ConnectPoint dst,
                                BandwidthResource bw, Path path) {
        // TODO Auto-generated method stub
    }


    @Override
    public void requestTunnel(ConnectPoint src, ConnectPoint dst, Type type,
                              BandwidthResource bw, Path path) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeTunnel(TunnelId tunnelId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeTunnels(ConnectPoint src, ConnectPoint dst) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeTunnels(ConnectPoint src, ConnectPoint dst, Type type) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTunnel(Tunnel tunnel, Path path) {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection<Tunnel> getTunnels(ConnectPoint src, ConnectPoint dst,
                                       Type type) {
        // TODO Auto-generated method stub
        return null;
    }
}
