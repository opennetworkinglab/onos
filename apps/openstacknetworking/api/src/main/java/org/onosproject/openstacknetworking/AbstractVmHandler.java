/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.Ip4Address;
import org.onlab.util.Tools;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.Constants.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides abstract virtual machine handler.
 */
public abstract class AbstractVmHandler {
    protected final Logger log = getLogger(getClass());

    protected final ExecutorService eventExecutor = newSingleThreadScheduledExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    protected CoreService coreService;
    protected MastershipService mastershipService;
    protected HostService hostService;

    protected HostListener hostListener = new InternalHostListener();

    protected void activate() {
        ServiceDirectory services = new DefaultServiceDirectory();
        coreService = services.get(CoreService.class);
        mastershipService = services.get(MastershipService.class);
        hostService = services.get(HostService.class);
        hostService.addListener(hostListener);

        log.info("Started");
    }

    protected void deactivate() {
        hostService.removeListener(hostListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    /**
     * Performs any action when a host is detected.
     *
     * @param host detected host
     */
    protected abstract void hostDetected(Host host);

    /**
     * Performs any action when a host is removed.
     *
     * @param host removed host
     */
    protected abstract void hostRemoved(Host host);

    protected boolean isValidHost(Host host) {
        return !host.ipAddresses().isEmpty() &&
                host.annotations().value(VXLAN_ID) != null &&
                host.annotations().value(NETWORK_ID) != null &&
                host.annotations().value(TENANT_ID) != null &&
                host.annotations().value(PORT_ID) != null;
    }

    protected Set<Host> getVmsInDifferentCnode(Host host) {
        return Tools.stream(hostService.getHosts())
                .filter(h -> !h.location().deviceId().equals(host.location().deviceId()))
                .filter(this::isValidHost)
                .filter(h -> Objects.equals(getVni(h), getVni(host)))
                .collect(Collectors.toSet());
    }

    protected Optional<Host> getVmByPortId(String portId) {
        return Tools.stream(hostService.getHosts())
                .filter(this::isValidHost)
                .filter(host -> host.annotations().value(PORT_ID).equals(portId))
                .findFirst();
    }

    protected Ip4Address getIp(Host host) {
        return host.ipAddresses().stream().findFirst().get().getIp4Address();
    }

    protected String getVni(Host host) {
        return host.annotations().value(VXLAN_ID);
    }

    protected String getTenantId(Host host) {
        return host.annotations().value(TENANT_ID);
    }

    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (!mastershipService.isLocalMaster(host.location().deviceId())) {
                // do not allow to proceed without mastership
                return;
            }

            if (!isValidHost(host)) {
                log.debug("Invalid host event, ignore it {}", host);
                return;
            }

            switch (event.type()) {
                case HOST_UPDATED:
                case HOST_ADDED:
                    eventExecutor.execute(() -> hostDetected(host));
                    break;
                case HOST_REMOVED:
                    eventExecutor.execute(() -> hostRemoved(host));
                    break;
                default:
                    break;
            }
        }
    }
}
