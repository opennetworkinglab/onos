/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routeservice.store;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.routeservice.InternalRouteEvent;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteSet;
import org.onosproject.routeservice.RouteStore;
import org.onosproject.routeservice.RouteStoreDelegate;
import org.onosproject.routeservice.RouteTableId;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Set;

import static org.onosproject.routeservice.store.OsgiPropertyConstants.DISTRIBUTED;
import static org.onosproject.routeservice.store.OsgiPropertyConstants.DISTRIBUTED_DEFAULT;

/**
 * An implementation of RouteStore that is backed by either LocalRouteStore or
 * DistributedRouteStore according to configuration.
 */
@Component(
    service = RouteStore.class,
    property = {
        DISTRIBUTED + ":Boolean=" + DISTRIBUTED_DEFAULT
    }
)
public class RouteStoreImpl extends AbstractStore<InternalRouteEvent, RouteStoreDelegate>
        implements RouteStore {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public StorageService storageService;

    /** Enable distributed route store. */
    private boolean distributed;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private RouteStore currentRouteStore;

    private DistributedRouteStore distributedRouteStore;
    private LocalRouteStore localRouteStore;

    @Activate
    public void activate(ComponentContext context) {
        distributedRouteStore = new DistributedRouteStore(storageService);
        distributedRouteStore.activate();
        localRouteStore = new LocalRouteStore();
        localRouteStore.activate();

        componentConfigService.registerProperties(getClass());
        modified(context);
    }

    @Deactivate
    public void deactivate() {
        localRouteStore.deactivate();
        distributedRouteStore.deactivate();

        componentConfigService.unregisterProperties(getClass(), false);
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        if (properties == null) {
            return;
        }

        String strDistributed = Tools.get(properties, DISTRIBUTED);
        boolean expectDistributed = Boolean.parseBoolean(strDistributed);

        // Start route store during first start or config change
        // NOTE: new route store will be empty
        if (currentRouteStore == null || expectDistributed != distributed) {
            if (expectDistributed) {
                currentRouteStore = distributedRouteStore;
            } else {
                currentRouteStore = localRouteStore;
            }

            this.distributed = expectDistributed;
            log.info("Switched to {} route store", distributed ? "distributed" : "local");
        }

    }

    @Override
    public void setDelegate(RouteStoreDelegate delegate) {
        super.setDelegate(delegate);

        // Set the delegate of underlying route store implementations
        localRouteStore.setDelegate(delegate);
        distributedRouteStore.setDelegate(delegate);
    }

    @Override
    public void unsetDelegate(RouteStoreDelegate delegate) {
        super.unsetDelegate(delegate);

        // Unset the delegate of underlying route store implementations
        localRouteStore.unsetDelegate(delegate);
        distributedRouteStore.unsetDelegate(delegate);
    }

    @Override
    public void updateRoute(Route route) {
        currentRouteStore.updateRoute(route);
    }

    @Override
    public void updateRoutes(Collection<Route> routes) {
        currentRouteStore.updateRoutes(routes);
    }

    @Override
    public void removeRoute(Route route) {
        currentRouteStore.removeRoute(route);
    }

    @Override
    public void removeRoutes(Collection<Route> routes) {
        currentRouteStore.removeRoutes(routes);
    }

    @Override
    public void replaceRoute(Route route) {
        currentRouteStore.replaceRoute(route);
    }

    @Override
    public Set<RouteTableId> getRouteTables() {
        return currentRouteStore.getRouteTables();
    }

    @Override
    public Collection<RouteSet> getRoutes(RouteTableId table) {
        return currentRouteStore.getRoutes(table);
    }

    @Override
    public Collection<Route> getRoutesForNextHop(IpAddress ip) {
        return currentRouteStore.getRoutesForNextHop(ip);
    }

    @Override
    public Collection<RouteSet> getRoutesForNextHops(Collection<IpAddress> ips) {
        return currentRouteStore.getRoutesForNextHops(ips);
    }

    @Override
    public RouteSet getRoutes(IpPrefix prefix) {
        return currentRouteStore.getRoutes(prefix);
    }

    @Override
    public String name() {
        return currentRouteStore.name();
    }
}
