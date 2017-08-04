/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.resource.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.onlab.util.Bandwidth;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BandwidthCapacity;
import org.onosproject.net.resource.ResourceAdminService;
import org.onosproject.net.resource.Resources;
import org.slf4j.Logger;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

// TODO Consider merging this with ResourceDeviceListener.
/**
 * Handler for NetworkConfiguration changes.
 */
@Beta
final class ResourceNetworkConfigListener implements NetworkConfigListener {

    /**
     *  Config classes relevant to this listener.
     */
    private static final Set<Class<?>> CONFIG_CLASSES = ImmutableSet.of(BandwidthCapacity.class);

    private final Logger log = getLogger(getClass());

    private final ResourceAdminService adminService;
    private final NetworkConfigService cfgService;
    private final MastershipService mastershipService;
    private final ExecutorService executor;

    /**
     * Creates an instance of listener.
     *
     * @param adminService {@link ResourceAdminService}
     * @param cfgService {@link NetworkConfigService}
     * @param mastershipService {@link MastershipService}
     * @param executor Executor to use.
     */
    ResourceNetworkConfigListener(ResourceAdminService adminService, NetworkConfigService cfgService,
                                  MastershipService mastershipService, ExecutorService executor) {
        this.adminService = checkNotNull(adminService);
        this.cfgService = checkNotNull(cfgService);
        this.mastershipService = checkNotNull(mastershipService);
        this.executor = checkNotNull(executor);
    }

    @Override
    public boolean isRelevant(NetworkConfigEvent event) {
        switch (event.type()) {
        case CONFIG_ADDED:
        case CONFIG_REMOVED:
        case CONFIG_UPDATED:
            return CONFIG_CLASSES.contains(event.configClass());

        case CONFIG_REGISTERED:
        case CONFIG_UNREGISTERED:
        default:
            return false;
        }
    }

    @Override
    public void event(NetworkConfigEvent event) {
        if (event.configClass() == BandwidthCapacity.class) {
            executor.execute(() -> {
            try {
                handleBandwidthCapacity(event);
            } catch (Exception e) {
                log.error("Exception handling BandwidthCapacity", e);
            }
            });
        }
    }

    private void handleBandwidthCapacity(NetworkConfigEvent event) {
        checkArgument(event.configClass() == BandwidthCapacity.class);

        ConnectPoint cp = (ConnectPoint) event.subject();
        if (!mastershipService.isLocalMaster(cp.deviceId())) {
            return;
        }

        BandwidthCapacity bwCapacity = cfgService.getConfig(cp, BandwidthCapacity.class);

        switch (event.type()) {
        case CONFIG_ADDED:
            if (!adminService.register(Resources.continuous(cp.deviceId(),
                    cp.port(), Bandwidth.class)
                    .resource(bwCapacity.capacity().bps()))) {
                log.info("Failed to register Bandwidth for {}, attempting update", cp);

                // Bandwidth based on port speed, was probably already registered.
                // need to update to the valued based on configuration

                if (!updateRegistration(cp, bwCapacity)) {
                    log.warn("Failed to update Bandwidth for {}", cp);
                }
            }
            break;

        case CONFIG_UPDATED:
            if (!updateRegistration(cp, bwCapacity)) {
                log.warn("Failed to update Bandwidth for {}", cp);
            }
            break;

        case CONFIG_REMOVED:
            // FIXME Following should be an update to the value based on port speed
            if (!adminService.unregister(Resources.continuous(cp.deviceId(),
                    cp.port(),
                    Bandwidth.class).id())) {
                log.warn("Failed to unregister Bandwidth for {}", cp);
            }
            break;

        case CONFIG_REGISTERED:
        case CONFIG_UNREGISTERED:
            // no-op
            break;

        default:
            break;
        }
    }

    private boolean updateRegistration(ConnectPoint cp, BandwidthCapacity bwCapacity) {
        // FIXME workaround until replace/update semantics become available
        // this potentially blows up existing registration
        // or end up as no-op
        //
        // Current code end up in situation like below:
        //        PortNumber: 2
        //        MplsLabel: [[16‥240)]
        //        VlanId: [[0‥4095)]
        //        Bandwidth: 2000000.000000
        //        Bandwidth: 20000000.000000
        //
        // but both unregisterResources(..) and  registerResources(..)
        // returns true (success)

        if (!adminService.unregister(
                Resources.continuous(cp.deviceId(), cp.port(), Bandwidth.class).id())) {
            log.warn("unregisterResources for {} failed", cp);
        }
        return adminService.register(Resources.continuous(cp.deviceId(),
                cp.port(),
                Bandwidth.class).resource(bwCapacity.capacity().bps()));
    }

}
