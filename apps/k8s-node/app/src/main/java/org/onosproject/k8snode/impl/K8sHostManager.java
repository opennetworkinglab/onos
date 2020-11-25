/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.k8snode.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.k8snode.api.K8sHost;
import org.onosproject.k8snode.api.K8sHostAdminService;
import org.onosproject.k8snode.api.K8sHostEvent;
import org.onosproject.k8snode.api.K8sHostListener;
import org.onosproject.k8snode.api.K8sHostService;
import org.onosproject.k8snode.api.K8sHostStore;
import org.onosproject.k8snode.api.K8sHostStoreDelegate;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snode.api.K8sHostState.COMPLETE;
import static org.onosproject.k8snode.impl.OsgiPropertyConstants.OVSDB_PORT;
import static org.onosproject.k8snode.impl.OsgiPropertyConstants.OVSDB_PORT_NUM_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service administering the inventory of kubernetes hosts.
 */
@Component(
        immediate = true,
        service = {K8sHostService.class, K8sHostAdminService.class},
        property = {
                OVSDB_PORT + ":Integer=" + OVSDB_PORT_NUM_DEFAULT
        }
)
public class K8sHostManager
        extends ListenerRegistry<K8sHostEvent, K8sHostListener>
        implements K8sHostService, K8sHostAdminService {

    private final Logger log = getLogger(getClass());

    private static final String MSG_HOST = "Kubernetes host %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_HOST = "Kubernetes host cannot be null";
    private static final String ERR_NULL_HOST_IP = "Kubernetes host IP cannot be null";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sHostStore hostStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    /** OVSDB server listen port. */
    private int ovsdbPortNum = OVSDB_PORT_NUM_DEFAULT;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final K8sHostStoreDelegate delegate = new K8sHostManager.InternalHostStoreDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        hostStore.setDelegate(delegate);

        leadershipService.runForLeadership(appId.name());
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        hostStore.unsetDelegate(delegate);

        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int updatedOvsdbPort = Tools.getIntegerProperty(properties, OVSDB_PORT);
        if (!Objects.equals(updatedOvsdbPort, ovsdbPortNum)) {
            ovsdbPortNum = updatedOvsdbPort;
        }

        log.info("Modified");
    }

    @Override
    public void createHost(K8sHost host) {
        checkNotNull(host, ERR_NULL_HOST);

        hostStore.createHost(host);

        log.info(String.format(MSG_HOST, host.hostIp().toString(), MSG_CREATED));
    }

    @Override
    public void updateHost(K8sHost host) {
        checkNotNull(host, ERR_NULL_HOST);

        hostStore.updateHost(host);

        log.info(String.format(MSG_HOST, host.hostIp().toString(), MSG_UPDATED));
    }

    @Override
    public K8sHost removeHost(IpAddress hostIp) {
        checkArgument(hostIp != null, ERR_NULL_HOST_IP);

        K8sHost host = hostStore.removeHost(hostIp);
        log.info(String.format(MSG_HOST, hostIp.toString(), MSG_REMOVED));

        return host;
    }

    @Override
    public Set<K8sHost> hosts() {
        return hostStore.hosts();
    }

    @Override
    public Set<K8sHost> completeHosts() {
        Set<K8sHost> hosts = hostStore.hosts().stream()
                .filter(h -> Objects.equals(h.state(), COMPLETE))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(hosts);
    }

    @Override
    public K8sHost host(IpAddress hostIp) {
        return hostStore.hosts().stream()
                .filter(h -> Objects.equals(h.hostIp(), hostIp))
                .findFirst().orElse(null);
    }

    @Override
    public K8sHost host(DeviceId deviceId) {
        return hostStore.hosts().stream()
                .filter(host -> Objects.equals(host.ovsdb(), deviceId))
                .findFirst().orElse(null);
    }

    @Override
    public K8sHost hostByTunBridge(DeviceId deviceId) {
        for (K8sHost host : hostStore.hosts()) {
            long cnt = host.tunBridges().stream().filter(
                    br -> br.dpid().equals(deviceId.toString())).count();
            if (cnt > 0) {
                return host;
            }
        }
        return null;
    }

    @Override
    public K8sHost hostByRouterBridge(DeviceId deviceId) {
        for (K8sHost host : hostStore.hosts()) {
            long cnt = host.routerBridges().stream().filter(
                    br -> br.dpid().equals(deviceId.toString())).count();
            if (cnt > 0) {
                return host;
            }
        }
        return null;
    }

    private class InternalHostStoreDelegate implements K8sHostStoreDelegate {

        @Override
        public void notify(K8sHostEvent event) {
            if (event != null) {
                log.trace("send kubernetes host event {}", event);
                process(event);
            }
        }
    }
}
