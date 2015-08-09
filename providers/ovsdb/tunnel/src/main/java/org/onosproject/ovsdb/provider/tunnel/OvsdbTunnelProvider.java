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
package org.onosproject.ovsdb.provider.tunnel;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderRegistry;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

/**
 * Provider which uses when tunnel added/removed.
 */
@Component(immediate = true)
@Service
public class OvsdbTunnelProvider extends AbstractProvider
        implements TunnelProvider {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelService tunnelService;

    private TunnelProviderService providerService;

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    public OvsdbTunnelProvider() {
        super(new ProviderId("ovsdb", "org.onosproject.ovsdb.provider.tunnel"));
    }

    @Override
    public void setupTunnel(Tunnel tunnel, Path path) {
        // TODO: This will be implemented later.
    }

    @Override
    public void setupTunnel(ElementId srcElement, Tunnel tunnel, Path path) {
        // TODO: This will be implemented later.
    }

    @Override
    public void releaseTunnel(Tunnel tunnel) {
        // TODO: This will be implemented later.
    }

    @Override
    public void releaseTunnel(ElementId srcElement, Tunnel tunnel) {
        // TODO: This will be implemented later.
    }

    @Override
    public void updateTunnel(Tunnel tunnel, Path path) {
        // TODO: This will be implemented later.
    }

    @Override
    public void updateTunnel(ElementId srcElement, Tunnel tunnel, Path path) {
        // TODO: This will be implemented later.
    }

    @Override
    public TunnelId tunnelAdded(TunnelDescription tunnel) {
        return providerService.tunnelAdded(tunnel);
    }

    @Override
    public void tunnelRemoved(TunnelDescription tunnel) {
        providerService.tunnelRemoved(tunnel);
    }

    @Override
    public void tunnelUpdated(TunnelDescription tunnel) {
        providerService.tunnelUpdated(tunnel);
    }

    @Override
    public Tunnel tunnelQueryById(TunnelId tunnelId) {
        // TODO: This will be implemented later.
        return null;
    }
}
