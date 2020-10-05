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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknetworking.api.OpenstackRouterStore;
import org.onosproject.openstacknetworking.api.OpenstackRouterStoreDelegate;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.deriveResourceName;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing OpenStack router and
 * floating IP address.
 */
@Component(
    immediate = true,
    service = { OpenstackRouterAdminService.class, OpenstackRouterService.class }
)
public class OpenstackRouterManager
        extends ListenerRegistry<OpenstackRouterEvent, OpenstackRouterListener>
        implements OpenstackRouterAdminService, OpenstackRouterService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_ROUTER = "OpenStack router %s %s";
    private static final String MSG_ROUTER_IFACE = "OpenStack router interface %s %s";
    private static final String MSG_FLOATING_IP = "OpenStack floating IP %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_ROUTER =
                                "OpenStack router cannot be null";
    private static final String ERR_NULL_ROUTER_ID =
                                "OpenStack router ID cannot be null";
    private static final String ERR_NULL_ROUTER_NAME =
                                "OpenStack router name cannot be null";
    private static final String ERR_NULL_IFACE =
                                "OpenStack router interface cannot be null";
    private static final String ERR_NULL_IFACE_ROUTER_ID =
                                "OpenStack router interface router ID cannot be null";
    private static final String ERR_NULL_IFACE_PORT_ID =
                                "OpenStack router interface port ID cannot be null";
    private static final String ERR_NULL_FLOATING =
                                "OpenStack floating IP cannot be null";
    private static final String ERR_NULL_FLOATING_ID =
                                "OpenStack floating IP cannot be null";

    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackRouterStore osRouterStore;

    private final OpenstackRouterStoreDelegate
                                    delegate = new InternalRouterStoreDelegate();

    @Activate
    protected void activate() {
        coreService.registerApplication(Constants.OPENSTACK_NETWORKING_APP_ID);
        osRouterStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osRouterStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createRouter(Router osRouter) {
        checkNotNull(osRouter, ERR_NULL_ROUTER);
        checkArgument(!Strings.isNullOrEmpty(osRouter.getId()), ERR_NULL_ROUTER_ID);

        osRouterStore.createRouter(osRouter);
        log.info(String.format(MSG_ROUTER, deriveResourceName(osRouter), MSG_CREATED));
    }

    @Override
    public void updateRouter(Router osRouter) {
        checkNotNull(osRouter, ERR_NULL_ROUTER);
        checkArgument(!Strings.isNullOrEmpty(osRouter.getId()), ERR_NULL_ROUTER_ID);

        osRouterStore.updateRouter(osRouter);
        log.info(String.format(MSG_ROUTER, osRouter.getId(), MSG_UPDATED));
    }

    @Override
    public void removeRouter(String routerId) {
        checkArgument(!Strings.isNullOrEmpty(routerId), ERR_NULL_ROUTER_ID);
        synchronized (this) {
            if (isRouterInUse(routerId)) {
                final String error = String.format(MSG_ROUTER, routerId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            Router osRouter = osRouterStore.removeRouter(routerId);
            if (osRouter != null) {
                log.info(String.format(MSG_ROUTER, deriveResourceName(osRouter), MSG_REMOVED));
            }
        }
    }

    @Override
    public void addRouterInterface(RouterInterface osIface) {
        checkNotNull(osIface, ERR_NULL_IFACE);
        checkArgument(!Strings.isNullOrEmpty(osIface.getId()), ERR_NULL_IFACE_ROUTER_ID);
        checkArgument(!Strings.isNullOrEmpty(osIface.getPortId()), ERR_NULL_IFACE_PORT_ID);

        osRouterStore.addRouterInterface(osIface);
        log.info(String.format(MSG_ROUTER_IFACE, osIface.getPortId(), MSG_CREATED));
    }

    @Override
    public void updateRouterInterface(RouterInterface osIface) {
        checkNotNull(osIface, ERR_NULL_IFACE);
        checkArgument(!Strings.isNullOrEmpty(osIface.getId()), ERR_NULL_IFACE_ROUTER_ID);
        checkArgument(!Strings.isNullOrEmpty(osIface.getPortId()), ERR_NULL_IFACE_PORT_ID);

        osRouterStore.updateRouterInterface(osIface);
        log.info(String.format(MSG_ROUTER_IFACE, osIface.getPortId(), MSG_UPDATED));
    }

    @Override
    public void removeRouterInterface(String osIfaceId) {
        checkArgument(!Strings.isNullOrEmpty(osIfaceId), ERR_NULL_IFACE_PORT_ID);
        synchronized (this) {
            if (isRouterIfaceInUse(osIfaceId)) {
                final String error = String.format(MSG_ROUTER, osIfaceId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            RouterInterface osIface = osRouterStore.removeRouterInterface(osIfaceId);
            if (osIface != null) {
                log.info(String.format(MSG_ROUTER_IFACE, osIface.getPortId(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void createFloatingIp(NetFloatingIP osFloatingIp) {
        checkNotNull(osFloatingIp, ERR_NULL_FLOATING);
        checkArgument(!Strings.isNullOrEmpty(osFloatingIp.getId()), ERR_NULL_FLOATING_ID);

        osRouterStore.createFloatingIp(osFloatingIp);
        log.info(String.format(MSG_FLOATING_IP, osFloatingIp.getId(), MSG_CREATED));
    }

    @Override
    public void updateFloatingIp(NetFloatingIP osFloatingIp) {
        checkNotNull(osFloatingIp, ERR_NULL_FLOATING);
        checkArgument(!Strings.isNullOrEmpty(osFloatingIp.getId()), ERR_NULL_FLOATING_ID);

        osRouterStore.updateFloatingIp(osFloatingIp);
        log.info(String.format(MSG_FLOATING_IP, osFloatingIp.getId(), MSG_UPDATED));
    }

    @Override
    public void removeFloatingIp(String floatingIpId) {
        checkArgument(!Strings.isNullOrEmpty(floatingIpId), ERR_NULL_FLOATING_ID);
        synchronized (this) {
            if (isFloatingIpInUse(floatingIpId)) {
                final String error = String.format(MSG_FLOATING_IP, floatingIpId, ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            NetFloatingIP osFloatingIp = osRouterStore.removeFloatingIp(floatingIpId);
            if (osFloatingIp != null) {
                log.info(String.format(MSG_FLOATING_IP, osFloatingIp.getId(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        osRouterStore.clear();
    }

    @Override
    public Router router(String routerId) {
        checkArgument(!Strings.isNullOrEmpty(routerId), ERR_NULL_ROUTER_ID);
        return osRouterStore.router(routerId);
    }

    @Override
    public Set<Router> routers() {
        return osRouterStore.routers();
    }

    @Override
    public RouterInterface routerInterface(String osIfaceId) {
        checkArgument(!Strings.isNullOrEmpty(osIfaceId), ERR_NULL_IFACE_PORT_ID);
        return osRouterStore.routerInterface(osIfaceId);
    }

    @Override
    public Set<RouterInterface> routerInterfaces() {
        return osRouterStore.routerInterfaces();
    }

    @Override
    public Set<RouterInterface> routerInterfaces(String routerId) {
        Set<RouterInterface> osIfaces = osRouterStore.routerInterfaces().stream()
                .filter(iface -> Objects.equals(iface.getId(), routerId))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osIfaces);
    }

    @Override
    public NetFloatingIP floatingIp(String floatingIpId) {
        checkArgument(!Strings.isNullOrEmpty(floatingIpId), ERR_NULL_FLOATING_ID);
        return osRouterStore.floatingIp(floatingIpId);
    }

    @Override
    public Set<NetFloatingIP> floatingIps() {
        return osRouterStore.floatingIps();
    }

    @Override
    public Set<NetFloatingIP> floatingIps(String routerId) {
        Set<NetFloatingIP> osFloatingIps = osRouterStore.floatingIps().stream()
                .filter(fip -> Objects.equals(fip.getRouterId(), routerId))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osFloatingIps);
    }

    private boolean isRouterInUse(String routerId) {
        // TODO add checking
        return false;
    }

    private boolean isRouterIfaceInUse(String osIfaceId) {
        // TODO add checking
        return false;
    }

    private boolean isFloatingIpInUse(String floatingIpId) {
        // TODO add checking
        return false;
    }

    private class InternalRouterStoreDelegate implements OpenstackRouterStoreDelegate {

        @Override
        public void notify(OpenstackRouterEvent event) {
            if (event != null) {
                log.trace("send openstack routing event {}", event);
                process(event);
            }
        }
    }
}
