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
package org.onosproject.net.resource.impl;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.config.basics.BandwidthCapacity;
import org.onosproject.net.resource.ResourceAdminService;
import org.onosproject.net.resource.ResourceService;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A class registering resources when they are detected.
 */
@Component(immediate = true)
@Beta
public final class ResourceRegistrar {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceAdminService adminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgRegistry;

    private final Logger log = getLogger(getClass());

    private final List<ConfigFactory<?, ?>> factories = ImmutableList.of(
     new ConfigFactory<ConnectPoint, BandwidthCapacity>(CONNECT_POINT_SUBJECT_FACTORY,
             BandwidthCapacity.class, BandwidthCapacity.CONFIG_KEY) {
         @Override
         public BandwidthCapacity createConfig() {
             return new BandwidthCapacity();
         }
     });


    private DeviceListener deviceListener;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(groupedThreads("onos/resource", "registrar", log));

    private NetworkConfigListener cfgListener;

    @Activate
    public void activate() {
        factories.forEach(cfgRegistry::registerConfigFactory);

        cfgListener = new ResourceNetworkConfigListener(adminService, cfgRegistry, mastershipService, executor);
        cfgRegistry.addListener(cfgListener);

        deviceListener = new ResourceDeviceListener(adminService, resourceService,
                deviceService, mastershipService, driverService, cfgRegistry, executor);
        deviceService.addListener(deviceListener);

        // TODO Attempt initial registration of existing resources?

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        cfgRegistry.removeListener(cfgListener);

        executor.shutdownNow();

        factories.forEach(cfgRegistry::unregisterConfigFactory);

        log.info("Stopped");
    }
}
