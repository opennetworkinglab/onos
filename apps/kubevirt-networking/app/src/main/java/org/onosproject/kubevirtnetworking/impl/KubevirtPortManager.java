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
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortAdminService;
import org.onosproject.kubevirtnetworking.api.KubevirtPortEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPortListener;
import org.onosproject.kubevirtnetworking.api.KubevirtPortService;
import org.onosproject.kubevirtnetworking.api.KubevirtPortStore;
import org.onosproject.kubevirtnetworking.api.KubevirtPortStoreDelegate;
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
 * Provides implementation of administering and interfacing kubevirt port.
 */
@Component(
        immediate = true,
        service = {KubevirtPortAdminService.class, KubevirtPortService.class }
)
public class KubevirtPortManager
        extends ListenerRegistry<KubevirtPortEvent, KubevirtPortListener>
        implements KubevirtPortAdminService, KubevirtPortService {

    protected final Logger log = getLogger(getClass());

    private static final String MSG_PORT = "Kubevirt port %s %s";
    private static final String MSG_CREATED = "created";
    private static final String MSG_UPDATED = "updated";
    private static final String MSG_REMOVED = "removed";

    private static final String ERR_NULL_PORT = "Kubevirt port cannot be null";
    private static final String ERR_NULL_PORT_MAC = "Kubevirt port MAC cannot be null";
    private static final String ERR_NULL_PORT_NET_ID = "Kubevirt port network ID cannot be null";
    private static final String ERR_IN_USE = " still in use";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPortStore kubevirtPortStore;

    private final InternalPortStorageDelegate delegate = new InternalPortStorageDelegate();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);

        kubevirtPortStore.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        kubevirtPortStore.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void createPort(KubevirtPort port) {
        checkNotNull(port, ERR_NULL_PORT);
        checkArgument(!Strings.isNullOrEmpty(port.macAddress().toString()), ERR_NULL_PORT_MAC);
        checkArgument(!Strings.isNullOrEmpty(port.networkId()), ERR_NULL_PORT_NET_ID);

        kubevirtPortStore.createPort(port);
        log.info(String.format(MSG_PORT, port.macAddress().toString(), MSG_CREATED));
    }

    @Override
    public void updatePort(KubevirtPort port) {
        checkNotNull(port, ERR_NULL_PORT);
        checkArgument(!Strings.isNullOrEmpty(port.macAddress().toString()), ERR_NULL_PORT_MAC);
        checkArgument(!Strings.isNullOrEmpty(port.networkId()), ERR_NULL_PORT_NET_ID);

        kubevirtPortStore.updatePort(port);
        log.debug(String.format(MSG_PORT, port.macAddress().toString(), MSG_UPDATED));
    }

    @Override
    public void removePort(MacAddress mac) {
        checkArgument(mac != null, ERR_NULL_PORT_MAC);
        synchronized (this) {
            if (isPortInUse(mac.toString())) {
                final String error = String.format(MSG_PORT, mac.toString(), ERR_IN_USE);
                throw new IllegalStateException(error);
            }
            KubevirtPort port = kubevirtPortStore.removePort(mac);
            if (port != null) {
                log.info(String.format(MSG_PORT, port.macAddress().toString(), MSG_REMOVED));
            }
        }
    }

    @Override
    public void clear() {
        kubevirtPortStore.clear();
    }

    @Override
    public KubevirtPort port(MacAddress mac) {
        checkArgument(mac != null, ERR_NULL_PORT_MAC);
        return kubevirtPortStore.port(mac);
    }

    @Override
    public Set<KubevirtPort> ports(String networkId) {
        checkArgument(!Strings.isNullOrEmpty(networkId), ERR_NULL_PORT_NET_ID);
        return ImmutableSet.copyOf(kubevirtPortStore.ports().stream()
                .filter(p -> p.networkId().equals(networkId))
                .collect(Collectors.toSet()));
    }

    @Override
    public Set<KubevirtPort> ports() {
        return ImmutableSet.copyOf(kubevirtPortStore.ports());
    }

    private boolean isPortInUse(String portId) {
        return false;
    }

    private class InternalPortStorageDelegate implements KubevirtPortStoreDelegate {

        @Override
        public void notify(KubevirtPortEvent event) {
            if (event != null) {
                log.trace("send kubevirt port event {}", event);
                process(event);
            }
        }
    }
}
