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

package org.onosproject.scalablegateway.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.scalablegateway.api.GatewayNode;
import org.onosproject.scalablegateway.api.GatewayNodeConfig;
import org.onosproject.scalablegateway.api.ScalableGatewayService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * Manages gateway node for gateway scalability.
 */

@Service
@Component(immediate = true)
public class ScalableGatewayManager implements ScalableGatewayService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    private static final String APP_ID = "org.onosproject.scalablegateway";
    private static final String APP_NAME = "scalablegateway";
    private static final String GATEWAYNODE_MAP_NAME = "gatewaynode-map";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private GatewayNodeConfig config;
    private SelectGroupHandler selectGroupHandler;

    private final NetworkConfigListener configListener = new InternalConfigListener();
    private final InternalDeviceListener internalDeviceListener = new InternalDeviceListener();

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, GatewayNodeConfig.class, APP_NAME) {
                @Override
                public GatewayNodeConfig createConfig() {
                    return new GatewayNodeConfig();
                }
            };

    private ConsistentMap<DeviceId, GatewayNode> gatewayNodeMap; // Map<GatewayNode Id, GatewayNode object>
    private static final KryoNamespace.Builder GATEWAYNODE_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(GatewayNode.class);

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);

        gatewayNodeMap = storageService.<DeviceId, GatewayNode>consistentMapBuilder()
                .withSerializer(Serializer.using(GATEWAYNODE_SERIALIZER.build()))
                .withName(GATEWAYNODE_MAP_NAME)
                .withApplicationId(appId)
                .build();

        configRegistry.registerConfigFactory(configFactory);
        configService.addListener(configListener);
        deviceService.addListener(internalDeviceListener);

        selectGroupHandler = new SelectGroupHandler(groupService, deviceService, driverService, appId);

        log.info("started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(internalDeviceListener);
        configService.removeListener(configListener);

        log.info("stopped");
    }

    @Override
    public GatewayNode getGatewayNode(DeviceId deviceId) {
        GatewayNode gatewayNode = gatewayNodeMap.get(deviceId).value();
        if (gatewayNode == null) {
            log.warn("Gateway with device ID {} does not exist");
            return null;
        }
        return gatewayNode;
    }

    @Override
    public PortNumber getUplinkPort(DeviceId deviceId) {
        GatewayNode gatewayNode = gatewayNodeMap.get(deviceId).value();
        if (gatewayNode == null) {
            log.warn("Gateway with device ID {} does not exist");
            return null;
        }

        Optional<Port> port = deviceService.getPorts(deviceId).stream()
                .filter(p -> Objects.equals(
                        p.annotations().value(PORT_NAME),
                        gatewayNode.getUplinkIntf()))
                .findFirst();
        if (!port.isPresent()) {
            log.warn("Cannot find uplink interface from gateway {}", deviceId);
            return null;
        }
        return port.get().number();
    }

    @Override
    public synchronized GroupId getGatewayGroupId(DeviceId srcDeviceId) {
        GroupKey groupKey = selectGroupHandler.getGroupKey(srcDeviceId);
        Group group = groupService.getGroup(srcDeviceId, groupKey);
        if (group == null) {
            log.info("Created gateway group for {}", srcDeviceId);
            return selectGroupHandler.createGatewayGroup(srcDeviceId, getGatewayNodes());
        } else {
            return group.id();
        }
    }

    @Override
    public List<GatewayNode> getGatewayNodes() {
        List<GatewayNode> gatewayNodeList = Lists.newArrayList();
        gatewayNodeMap.values()
                .stream()
                .map(Versioned::value)
                .forEach(gatewayNodeList::add);
        return gatewayNodeList;
    }

    @Override
    public List<DeviceId> getGatewayDeviceIds() {
        List<DeviceId> deviceIdList = Lists.newArrayList();
        gatewayNodeMap.values()
                .stream()
                .map(Versioned::value)
                .forEach(gatewayNode -> deviceIdList.add(gatewayNode.getGatewayDeviceId()));
        return deviceIdList;
    }

    @Override
    public synchronized boolean addGatewayNode(GatewayNode gatewayNode) {
        Versioned<GatewayNode> existingNode = gatewayNodeMap.put(gatewayNode.getGatewayDeviceId(),
                gatewayNode);
        if (existingNode == null) {
            updateGatewayGroup(gatewayNode, true);
            log.info("Gateway {} is added to Gateway pool", gatewayNode);
            return true;
        } else if (!existingNode.value().equals(gatewayNode)) {
            updateGatewayGroup(existingNode.value(), false);
            updateGatewayGroup(gatewayNode, true);
            log.info("Gateway {} is updated", gatewayNode);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean deleteGatewayNode(GatewayNode gatewayNode) {
        boolean result = gatewayNodeMap.remove(gatewayNode.getGatewayDeviceId(), gatewayNode);
        if (result) {
            updateGatewayGroup(gatewayNode, false);
            log.info("Deleted gateway with device ID {}", gatewayNode.getGatewayDeviceId());
        }
        return result;
    }

    private void updateGatewayGroup(GatewayNode gatewayNode, boolean isInsert) {
        Tools.stream(deviceService.getAvailableDevices()).forEach(device -> {
            Tools.stream(groupService.getGroups(device.id(), appId)).forEach(group -> {
                selectGroupHandler.updateGatewayGroupBuckets(
                        device.id(),
                        ImmutableList.of(gatewayNode),
                        isInsert);
                log.trace("Updated gateway group on {}", device.id());
            });
        });
    }

    private class InternalConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(GatewayNodeConfig.class)) {
                return;
            }
            switch (event.type()) {
                case CONFIG_UPDATED:
                    gatewayNodeMap.clear();
                    readConfiguration();
                    break;
                case CONFIG_ADDED:
                    readConfiguration();
                    break;
                default:
                    log.debug("Unsupportable event type is occurred");
                    break;
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent deviceEvent) {
            if (deviceEvent.type() == DeviceEvent.Type.DEVICE_SUSPENDED ||
                    deviceEvent.type() == DeviceEvent.Type.DEVICE_REMOVED) {
                DeviceId deviceId = deviceEvent.subject().id();
                deleteGatewayNode(getGatewayNode(deviceId));
                log.warn("Gateway with device ID {} is disconnected", deviceId);
            }
        }
    }

    private void readConfiguration() {
        config = configService.getConfig(appId, GatewayNodeConfig.class);
        if (config == null) {
            log.error("No configuration found");
            return;
        }

        config.gatewayNodes().forEach(this::addGatewayNode);
        log.info("ScalableGateway configured");
    }
}
