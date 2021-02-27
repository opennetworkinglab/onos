/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.KubevirtPeerRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterListener;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterStore;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterStoreDelegate;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of administering and interfacing kubevirt router.
 */
@Component(
        immediate = true,
        service = {KubevirtRouterAdminService.class, KubevirtRouterService.class }
)
public class KubevirtRouterManager
        extends ListenerRegistry<KubevirtRouterEvent, KubevirtRouterListener>
        implements KubevirtRouterAdminService, KubevirtRouterService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_ROUTER = "Kubevirt router %s %s";
    private static final String MSG_FLOATING_IP = "Kubevirt floating IP %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_ROUTER = "Kubevirt router cannot be null";
    private static final String ERR_NULL_ROUTER_NAME = "Kubevirt router name cannot be null";
    private static final String ERR_NULL_FLOATING_IP = "Kubevirt floating IP cannot be null";
    private static final String ERR_NULL_FLOATING_IP_ID = "Kubevirt floating IP ID cannot be null";
    private static final String ERR_NULL_POD_NAME = "Kubevirt POD name cannot be null";
    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtRouterStore kubevirtRouterStore;

    private final InternalRouterStorageDelegate delegate = new InternalRouterStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);

        kubevirtRouterStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        kubevirtRouterStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createRouter(KubevirtRouter router) {
        checkNotNull(router, ERR_NULL_ROUTER);
        checkArgument(!Strings.isNullOrEmpty(router.name()), ERR_NULL_ROUTER_NAME);

        kubevirtRouterStore.createRouter(router);
        log.info(String.format(MSG_ROUTER, router.name(), MSG_CREATED));
    }

    @Override
    public void updateRouter(KubevirtRouter router) {
        checkNotNull(router, ERR_NULL_ROUTER);
        checkArgument(!Strings.isNullOrEmpty(router.name()), ERR_NULL_ROUTER_NAME);

        kubevirtRouterStore.updateRouter(router);
        log.info(String.format(MSG_ROUTER, router.name(), MSG_UPDATED));
    }

    @Override
    public void removeRouter(String name) {
        checkArgument(name != null, ERR_NULL_ROUTER_NAME);
        synchronized (this) {
            if (isRouterInUse(name)) {
                final String error = String.format(MSG_ROUTER, name, ERR_IN_USE);
                throw new IllegalStateException(error);
            }

            KubevirtRouter router = kubevirtRouterStore.removeRouter(name);
            if (router != null) {
                log.info(String.format(MSG_ROUTER, router.name(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void updatePeerRouterMac(String name, MacAddress mac) {
        KubevirtRouter router = kubevirtRouterStore.router(name);
        if (router == null) {
            log.warn("The router is not found with the given name {}", name);
            return;
        }

        KubevirtPeerRouter existing = router.peerRouter();
        if (existing == null) {
            log.warn("The peer router is not found with the given name {}", name);
            return;
        }

        KubevirtPeerRouter updated = new KubevirtPeerRouter(existing.ipAddress(), mac);
        kubevirtRouterStore.updateRouter(router.updatePeerRouter(updated));
    }

    @Override
    public void createFloatingIp(KubevirtFloatingIp floatingIp) {
        checkNotNull(floatingIp, ERR_NULL_FLOATING_IP);
        checkArgument(!Strings.isNullOrEmpty(floatingIp.id()), ERR_NULL_FLOATING_IP_ID);

        kubevirtRouterStore.createFloatingIp(floatingIp);

        log.info(String.format(MSG_FLOATING_IP, floatingIp.floatingIp(), MSG_CREATED));
    }

    @Override
    public void updateFloatingIp(KubevirtFloatingIp floatingIp) {
        checkNotNull(floatingIp, ERR_NULL_FLOATING_IP);
        checkArgument(!Strings.isNullOrEmpty(floatingIp.id()), ERR_NULL_FLOATING_IP_ID);

        kubevirtRouterStore.updateFloatingIp(floatingIp);

        log.info(String.format(MSG_FLOATING_IP, floatingIp.floatingIp(), MSG_UPDATED));
    }

    @Override
    public void removeFloatingIp(String id) {
        checkArgument(!Strings.isNullOrEmpty(id), ERR_NULL_FLOATING_IP_ID);

        synchronized (this) {
            KubevirtFloatingIp floatingIp = kubevirtRouterStore.removeFloatingIp(id);

            if (floatingIp != null) {
                log.info(String.format(MSG_FLOATING_IP, floatingIp.floatingIp(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        kubevirtRouterStore.clear();
    }

    @Override
    public KubevirtRouter router(String name) {
        checkArgument(name != null, ERR_NULL_ROUTER_NAME);
        return kubevirtRouterStore.router(name);
    }

    @Override
    public Set<KubevirtRouter> routers() {
        return ImmutableSet.copyOf(kubevirtRouterStore.routers());
    }

    @Override
    public KubevirtFloatingIp floatingIp(String id) {
        checkArgument(!Strings.isNullOrEmpty(id), ERR_NULL_FLOATING_IP_ID);
        return kubevirtRouterStore.floatingIp(id);
    }

    @Override
    public KubevirtFloatingIp floatingIpByPodName(String podName) {
        checkArgument(!Strings.isNullOrEmpty(podName), ERR_NULL_POD_NAME);
        return kubevirtRouterStore.floatingIps().stream()
                .filter(ips -> podName.equals(ips.podName()))
                .findAny().orElse(null);
    }

    @Override
    public Set<KubevirtFloatingIp> floatingIpsByRouter(String routerName) {
        checkArgument(!Strings.isNullOrEmpty(routerName), ERR_NULL_ROUTER_NAME);
        return kubevirtRouterStore.floatingIps().stream()
                .filter(ips -> routerName.equals(ips.routerName()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<KubevirtFloatingIp> floatingIps() {
        return ImmutableSet.copyOf(kubevirtRouterStore.floatingIps());
    }

    private boolean isRouterInUse(String name) {
        return floatingIpsByRouter(name).size() > 0;
    }

    private class InternalRouterStorageDelegate implements KubevirtRouterStoreDelegate {

        @Override
        public void notify(KubevirtRouterEvent event) {
            log.trace("send kubevirt router event {}", event);
            process(event);
        }
    }
}
