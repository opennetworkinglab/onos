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

package org.onosproject.l3vpn.netl3vpn.impl;

import org.onosproject.l3vpn.netl3vpn.AccessInfo;
import org.onosproject.l3vpn.netl3vpn.BgpDriverInfo;
import org.onosproject.l3vpn.netl3vpn.BgpInfo;
import org.onosproject.l3vpn.netl3vpn.InterfaceInfo;
import org.onosproject.l3vpn.netl3vpn.NetL3VpnException;
import org.onosproject.l3vpn.netl3vpn.VpnType;
import org.onosproject.net.DeviceId;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.device.Interfaces;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.DefaultL3VpnSvc;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.SiteRole;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.l3vpnsvc.DefaultSites;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.DefaultDevices;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.Devices;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.DefaultDevice;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.Device;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.DeviceKeys;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.NetworkInstances;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.model.InnerModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.onosproject.l3vpn.netl3vpn.BgpModelIdLevel.DEVICE;
import static org.onosproject.l3vpn.netl3vpn.BgpModelIdLevel.DEVICES;
import static org.onosproject.l3vpn.netl3vpn.BgpModelIdLevel.ROOT;
import static org.onosproject.l3vpn.netl3vpn.BgpModelIdLevel.VPN;
import static org.onosproject.l3vpn.netl3vpn.VpnType.ANY_TO_ANY;
import static org.onosproject.l3vpn.netl3vpn.VpnType.HUB;
import static org.onosproject.l3vpn.netl3vpn.VpnType.SPOKE;

/**
 * Representation of utility for YANG tree builder.
 */
public final class NetL3VpnUtil {

    /**
     * Error message for site VPN name being not present in global VPN.
     */
    static final String SITE_VPN_MISMATCH = "Site VPN instance name did not " +
            "match any of the global VPN names";

    /**
     * Error message for VPN attachment object being null.
     */
    static final String VPN_ATTACHMENT_NULL = "The VPN attachment information" +
            " cannot be null";

    /**
     * Error message for VPN policy being not supported.
     */
    static final String VPN_POLICY_NOT_SUPPORTED = "VPN policy implementation" +
            " is not supported.";

    /**
     * Static constant value for hundred.
     */
    static final String CONS_HUNDRED = "100:";

    /**
     * Error message for site role being not present in site network access.
     */
    static final String SITE_ROLE_NULL = "There must be a site role available" +
            " for the VPN in site network access.";

    /**
     * Error message for bearer object information being null.
     */
    static final String BEARER_NULL = "The bearer information of the access " +
            "is not available";

    /**
     * Error message for requested type or ip connect being null.
     */
    static final String IP_INT_INFO_NULL = "The required information of " +
            "request type or ip connection is not available";

    /**
     * Error message for device info being not available from augment.
     */
    static final String DEVICE_INFO_NULL = "Bearer of site does not have any " +
            "device information in the augment info.";

    /**
     * Static constant value for ip address.
     */
    static final String IP = "ipaddress";

    /**
     * Error message for VPN type being not supported.
     */
    static final String VPN_TYPE_UNSUPPORTED = "The VPN type is not supported";

    /**
     * Error message when the generated ID has crossed the limit.
     */
    static final String ID_LIMIT_EXCEEDED = "The ID generation has got " +
            "exceeded";

    /**
     * Static constant value ID management limit.
     */
    static final Long ID_LIMIT = 4294967295L;

    /**
     * Error message for interface information being not available.
     */
    static final String INT_INFO_NULL = "Requested type does not have any " +
            "interface information in the augment info.";

    /**
     * Static constant value of port name.
     */
    static final String PORT_NAME = "portName";

    /**
     * Static constants to use with accumulator for maximum number of events.
     */
    static final int MAX_EVENTS = 1000;

    /**
     * Static constants to use with accumulator for maximum number of millis.
     */
    static final int MAX_BATCH_MS = 5000;

    /**
     * Static constants to use with accumulator for maximum number of idle
     * millis.
     */
    static final int MAX_IDLE_MS = 1000;

    /**
     * Static constants for timer name.
     */
    static final String TIMER = "dynamic-config-l3vpn-timer";

    /**
     * Error message for unknown event being occurred.
     */
    static final String UNKNOWN_EVENT = "NetL3VPN listener: unknown event: {}";

    /**
     * Error message for event being null.
     */
    static final String EVENT_NULL = "Event cannot be null";

    private static final String SITE_ROLE_INVALID = "The given site role is " +
            "invalid";
    private static final String ANY_TO_ANY_ROLE = "AnyToAnyRole";
    private static final String HUB_ROLE = "HubRole";
    private static final String SPOKE_ROLE = "SpokeRole";

    // No instantiation.
    private NetL3VpnUtil() {
    }

    /**
     * Returns the model object id for service L3VPN container.
     *
     * @return model object id
     */
    static ModelObjectId getModIdForL3VpnSvc() {
        return ModelObjectId.builder().addChild(DefaultL3VpnSvc.class).build();
    }

