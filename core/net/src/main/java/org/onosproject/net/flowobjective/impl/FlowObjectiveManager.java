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
package org.onosproject.net.flowobjective.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.cluster.ClusterService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by ash on 07/04/15.
 */
@Component(immediate = true)
@Service
public class FlowObjectiveManager implements FlowObjectiveService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    protected ServiceDirectory serviceDirectory = new DefaultServiceDirectory();

    private MastershipListener mastershipListener = new InnerMastershipListener();

    private DeviceListener deviceListener = new InnerDeviceListener();

    private Map<DeviceId, DriverHandler> driverHandlers =
            Maps.newConcurrentMap();

    @Activate
    protected void activate() {
        mastershipService.addListener(mastershipListener);
        deviceService.addListener(deviceListener);
        deviceService.getDevices().forEach(device -> setupDriver(device.id()));
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        mastershipService.removeListener(mastershipListener);
        deviceService.removeListener(deviceListener);
        log.info("Stopped");
    }

    @Override
    public Future<Boolean> filter(DeviceId deviceId,
                                  Collection<FilteringObjective> filterObjectives) {
        DriverHandler handler = driverHandlers.get(deviceId);
        checkState(handler != null, "Driver not initialized");

        Pipeliner pipe = handler.behaviour(Pipeliner.class);

        return pipe.filter(filterObjectives);
    }

    @Override
    public Future<Boolean> forward(DeviceId deviceId,
                                   Collection<ForwardingObjective> forwardingObjectives) {
        DriverHandler handler = driverHandlers.get(deviceId);
        checkState(handler != null, "Driver not initialized");

        Pipeliner pipe = handler.behaviour(Pipeliner.class);

        return pipe.forward(forwardingObjectives);
    }

    @Override
    public Future<Boolean> next(DeviceId deviceId,
                                Collection<NextObjective> nextObjectives) {
        DriverHandler handler = driverHandlers.get(deviceId);
        checkState(handler != null, "Driver not initialized");

        Pipeliner pipe = handler.behaviour(Pipeliner.class);

        return pipe.next(nextObjectives);
    }



    private class InnerMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            switch (event.type()) {

                case MASTER_CHANGED:
                    setupDriver(event.subject());

                    break;
                case BACKUPS_CHANGED:
                    break;
                default:
                    log.warn("Unknown mastership type {}", event.type());
            }
        }


    }

    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    setupDriver(event.subject().id());
                    break;
                case DEVICE_UPDATED:
                    break;
                case DEVICE_REMOVED:
                    break;
                case DEVICE_SUSPENDED:
                    break;
                case PORT_ADDED:
                    break;
                case PORT_UPDATED:
                    break;
                case PORT_REMOVED:
                    break;
                default:
                    log.warn("Unknown event type {}", event.type());
            }
        }
    }

    private void setupDriver(DeviceId deviceId) {
        //TODO: Refactor this to make it nicer and use a cache.
        if (mastershipService.getMasterFor(
                deviceId).equals(clusterService.getLocalNode().id())) {

            DriverHandler handler = lookupDriver(deviceId);
            if (handler != null) {
                Pipeliner pipe = handler.behaviour(Pipeliner.class);
                pipe.init(deviceId, serviceDirectory);
                driverHandlers.put(deviceId, handler);
                log.info("Driver {} bound to device {}",
                         handler.data().type().name(), deviceId);
            } else {
                log.error("No driver for device {}", deviceId);
            }
        }
    }


    private DriverHandler lookupDriver(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            log.warn("Device is null!");
            return null;
        }
        Driver driver = driverService.getDriver(device.manufacturer(),
                                                device.hwVersion(), device.swVersion());

        return driverService.createHandler(driver.name(), deviceId);
    }

}
