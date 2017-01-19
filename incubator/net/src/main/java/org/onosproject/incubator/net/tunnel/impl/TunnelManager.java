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
package org.onosproject.incubator.net.tunnel.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.incubator.net.tunnel.Tunnel.Type;
import org.onosproject.incubator.net.tunnel.TunnelAdminService;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelEvent;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelListener;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderRegistry;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.incubator.net.tunnel.TunnelStore;
import org.onosproject.incubator.net.tunnel.TunnelStoreDelegate;
import org.onosproject.incubator.net.tunnel.TunnelSubscription;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the tunnel NB/SB APIs.
 */
@Component(immediate = true, enabled = true)
@Service
public class TunnelManager
        extends AbstractListenerProviderRegistry<TunnelEvent, TunnelListener,
                                                 TunnelProvider, TunnelProviderService>
        implements TunnelService, TunnelAdminService, TunnelProviderRegistry {

    private static final String TUNNNEL_ID_NULL = "Tunnel ID cannot be null";
    private static final String TUNNNEL_NULL = "Tunnel cannot be null";

    private final Logger log = getLogger(getClass());

    private final TunnelStoreDelegate delegate = new InternalStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelStore store;


    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(TunnelEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        eventDispatcher.removeSink(TunnelEvent.class);
        log.info("Stopped");
    }

    @Override
    public void removeTunnel(TunnelId tunnelId) {
        checkNotNull(tunnelId, TUNNNEL_ID_NULL);
        Tunnel tunnel = store.queryTunnel(tunnelId);
        if (tunnel != null) {
            store.deleteTunnel(tunnelId);
            if (tunnel.providerId() != null) {
                TunnelProvider provider = getProvider(tunnel.providerId());
                if (provider != null) {
                    provider.releaseTunnel(tunnel);
                }
            } else {
                Set<ProviderId> ids = getProviders();
                for (ProviderId providerId : ids) {
                    TunnelProvider provider = getProvider(providerId);
                    provider.releaseTunnel(tunnel);
                }
            }
        }
    }

    @Override
    public void updateTunnel(Tunnel tunnel, Path path) {
        store.createOrUpdateTunnel(tunnel);
        if (tunnel.providerId() != null) {
            TunnelProvider provider = getProvider(tunnel.providerId());
            if (provider != null) {
                provider.updateTunnel(tunnel, path);
            }
        } else {
            Set<ProviderId> ids = getProviders();
            for (ProviderId providerId : ids) {
                TunnelProvider provider = getProvider(providerId);
                provider.updateTunnel(tunnel, path);
            }
        }
    }

    @Override
    public void updateTunnelState(Tunnel tunnel, State state) {
        Tunnel storedTunnel = store.queryTunnel(tunnel.tunnelId());
        store.createOrUpdateTunnel(storedTunnel, state);
    }

    @Override
    public void removeTunnels(TunnelEndPoint src, TunnelEndPoint dst,
                              ProviderId producerName) {
        Collection<Tunnel> setTunnels = store.queryTunnel(src, dst);
        if (!setTunnels.isEmpty()) {
            store.deleteTunnel(src, dst, producerName);
            for (Tunnel tunnel : setTunnels) {
                if (producerName != null
                        && !tunnel.providerId().equals(producerName)) {
                    continue;
                }
                if (tunnel.providerId() != null) {
                    TunnelProvider provider = getProvider(tunnel.providerId());
                    if (provider != null) {
                        provider.releaseTunnel(tunnel);
                    }
                } else {
                    Set<ProviderId> ids = getProviders();
                    for (ProviderId providerId : ids) {
                        TunnelProvider provider = getProvider(providerId);
                        provider.releaseTunnel(tunnel);
                    }
                }
            }
        }
    }

    @Override
    public void removeTunnels(TunnelEndPoint src, TunnelEndPoint dst, Type type,
                              ProviderId producerName) {
        Collection<Tunnel> setTunnels = store.queryTunnel(src, dst);
        if (!setTunnels.isEmpty()) {
            store.deleteTunnel(src, dst, type, producerName);
            for (Tunnel tunnel : setTunnels) {
                if (producerName != null
                        && !tunnel.providerId().equals(producerName)
                        || !type.equals(tunnel.type())) {
                    continue;
                }
                if (tunnel.providerId() != null) {
                    TunnelProvider provider = getProvider(tunnel.providerId());
                    if (provider != null) {
                        provider.releaseTunnel(tunnel);
                    }
                } else {
                    Set<ProviderId> ids = getProviders();
                    for (ProviderId providerId : ids) {
                        TunnelProvider provider = getProvider(providerId);
                        provider.releaseTunnel(tunnel);
                    }
                }
            }
        }
    }

    @Override
    public Tunnel borrowTunnel(ApplicationId consumerId, TunnelId tunnelId,
                                  Annotations... annotations) {
        return store.borrowTunnel(consumerId, tunnelId, annotations);
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId consumerId,
                                              TunnelName tunnelName,
                                              Annotations... annotations) {
        return store.borrowTunnel(consumerId, tunnelName, annotations);
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId consumerId,
                                              TunnelEndPoint src, TunnelEndPoint dst,
                                              Annotations... annotations) {
        Collection<Tunnel> tunnels = store.borrowTunnel(consumerId, src,
                                                           dst, annotations);
        if (tunnels == null || tunnels.isEmpty()) {
            Tunnel tunnel = new DefaultTunnel(null, src, dst, null, null, null,
                                              null, null, annotations);
            Set<ProviderId> ids = getProviders();
            for (ProviderId providerId : ids) {
                TunnelProvider provider = getProvider(providerId);
                provider.setupTunnel(tunnel, null);
            }
        }
        return tunnels;
    }

    @Override
    public Collection<Tunnel> borrowTunnel(ApplicationId consumerId,
                                              TunnelEndPoint src, TunnelEndPoint dst,
                                              Type type, Annotations... annotations) {
        Collection<Tunnel> tunnels = store.borrowTunnel(consumerId, src,
                                                           dst, type,
                                                           annotations);
        if (tunnels == null || tunnels.isEmpty()) {
            Tunnel tunnel = new DefaultTunnel(null, src, dst, type, null, null,
                                              null, null, annotations);
            Set<ProviderId> ids = getProviders();
            for (ProviderId providerId : ids) {
                TunnelProvider provider = getProvider(providerId);
                provider.setupTunnel(tunnel, null);
            }
        }
        return tunnels;
    }

    @Override
    public TunnelId setupTunnel(ApplicationId producerId, ElementId srcElementId, Tunnel tunnel, Path path) {
        // TODO: producerId to check if really required to consider while setup the tunnel.
        checkNotNull(tunnel, TUNNNEL_NULL);
        TunnelId tunnelId = store.createOrUpdateTunnel(tunnel, State.INIT);
        if (tunnelId != null) {
            Set<ProviderId> ids = getProviders();
            Tunnel newT = queryTunnel(tunnelId);
            for (ProviderId providerId : ids) {
                TunnelProvider provider = getProvider(providerId);
                provider.setupTunnel(srcElementId, newT, path);
            }
        }
        return tunnelId;
    }

    @Override
    public boolean downTunnel(ApplicationId producerId, TunnelId tunnelId) {
        // TODO: producerId to check if really required to consider while deleting the tunnel.
        checkNotNull(tunnelId, TUNNNEL_ID_NULL);
        Tunnel tunnel = store.queryTunnel(tunnelId);
        if (tunnel != null) {
            TunnelId updtTunnelId = store.createOrUpdateTunnel(tunnel, State.INACTIVE);
            if (updtTunnelId != null) {
                Set<ProviderId> ids = getProviders();
                for (ProviderId providerId : ids) {
                    TunnelProvider provider = getProvider(providerId);
                    provider.releaseTunnel(tunnel);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId,
                                     TunnelId tunnelId, Annotations... annotations) {
        return store.returnTunnel(consumerId, tunnelId, annotations);
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId,
                                     TunnelName tunnelName,
                                     Annotations... annotations) {
        return store.returnTunnel(consumerId, tunnelName, annotations);
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId, TunnelEndPoint src,
                                     TunnelEndPoint dst, Type type,
                                     Annotations... annotations) {
        return store.returnTunnel(consumerId, src, dst, type, annotations);
    }

    @Override
    public boolean returnTunnel(ApplicationId consumerId, TunnelEndPoint src,
                                     TunnelEndPoint dst, Annotations... annotations) {
        return store.returnTunnel(consumerId, src, dst, annotations);
    }

    @Override
    public Tunnel queryTunnel(TunnelId tunnelId) {
        return store.queryTunnel(tunnelId);
    }

    @Override
    public Collection<TunnelSubscription> queryTunnelSubscription(ApplicationId consumerId) {
        return store.queryTunnelSubscription(consumerId);
    }

    @Override
    public Collection<Tunnel> queryTunnel(Type type) {
        return store.queryTunnel(type);
    }

    @Override
    public Collection<Tunnel> queryTunnel(TunnelEndPoint src, TunnelEndPoint dst) {
        return store.queryTunnel(src, dst);
    }


    @Override
    public Collection<Tunnel> queryAllTunnels() {
        return store.queryAllTunnels();
    }

    @Override
    public int tunnelCount() {
        return store.tunnelCount();
    }

    @Override
    protected TunnelProviderService createProviderService(TunnelProvider provider) {
        return new InternalTunnelProviderService(provider);
    }

    private class InternalTunnelProviderService
            extends AbstractProviderService<TunnelProvider>
            implements TunnelProviderService {
        protected InternalTunnelProviderService(TunnelProvider provider) {
            super(provider);
        }


        @Override
        public TunnelId tunnelAdded(TunnelDescription tunnel) {
            Tunnel storedTunnel = new DefaultTunnel(provider().id(),
                                                    tunnel.src(), tunnel.dst(),
                                                    tunnel.type(),
                                                    tunnel.groupId(),
                                                    tunnel.id(),
                                                    tunnel.tunnelName(),
                                                    tunnel.path(),
                                                    tunnel.resource(),
                                                    tunnel.annotations());
            return store.createOrUpdateTunnel(storedTunnel);
        }

        @Override
        public TunnelId tunnelAdded(TunnelDescription tunnel, State state) {
            Tunnel storedTunnel = new DefaultTunnel(provider().id(),
                                                    tunnel.src(), tunnel.dst(),
                                                    tunnel.type(),
                                                    state,
                                                    tunnel.groupId(),
                                                    tunnel.id(),
                                                    tunnel.tunnelName(),
                                                    tunnel.path(),
                                                    tunnel.resource(),
                                                    tunnel.annotations());
            return store.createOrUpdateTunnel(storedTunnel);
        }

        @Override
        public void tunnelUpdated(TunnelDescription tunnel) {
            Tunnel storedTunnel = new DefaultTunnel(provider().id(),
                                                    tunnel.src(), tunnel.dst(),
                                                    tunnel.type(),
                                                    tunnel.groupId(),
                                                    tunnel.id(),
                                                    tunnel.tunnelName(),
                                                    tunnel.path(),
                                                    tunnel.resource(),
                                                    tunnel.annotations());
            store.createOrUpdateTunnel(storedTunnel);
        }

        @Override
        public void tunnelUpdated(TunnelDescription tunnel, State state) {
            Tunnel storedTunnel = new DefaultTunnel(provider().id(),
                                                    tunnel.src(), tunnel.dst(),
                                                    tunnel.type(),
                                                    state,
                                                    tunnel.groupId(),
                                                    tunnel.id(),
                                                    tunnel.tunnelName(),
                                                    tunnel.path(),
                                                    tunnel.resource(),
                                                    tunnel.annotations());
            store.createOrUpdateTunnel(storedTunnel, state);
        }

        @Override
        public void tunnelRemoved(TunnelDescription tunnel) {
            if (tunnel.id() != null) {
                store.deleteTunnel(tunnel.id());
                return;
            }
            if (tunnel.src() != null && tunnel.dst() != null
                    && tunnel.type() != null) {
                store.deleteTunnel(tunnel.src(), tunnel.dst(), tunnel.type(),
                                   provider().id());
                return;
            }
            if (tunnel.src() != null && tunnel.dst() != null
                    && tunnel.type() == null) {
                store.deleteTunnel(tunnel.src(), tunnel.dst(), provider().id());
                return;
            }
        }


        @Override
        public Tunnel tunnelQueryById(TunnelId tunnelId) {
            return store.queryTunnel(tunnelId);
        }


    }

    private class InternalStoreDelegate implements TunnelStoreDelegate {
        @Override
        public void notify(TunnelEvent event) {
            if (event != null) {
                post(event);
            }
        }
    }

    @Override
    public Iterable<Tunnel> getTunnels(DeviceId deviceId) {
        return Collections.emptyList();
    }

}
