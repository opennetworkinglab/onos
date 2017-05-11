/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.openstacknode;

import com.google.common.collect.Lists;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.openstacknode.OpenstackNodeService.NetworkMode;
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
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.onosproject.net.AnnotationKeys.PORT_MAC;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknode.Constants.*;
import static org.onosproject.net.group.DefaultGroupBucket.createSelectGroupBucket;

/**
 * Handles group generation request from OpenstackNode.
 */
public class SelectGroupHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String TUNNEL_DESTINATION = "tunnelDst";
    private static final String ERR_UNSUPPORTED_NET_TYPE = "Unsupported network type";

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
     * @param computeNode target device id for group description
     * @param gatewayNodeList gateway node list for bucket action
     */
    public void createGatewayGroup(OpenstackNode computeNode, List<OpenstackNode> gatewayNodeList) {
        List<GroupBucket> bucketList;
        GroupId groupId;

        if (computeNode.dataIp().isPresent()) {
            bucketList = generateBucketsForSelectGroup(computeNode, gatewayNodeList, NetworkMode.VXLAN);
            groupId = groupId(computeNode.intBridge(), NetworkMode.VXLAN);

            GroupDescription groupDescription = new DefaultGroupDescription(
                    computeNode.intBridge(),
                    GroupDescription.Type.SELECT,
                    new GroupBuckets(bucketList),
                    groupKey(computeNode.intBridge(), NetworkMode.VXLAN),
                    groupId.id(),
                    appId);

            groupService.addGroup(groupDescription);
        }

        if (computeNode.vlanPort().isPresent()) {
            bucketList = generateBucketsForSelectGroup(computeNode, gatewayNodeList, NetworkMode.VLAN);
            groupId = groupId(computeNode.intBridge(), NetworkMode.VLAN);

            GroupDescription groupDescription = new DefaultGroupDescription(
                    computeNode.intBridge(),
                    GroupDescription.Type.SELECT,
                    new GroupBuckets(bucketList),
                    groupKey(computeNode.intBridge(), NetworkMode.VLAN),
                    groupId.id(),
                    appId);

            groupService.addGroup(groupDescription);
        }
    }

    /**
     * Returns unique group key with supplied source device ID and network mode as a hash.
     * @param srcDeviceId source device id
     * @param networkMode network mode
     * @return group key
     */
    public GroupKey groupKey(DeviceId srcDeviceId, NetworkMode networkMode) {
        if (networkMode.equals(NetworkMode.VXLAN)) {
            return new DefaultGroupKey(srcDeviceId.toString().concat(DEFAULT_TUNNEL).getBytes());
        } else {
            return new DefaultGroupKey(srcDeviceId.toString().concat(VLAN).getBytes());
        }
    }

    private GroupId groupId(DeviceId srcDeviceId, NetworkMode networkMode) {
        if (networkMode.equals(NetworkMode.VXLAN)) {
            return new GroupId(srcDeviceId.toString().concat(DEFAULT_TUNNEL).hashCode());
        } else {
            return new GroupId(srcDeviceId.toString().concat(VLAN).hashCode());
        }
    }


    /**
     * Updates groupBuckets in select type group.
     *
     * @param computeNode compute node
     * @param gatewayNodeList updated gateway node list for bucket action
     * @param networkMode network mode
     * @param isInsert update type(add or remove)
     */
    public void updateGatewayGroupBuckets(OpenstackNode computeNode,
                                          List<OpenstackNode> gatewayNodeList,
                                          NetworkMode networkMode,
                                          boolean isInsert) {
        List<GroupBucket> bucketList = generateBucketsForSelectGroup(computeNode, gatewayNodeList, networkMode);
        GroupKey groupKey = groupKey(computeNode.intBridge(), networkMode);
        if (groupService.getGroup(computeNode.intBridge(), groupKey) == null) {
            log.error("There's no group in compute node {}", computeNode.intBridge());
            return;
        }

        if (isInsert) {
            groupService.addBucketsToGroup(
                    computeNode.intBridge(),
                    groupKey,
                    new GroupBuckets(bucketList),
                    groupKey, appId);
        } else {
            groupService.removeBucketsFromGroup(
                    computeNode.intBridge(),
                    groupKey,
                    new GroupBuckets(bucketList),
                    groupKey, appId);
        }
    }



    private List<GroupBucket> generateBucketsForSelectGroup(OpenstackNode computeNode,
                                                         List<OpenstackNode> gatewayNodeList,
                                                         NetworkMode networkMode) {
        List<GroupBucket> bucketList = Lists.newArrayList();

        switch (networkMode) {
            case VXLAN:
                gatewayNodeList.stream()
                        .filter(node -> node.dataIp().isPresent())
                        .forEach(node -> {
                            TrafficTreatment tBuilder = DefaultTrafficTreatment.builder()
                                    .extension(buildNiciraExtenstion(computeNode.intBridge(),
                                            node.dataIp().get().getIp4Address()),
                                            computeNode.intBridge())
                                    .setOutput(getTunnelPort(computeNode.intBridge()))
                                    .build();
                            bucketList.add(createSelectGroupBucket(tBuilder));
                        });
                return bucketList;
            case VLAN:
                gatewayNodeList.stream()
                        .filter(node -> node.vlanPort().isPresent())
                        .forEach(node -> {
                            TrafficTreatment tBuilder = DefaultTrafficTreatment.builder()
                                    .setEthDst(MacAddress.valueOf(vlanPortMac(node)))
                                    .setOutput(vlanPortNum(computeNode))
                                    .build();
                            bucketList.add(createSelectGroupBucket(tBuilder));
                        });
                return bucketList;
            default:
                final String error = String.format(
                        ERR_UNSUPPORTED_NET_TYPE + "%s",
                        networkMode.toString());
                throw new IllegalStateException(error);
        }
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
                .filter(p -> p.annotations().value(PORT_NAME).equals(DEFAULT_TUNNEL))
                .findAny().orElse(null);

        if (port == null) {
            log.error("No TunnelPort was created.");
            return null;
        }
        return port.number();
    }

    private PortNumber vlanPortNum(OpenstackNode node) {
        return deviceService.getPorts(node.intBridge()).stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(node.vlanPort().get()) &&
                        p.isEnabled())
                .map(Port::number).findFirst().get();

    }
    private String vlanPortMac(OpenstackNode node) {
        return deviceService.getPorts(node.intBridge()).stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(node.vlanPort().get()) && p.isEnabled())
                .findFirst().get().annotations().value(PORT_MAC);
    }
}