    /**
     * Returns the model object id for service sites container.
     *
     * @return model object id
     */
    static ModelObjectId getModIdForSites() {
        return ModelObjectId.builder().addChild(DefaultL3VpnSvc.class)
                .addChild(DefaultSites.class).build();
    }

    /**
     * Returns the resource data from the data node and the resource id.
     *
     * @param dataNode data node
     * @param resId    resource id
     * @return resource data
     */
    static ResourceData getResourceData(DataNode dataNode, ResourceId resId) {
        return DefaultResourceData.builder().addDataNode(dataNode)
                .resourceId(resId).build();
    }

    /**
     * Returns the VPN role from the service site role.
     *
     * @param siteRole service site role
     * @return VPN type
     */
    static VpnType getRole(Class<? extends SiteRole> siteRole) {
        switch (siteRole.getSimpleName()) {
            case ANY_TO_ANY_ROLE:
                return ANY_TO_ANY;

            case HUB_ROLE:
                return HUB;

            case SPOKE_ROLE:
                return SPOKE;

            default:
                throw new NetL3VpnException(SITE_ROLE_INVALID);
        }
    }

    /**
     * Returns error message for management ip being unavailable in device.
     *
     * @param ip management ip
     * @return error message
     */
    static String getMgmtIpUnAvailErr(String ip) {
        return "The device with management ip " + ip + " is not available.";
    }

