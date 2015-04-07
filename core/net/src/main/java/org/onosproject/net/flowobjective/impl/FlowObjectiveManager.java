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

    private Map<DeviceId, DriverHandler> driverHandlers =
            Maps.newConcurrentMap();

    @Activate
    protected void activate() {
        mastershipService.addListener(mastershipListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        mastershipService.removeListener(mastershipListener);
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
                    //TODO: refactor this into a method
                    if (event.roleInfo().master().equals(
                            clusterService.getLocalNode().id())) {
                        DriverHandler handler = lookupDriver(event.subject());
                        if (handler != null) {
                            Pipeliner pipe = handler.behaviour(Pipeliner.class);
                            pipe.init(event.subject(), serviceDirectory);
                            driverHandlers.put(event.subject(), handler);
                            log.info("Driver {} bound to device {}",
                                     handler.data().type().name(), event.subject());
                        } else {
                            log.error("No driver for device {}", event.subject());
                        }

                    }

                    break;
                case BACKUPS_CHANGED:
                    break;
                default:
                    log.warn("Unknown mastership type {}", event.type());
            }
        }

        private DriverHandler lookupDriver(DeviceId deviceId) {
            Device device = deviceService.getDevice(deviceId);

            Driver driver = driverService.getDriver(device.manufacturer(),
                                           device.hwVersion(), device.swVersion());

            return driverService.createHandler(driver.name(), deviceId);
        }
    }
}
