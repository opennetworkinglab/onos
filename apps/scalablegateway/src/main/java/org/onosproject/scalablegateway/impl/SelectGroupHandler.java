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
import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.scalablegateway.api.GatewayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.onosproject.net.group.DefaultGroupBucket.createSelectGroupBucket;

/**
 * Handles group generation request from ScalableGateway.
 */
public class SelectGroupHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String TUNNEL_DESTINATION = "tunnelDst";
    private static final String PORTNAME_PREFIX_TUNNEL = "vxlan";
    private static final String PORTNAME = "portName";

    private final GroupService groupService;
    private final DeviceService deviceService;
    private final DriverService driverService;
    private final ApplicationId appId;

    /**
     * Default constructor.
     *
     * @param targetGroupService group service
     * @param targetDeviceService device service
     * @param targetDriverService driver service
     * @param appId application id for group service
     */
    public SelectGroupHandler(GroupService targetGroupService, DeviceService targetDeviceService,
                              DriverService targetDriverService, ApplicationId appId) {
        groupService = targetGroupService;
        deviceService = targetDeviceService;
        driverService = targetDriverService;
        this.appId = appId;
    }

    /**
     * Creates select type group description according to given deviceId.
     *
     * @param srcDeviceId target device id for group description
     * @param nodeList gateway node list for bucket action
     * @return created select type group description
     */
    public GroupDescription createSelectGroupInVxlan(DeviceId srcDeviceId, List<GatewayNode> nodeList) {
        List<GroupBucket> bucketList = generateBucketsForSelectGroup(srcDeviceId, nodeList);
        GroupKey key = generateGroupKey(srcDeviceId, nodeList);
        return new DefaultGroupDescription(srcDeviceId, GroupDescription.Type.SELECT,
                new GroupBuckets(bucketList), key, null, appId);
    }

    private GroupKey generateGroupKey(DeviceId srcDeviceId, List<GatewayNode> nodeList) {
        String cookie = srcDeviceId.toString();
        for (GatewayNode node : nodeList) {
            cookie = cookie.concat(node.getGatewayDeviceId().toString());
        }
        return new DefaultGroupKey(cookie.getBytes());

    }

    /**
     * Updates groupBuckets in select type group.
     *
     * @param deviceId target device id for group description
     * @param oldAppCookie group key for target group
     * @param nodeList updated gateway node list for bucket action
     * @param nodeInsertion update type(add or remove)
     * @return result of process
     */
    public boolean updateBucketToSelectGroupInVxlan(DeviceId deviceId, GroupKey oldAppCookie,
                                                    List<GatewayNode> nodeList, boolean nodeInsertion) {
        List<GroupBucket> bucketList = generateBucketsForSelectGroup(deviceId, nodeList);

        GroupKey newAppCookie = generateGroupKey(deviceId, nodeList);
        if (nodeInsertion) {
            groupService.addBucketsToGroup(deviceId, oldAppCookie,
                    new GroupBuckets(bucketList), newAppCookie, appId);
        } else {
            groupService.removeBucketsFromGroup(deviceId, oldAppCookie,
                    new GroupBuckets(bucketList), newAppCookie, appId);
        }
        Group group = groupService.getGroup(deviceId, newAppCookie);
        return group != null ? true : false;
    }

    private List<GroupBucket> generateBucketsForSelectGroup(DeviceId deviceId, List<GatewayNode> nodeList) {
        List<GroupBucket> bucketList = Lists.newArrayList();
        nodeList.forEach(node -> {
            TrafficTreatment tBuilder = DefaultTrafficTreatment.builder()
                    .extension(buildNiciraExtenstion(deviceId, node.getDataIpAddress()), deviceId)
                    .setOutput(getTunnelPort(deviceId))
                    .build();
            bucketList.add(createSelectGroupBucket(tBuilder));
        });
        return bucketList;
    }

    /**
     * Builds Nicira extension for tagging remoteIp of vxlan.
     *
     * @param id device id of vxlan source device
     * @param hostIp remote ip of vxlan destination device
     * @return NiciraExtension Treatment
     */
    private ExtensionTreatment buildNiciraExtenstion(DeviceId id, Ip4Address hostIp) {
        Driver driver = driverService.getDriver(id);
        DriverHandler driverHandler = new DefaultDriverHandler(new DefaultDriverData(driver, id));
        ExtensionTreatmentResolver resolver = driverHandler.behaviour(ExtensionTreatmentResolver.class);

        ExtensionTreatment extensionInstruction =
                resolver.getExtensionInstruction(
                        ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST.type());

        try {
            extensionInstruction.setPropertyValue(TUNNEL_DESTINATION, hostIp);
        } catch (ExtensionPropertyException e) {
            log.error("Error setting Nicira extension setting {}", e);
        }

        return extensionInstruction;
    }

    /**
     * Returns port number of vxlan tunnel.
     *
     * @param deviceId target Device Id
     * @return portNumber
     */
    private PortNumber getTunnelPort(DeviceId deviceId) {
        Port port = deviceService.getPorts(deviceId).stream()
                .filter(p -> p.annotations().value(PORTNAME).equals(PORTNAME_PREFIX_TUNNEL))
                .findAny().orElse(null);

        if (port == null) {
            log.error("No TunnelPort was created.");
            return null;
        }
        return port.number();

    }
}