    /**
     * Returns true if device id present in the interface map; false otherwise.
     *
     * @param info interface map
     * @param id   device id
     * @return true if device id available; false otherwise
     */
    private static boolean isDevIdPresent(Map<AccessInfo, InterfaceInfo> info,
                                          String id) {
        for (Map.Entry<AccessInfo, InterfaceInfo> inter : info.entrySet()) {
            InterfaceInfo interfaceInfo = inter.getValue();
            DeviceId devId = interfaceInfo.devInfo().deviceId();
            if (devId.toString().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the device model VPN instance model object data, with respect to
     * the device level.
     *
     * @param id  device id
     * @param ins VPN instance
     * @return model object data, with device level
     */
    private static ModelObjectData buildInsModDataDevice(String id,
                                                         NetworkInstances ins) {
        DeviceKeys devKeys = new DeviceKeys();
        devKeys.deviceid(id);
        ModelObjectId modelId = ModelObjectId.builder()
                .addChild(DefaultDevices.class)
                .addChild(DefaultDevice.class, devKeys)
                .build();
        return DefaultModelObjectData.builder().identifier(modelId)
                .addModelObject((InnerModelObject) ins).build();
    }

    /**
     * Builds the device model VPN instance model object data, with respect to
     * the devices level.
     *
     * @param id  device id
     * @param ins VPN instance
     * @return model object data, with devices level
     */
    private static ModelObjectData buildInsModDataDevices(String id,
                                                          NetworkInstances ins) {
        ModelObjectId modelId = ModelObjectId.builder()
                .addChild(DefaultDevices.class).build();
        Device device = new DefaultDevice();
        device.deviceid(id);
        device.networkInstances(ins);
        return DefaultModelObjectData.builder().identifier(modelId)
                .addModelObject((InnerModelObject) device).build();
    }

    /**
     * Builds the device model VPN instance model object data, with respect to
     * root level.
     *
     * @param id  device id
     * @param ins VPN instance
     * @return model object data, with root level
     */
    private static ModelObjectData buildInsModDataRoot(String id,
                                                       NetworkInstances ins) {
        Devices devices = new DefaultDevices();
        Device device = new DefaultDevice();
        List<Device> deviceList = new LinkedList<>();
        device.deviceid(id);
        device.networkInstances(ins);
        deviceList.add(device);
        devices.device(deviceList);
        return DefaultModelObjectData.builder()
                .addModelObject((InnerModelObject) devices).build();
    }

    /**
     * Builds the device model interface model object data, with respect to
     * device level.
     *
     * @param id  device id
     * @param ifs interface object
     * @return model object data, with device level
     */
    private static ModelObjectData buildIntModDataDevice(String id,
                                                         Interfaces ifs) {
        org.onosproject.yang.gen.v1.ietfinterfaces
                .rev20140508.ietfinterfaces.devices.DeviceKeys keys = new org.
                onosproject.yang.gen.v1.ietfinterfaces.rev20140508
                .ietfinterfaces.devices.DeviceKeys();
        keys.deviceid(id);
        ModelObjectId modelId = ModelObjectId.builder()
                .addChild(org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508
                                  .ietfinterfaces.DefaultDevices.class)
                .addChild(org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508
                                  .ietfinterfaces.devices.DefaultDevice.class,
                          keys)
                .build();
        return DefaultModelObjectData.builder().identifier(modelId)
                .addModelObject((InnerModelObject) ifs).build();
    }

    /**
     * Returns the VPN instance create model object data.
     *
     * @param intMap    interface map
     * @param instances VPN instances
     * @param id        device id
     * @return VPN instance model object data
     */
    static ModelObjectData getVpnCreateModObj(Map<AccessInfo, InterfaceInfo> intMap,
                                              NetworkInstances instances,
                                              String id) {
        ModelObjectData modData;
        boolean devAdded = isDevIdPresent(intMap, id);
        if (devAdded) {
            modData = buildInsModDataDevice(id, instances);
        } else if (intMap.size() != 0) {
            modData = buildInsModDataDevices(id, instances);
        } else {
            modData = buildInsModDataRoot(id, instances);
        }
        return modData;
    }

    /**
     * Returns error message for interface being unavailable in device.
     *
     * @param intName interface name
     * @return error message
     */
    static String getIntNotAvailable(String intName) {
        return "The interface " + intName + " is not available.";
    }

    /**
     * Returns the interface create model object data.
     *
     * @param ifNames interfaces
     * @param ifs     interface instance
     * @param id      device id
     * @return interface model object data
     */
    static ModelObjectData getIntCreateModObj(List<String> ifNames,
                                              Interfaces ifs, String id) {
        ModelObjectData modData;
        if (ifNames.size() > 1) {
            modData = buildIntModDataDevice(id, ifs);
        } else {
            modData = buildIntModDataRoot(id, ifs);
        }
        return modData;
    }

    /**
     * Builds the device model interface model object data, with respect to
     * root level.
     *
     * @param id  device id
     * @param ifs interface object
     * @return model object data, with root level
     */
    private static ModelObjectData buildIntModDataRoot(String id,
                                                       Interfaces ifs) {
        org.onosproject.yang.gen.v1.ietfinterfaces
                .rev20140508.ietfinterfaces.Devices devices = new org
                .onosproject.yang.gen.v1.ietfinterfaces.rev20140508
                .ietfinterfaces.DefaultDevices();
        org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.
                devices.Device device = new org.onosproject.yang.gen.v1.
                ietfinterfaces.rev20140508.ietfinterfaces.devices.DefaultDevice();
        List<org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508
                .ietfinterfaces.devices.Device> deviceList = new LinkedList<>();

        device.deviceid(id);
        device.interfaces(ifs);
        deviceList.add(device);
        devices.device(deviceList);
        return DefaultModelObjectData.builder()
                .addModelObject((InnerModelObject) devices).build();
    }

    /**
     * Returns the BGP create driver info.
     *
     * @param bgpMap BGP map
     * @param id     device id
     * @param devBgp device BGP info
     * @param intBgp interface BGP info
     * @return BGP driver config
     */
    static BgpDriverInfo getBgpCreateConfigObj(Map<BgpInfo, DeviceId> bgpMap,
                                               String id, BgpInfo devBgp,
                                               BgpInfo intBgp) {
        boolean isDevIdPresent = isDevIdBgpPresent(bgpMap, id);
        BgpDriverInfo info;
        if (devBgp != intBgp) {
            //TODO: With ipv6 BGP it has to be changed
            info = new BgpDriverInfo(VPN, id);
        } else if (isDevIdPresent) {
            info = new BgpDriverInfo(DEVICE, id);
        } else if (bgpMap.size() != 0) {
            info = new BgpDriverInfo(DEVICES, id);
        } else {
            info = new BgpDriverInfo(ROOT, id);
        }
        return info;
    }

    /**
     * Returns true if the device is present in the BGP map; false otherwise.
     *
     * @param bgpMap BGP map
     * @param id     device id
     * @return true if device is present; false otherwise
     */
    private static boolean isDevIdBgpPresent(Map<BgpInfo, DeviceId> bgpMap,
                                             String id) {
        for (Map.Entry<BgpInfo, DeviceId> info : bgpMap.entrySet()) {
            DeviceId devId = info.getValue();
            if (devId.toString().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the model object data for VPN instance deletion.
     *
     * @param intMap interface map
     * @param ins    VPN instance
     * @param id     device id
     * @return model object data
     */
    static ModelObjectData getVpnDelModObj(Map<AccessInfo, InterfaceInfo> intMap,
                                           NetworkInstances ins,
                                           String id) {
        boolean isDevIdPresent = isDevIdPresent(intMap, id);
        ModelObjectData modData;
        if (intMap.size() == 0) {
            modData = buildInsModDataRoot(id, ins);
        } else if (isDevIdPresent) {
            modData = buildInsModDataDevice(id, ins);
        } else {
            modData = buildInsModDataDevices(id, ins);
        }
        return modData;
    }

    /**
     * Returns the BGP driver info for VPN BGP instance deletion.
     *
     * @param bgpMap BGP map
     * @param id     device id
     * @return BGP driver info
     */
    static BgpDriverInfo getVpnBgpDelModObj(Map<BgpInfo, DeviceId> bgpMap,
                                            String id) {
        boolean isDevIdPresent = isDevIdBgpPresent(bgpMap, id);
        BgpDriverInfo driInfo;
        if (bgpMap.size() == 0) {
            driInfo = new BgpDriverInfo(ROOT, id);
        } else if (isDevIdPresent) {
            driInfo = new BgpDriverInfo(DEVICE, id);
        } else {
            driInfo = new BgpDriverInfo(DEVICES, id);
        }
        return driInfo;
    }
}
