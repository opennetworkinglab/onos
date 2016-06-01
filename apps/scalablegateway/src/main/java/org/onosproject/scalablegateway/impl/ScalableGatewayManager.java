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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
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
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupService;
import org.onosproject.scalablegateway.api.GatewayNode;
import org.onosproject.scalablegateway.api.GatewayNodeConfig;
import org.onosproject.scalablegateway.api.ScalableGatewayService;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

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
    private static final String GATEWAYNODE_CAN_NOT_BE_NULL = "The gateway node can not be null";
    private static final String PORT_CAN_NOT_BE_NULL = "The port can not be null";
    private static final String FAIL_ADD_GATEWAY = "Adding process is failed as existing deivce id";
    private static final String FAIL_REMOVE_GATEWAY = "Removing process is failed as unknown deivce id";
    private static final String PORT_NAME = "portName";

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

    private GatewayNodeConfig config;
    private SelectGroupHandler selectGroupHandler;

    private final NetworkConfigListener configListener = new InternalConfigListener();

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, GatewayNodeConfig.class, APP_NAME) {
                @Override
                public GatewayNodeConfig createConfig() {
                    return new GatewayNodeConfig();
                }
            };
    private Map<DeviceId, GatewayNode> gatewayNodeMap = Maps.newHashMap(); // Map<GatewayNode`s Id, GatewayNode object>

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_ID);
        configRegistry.registerConfigFactory(configFactory);
        configService.addListener(configListener);

        selectGroupHandler = new SelectGroupHandler(groupService, deviceService, driverService, appId);
        readConfiguration();

        log.info("started");
    }

    @Deactivate
    protected void deactivate() {
        gatewayNodeMap.clear();

        configService.removeListener(configListener);

        log.info("stopped");
    }

    @Override
    public GatewayNode getGatewayNode(DeviceId deviceId) {
        return checkNotNull(gatewayNodeMap.get(deviceId), GATEWAYNODE_CAN_NOT_BE_NULL);
    }

    @Override
    public List<PortNumber> getGatewayExternalPorts(DeviceId deviceId) {
        GatewayNode gatewayNode = checkNotNull(gatewayNodeMap.get(deviceId), GATEWAYNODE_CAN_NOT_BE_NULL);
        List<PortNumber> portNumbers = Lists.newArrayList();
        gatewayNode.getGatewayExternalInterfaceNames()
                .stream()
                .forEach(name -> portNumbers.add(findPortNumFromPortName(gatewayNode.getGatewayDeviceId(), name)));
        return portNumbers;
    }

    private PortNumber findPortNumFromPortName(DeviceId gatewayDeviceId, String name) {
        Port port = deviceService.getPorts(gatewayDeviceId)
                .stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(name))
                .iterator()
                .next();
        return checkNotNull(port, PORT_CAN_NOT_BE_NULL).number();

    }

    @Override
    public GroupId getGroupIdForGatewayLoadBalance(DeviceId srcDeviceId) {
        GroupDescription description = selectGroupHandler.createSelectGroupInVxlan(srcDeviceId, getGatewayNodes());
        groupService.addGroup(description);
        Group group = groupService.getGroup(description.deviceId(), description.appCookie());
        return group != null ? group.id() : null;
    }

    @Override
    public List<GatewayNode> getGatewayNodes() {
        List<GatewayNode> gatewayNodeList = Lists.newArrayList();
        gatewayNodeMap.values()
                .stream()
                .forEach(gatewayNode -> gatewayNodeList.add(gatewayNode));
        return gatewayNodeList;

    }

    @Override
    public List<DeviceId> getGatewayDeviceIds() {
        List<DeviceId> deviceIdList = Lists.newArrayList();
        gatewayNodeMap.values()
                .stream()
                .forEach(gatewayNode -> deviceIdList.add(gatewayNode.getGatewayDeviceId()));
        return deviceIdList;

    }

    @Override
    public boolean addGatewayNode(GatewayNode gatewayNode) {
        gatewayNodeMap.putIfAbsent(gatewayNode.getGatewayDeviceId(), gatewayNode);
        return true;
    }

    @Override
    public boolean deleteGatewayNode(GatewayNode gatewayNode) {
        return gatewayNodeMap.remove(gatewayNode.getGatewayDeviceId(), gatewayNode);
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

    private void readConfiguration() {
        config = configService.getConfig(appId, GatewayNodeConfig.class);
        if (config == null) {
            log.error("No configuration found");
            return;
        }

        config.gatewayNodes().forEach(gatewayNode -> addGatewayNode(gatewayNode));

        log.info("ScalableGateway configured");
    }
}
