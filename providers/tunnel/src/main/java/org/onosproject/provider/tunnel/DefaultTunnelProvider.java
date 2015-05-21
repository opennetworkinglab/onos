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
package org.onosproject.provider.tunnel;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.ElementId;
import org.onosproject.net.Path;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderRegistry;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

/**
 * Provider of a fake network environment, i.e. devices, links, hosts, etc. To
 * be used for benchmarking only.
 */
@Component(immediate = true)
@Service
public class DefaultTunnelProvider extends AbstractProvider
        implements TunnelProvider {

    private static final Logger log = getLogger(DefaultTunnelProvider.class);

    static final String PROVIDER_ID = "org.onosproject.provider.tunnel.default";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelProviderRegistry tunnelProviderRegistry;

    TunnelProviderService service;

    /**
     * Creates a Tunnel provider.
     */
    public DefaultTunnelProvider() {
        super(new ProviderId("default", PROVIDER_ID));
    }

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        service = tunnelProviderRegistry.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
        tunnelProviderRegistry.unregister(this);
        log.info("Stopped");
    }

    @Override
    public void setupTunnel(Tunnel tunnel, Path path) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupTunnel(ElementId srcElement, Tunnel tunnel, Path path) {
        // TODO Auto-generated method stub

    }

    @Override
    public void releaseTunnel(Tunnel tunnel) {
        // TODO Auto-generated method stub

    }

    @Override
    public void releaseTunnel(ElementId srcElement, Tunnel tunnel) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTunnel(Tunnel tunnel, Path path) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTunnel(ElementId srcElement, Tunnel tunnel, Path path) {
        // TODO Auto-generated method stub

    }

    @Override
    public TunnelId tunnelAdded(TunnelDescription tunnel) {
        return service.tunnelAdded(tunnel);
    }

    @Override
    public void tunnelRemoved(TunnelDescription tunnel) {
        service.tunnelRemoved(tunnel);
    }

    @Override
    public void tunnelUpdated(TunnelDescription tunnel) {
        service.tunnelUpdated(tunnel);
    }

}
