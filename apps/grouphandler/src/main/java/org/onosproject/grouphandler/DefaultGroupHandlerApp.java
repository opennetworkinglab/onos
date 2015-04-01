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
package org.onosproject.grouphandler;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.TopologyService;
import org.slf4j.Logger;

/**
 * Sample application to verify group subsystem end to end.
 * This application expects a network of maximum of six connected
 * devices for the test to work. For every device in the network,
 * this test application launches a default group handler function
 * that creates ECMP groups for every neighbor the device is
 * connected to.
 */
@Component(immediate = true)
public class DefaultGroupHandlerApp {

    private final Logger log = getLogger(getClass());

    private final DeviceProperties config = new DeviceConfiguration();
    private ApplicationId appId;
    private HashMap<DeviceId, DefaultGroupHandler> dghMap =
            new HashMap<DeviceId, DefaultGroupHandler>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    private DeviceListener deviceListener = new InternalDeviceListener();
    private LinkListener linkListener = new InternalLinkListener();

    protected KryoNamespace.Builder kryo = new KryoNamespace.Builder()
                        .register(URI.class)
                        .register(HashSet.class)
                        .register(DeviceId.class)
                        .register(NeighborSet.class);

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.defaultgrouphandler");
        log.info("DefaultGroupHandlerApp Activating");

        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        for (Device device: deviceService.getDevices()) {
            if (mastershipService.
                    getLocalRole(device.id()) == MastershipRole.MASTER) {
                log.debug("Initiating default group handling for {}", device.id());
                DefaultGroupHandler dgh = DefaultGroupHandler.createGroupHandler(device.id(),
                                                                  appId,
                                                                  config,
                                                                  linkService,
                                                                  groupService);
                dgh.createGroups();
                dghMap.put(device.id(), dgh);
            } else {
                log.debug("Activate: Local role {} "
                        + "is not MASTER for device {}",
                          mastershipService.
                          getLocalRole(device.id()),
                          device.id());
            }
        }

        log.info("Activated");
    }

    @Deactivate
    public void deactivate() {
        dghMap.clear();
    }

    public class DeviceConfiguration implements DeviceProperties {
        private final List<Integer> allSegmentIds =
                Arrays.asList(101, 102, 103, 104, 105, 106);
        private HashMap<DeviceId, Integer> deviceSegmentIdMap =
                new HashMap<DeviceId, Integer>() {
            {
                put(DeviceId.deviceId("of:0000000000000001"), 101);
                put(DeviceId.deviceId("of:0000000000000002"), 102);
                put(DeviceId.deviceId("of:0000000000000003"), 103);
                put(DeviceId.deviceId("of:0000000000000004"), 104);
                put(DeviceId.deviceId("of:0000000000000005"), 105);
                put(DeviceId.deviceId("of:0000000000000006"), 106);
            }
        };
        private final HashMap<DeviceId, MacAddress> deviceMacMap =
                new HashMap<DeviceId, MacAddress>() {
            {
                put(DeviceId.deviceId("of:0000000000000001"),
                     MacAddress.valueOf("00:00:00:00:00:01"));
                put(DeviceId.deviceId("of:0000000000000002"),
                     MacAddress.valueOf("00:00:00:00:00:02"));
                put(DeviceId.deviceId("of:0000000000000003"),
                     MacAddress.valueOf("00:00:00:00:00:03"));
                put(DeviceId.deviceId("of:0000000000000004"),
                     MacAddress.valueOf("00:00:00:00:00:04"));
                put(DeviceId.deviceId("of:0000000000000005"),
                     MacAddress.valueOf("00:00:00:00:00:05"));
                put(DeviceId.deviceId("of:0000000000000006"),
                     MacAddress.valueOf("00:00:00:00:00:06"));
            }
        };

        @Override
        public int getSegmentId(DeviceId deviceId) {
            if (deviceSegmentIdMap.get(deviceId) != null) {
                log.debug("getSegmentId for device{} is {}",
                          deviceId,
                          deviceSegmentIdMap.get(deviceId));
                return deviceSegmentIdMap.get(deviceId);
            } else {
                throw new IllegalStateException();
            }
        }
        @Override
        public MacAddress getDeviceMac(DeviceId deviceId) {
            if (deviceMacMap.get(deviceId) != null) {
                log.debug("getDeviceMac for device{} is {}",
                          deviceId,
                          deviceMacMap.get(deviceId));
                return deviceMacMap.get(deviceId);
            } else {
                throw new IllegalStateException();
            }
        }
        @Override
        public boolean isEdgeDevice(DeviceId deviceId) {
            return true;
        }
        @Override
        public List<Integer> getAllDeviceSegmentIds() {
            return allSegmentIds;
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
           if (mastershipService.
                    getLocalRole(event.subject().id()) != MastershipRole.MASTER) {
               log.debug("Local role {} is not MASTER for device {}",
                         mastershipService.
                         getLocalRole(event.subject().id()),
                         event.subject().id());
               return;
           }
           switch (event.type()) {
            case DEVICE_ADDED:
                log.debug("Initiating default group handling for {}", event.subject().id());
                DefaultGroupHandler dgh = DefaultGroupHandler.createGroupHandler(
                                                                  event.subject().id(),
                                                                  appId,
                                                                  config,
                                                                  linkService,
                                                                  groupService);
                dgh.createGroups();
                dghMap.put(event.subject().id(), dgh);
                break;
            case PORT_REMOVED:
                if (dghMap.get(event.subject().id()) != null) {
                    dghMap.get(event.subject().id()).portDown(event.port().number());
                }
                break;
            default:
                break;
            }

        }
    }

    private class InternalLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            if (mastershipService.
                    getLocalRole(event.subject().src().deviceId()) !=
                    MastershipRole.MASTER) {
                log.debug("InternalLinkListener: Local role {} "
                        + "is not MASTER for device {}",
                          mastershipService.
                          getLocalRole(event.subject().src().deviceId()),
                          event.subject().src().deviceId());
                return;
            }
            switch (event.type()) {
            case LINK_ADDED:
                if (dghMap.get(event.subject().src().deviceId()) != null) {
                    dghMap.get(event.subject().src().deviceId()).linkUp(event.subject());
                }
                break;
            default:
                break;
            }
        }

    }
}