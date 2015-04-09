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
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.group.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkState;

/**
 * Provides implementation of the flow objective programming service.
 */
@Component(immediate = true)
@Service
public class FlowObjectiveManager implements FlowObjectiveService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String NOT_INITIALIZED = "Driver not initialized";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    // Note: The following dependencies are added on behalf of the pipeline
    // driver behaviours to assure these services are available for their
    // initialization.
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;


    private final Map<DeviceId, DriverHandler> driverHandlers = Maps.newConcurrentMap();

    private final PipelinerContext context = new InnerPipelineContext();
    private final MastershipListener mastershipListener = new InnerMastershipListener();
    private final DeviceListener deviceListener = new InnerDeviceListener();

    protected ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
    private NodeId localNode;

    @Activate
    protected void activate() {
        localNode = clusterService.getLocalNode().id();
        mastershipService.addListener(mastershipListener);
        deviceService.addListener(deviceListener);
        deviceService.getDevices().forEach(device -> setupPipelineHandler(device.id()));
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
                                  Collection<FilteringObjective> filteringObjectives) {
        return getDevicePipeliner(deviceId).filter(filteringObjectives);
    }

    @Override
    public Future<Boolean> forward(DeviceId deviceId,
                                   Collection<ForwardingObjective> forwardingObjectives) {
        return getDevicePipeliner(deviceId).forward(forwardingObjectives);
    }

    @Override
    public Future<Boolean> next(DeviceId deviceId,
                                Collection<NextObjective> nextObjectives) {
        return getDevicePipeliner(deviceId).next(nextObjectives);
    }

    // Retrieves the device handler pipeline behaviour from the cache.
    private Pipeliner getDevicePipeliner(DeviceId deviceId) {
        DriverHandler handler = driverHandlers.get(deviceId);
        checkState(handler != null, NOT_INITIALIZED);
        return handler != null ? handler.behaviour(Pipeliner.class) : null;
    }


    // Triggers driver setup when the local node becomes a device master.
    private class InnerMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            switch (event.type()) {
                case MASTER_CHANGED:
                    setupPipelineHandler(event.subject());
                    break;
                default:
                    break;
            }
        }
    }

    // Triggers driver setup when a device is (re)detected.
    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    setupPipelineHandler(event.subject().id());
                    break;
                default:
                    break;
            }
        }
    }

    private void setupPipelineHandler(DeviceId deviceId) {
        if (localNode.equals(mastershipService.getMasterFor(deviceId))) {
            // Attempt to lookup the handler in the cache
            DriverHandler handler = driverHandlers.get(deviceId);
            if (handler == null) {
                try {
                    // Otherwise create it and if it has pipeline behaviour, cache it
                    handler = driverService.createHandler(deviceId);
                    if (!handler.driver().hasBehaviour(Pipeliner.class)) {
                        log.warn("Pipeline behaviour not supported for device {}",
                                 deviceId);
                        return;
                    }
                } catch (ItemNotFoundException e) {
                    log.warn("No applicable driver for device {}", deviceId);
                    return;
                }
                driverHandlers.put(deviceId, handler);
            }

            // Always (re)initialize the pipeline behaviour
            handler.behaviour(Pipeliner.class).init(deviceId, context);
            log.info("Driver {} bound to device {}", handler.driver().name(), deviceId);
        }
    }

    // Processing context for initializing pipeline driver behaviours.
    private class InnerPipelineContext implements PipelinerContext {
        @Override
        public ServiceDirectory directory() {
            return serviceDirectory;
        }
    }
}
