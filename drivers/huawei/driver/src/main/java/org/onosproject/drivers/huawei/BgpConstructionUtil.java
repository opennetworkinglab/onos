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

package org.onosproject.drivers.huawei;

import org.onosproject.l3vpn.netl3vpn.BgpDriverInfo;
import org.onosproject.l3vpn.netl3vpn.BgpInfo;
import org.onosproject.l3vpn.netl3vpn.BgpModelIdLevel;
import org.onosproject.l3vpn.netl3vpn.ProtocolInfo;
import org.onosproject.l3vpn.netl3vpn.RouteProtocol;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.DefaultDevices;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.Devices;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.DefaultDevice;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.Device;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.DeviceKeys;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.Bgp;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.DefaultBgp;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.Bgpcomm;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.DefaultBgpcomm;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.BgpVrfs;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.DefaultBgpVrfs;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.BgpVrf;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.BgpVrfKeys;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.DefaultBgpVrf;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.bgpvrf.BgpVrfAfs;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.bgpvrf.DefaultBgpVrfAfs;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.BgpVrfAf;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.BgpVrfAfKeys;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.DefaultBgpVrfAf;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf.DefaultImportRoutes;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf.ImportRoutes;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf.importroutes.DefaultImportRoute;
import org.onosproject.yang.gen.v1.nebgpcomm.rev20141225.nebgpcomm.devices.device.bgp.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf.importroutes.ImportRoute;
import org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.nebgpcommtype.BgpcommImRouteProtocol;
import org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.nebgpcommtype.bgpcommimrouteprotocol.BgpcommImRouteProtocolEnum;
import org.onosproject.yang.model.InnerModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.onosproject.drivers.huawei.DriverUtil.UNSUPPORTED_MODEL_LVL;
import static org.onosproject.drivers.huawei.DriverUtil.getData;
import static org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.nebgpcommtype.BgpcommPrefixType.of;
import static org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.nebgpcommtype.bgpcommimrouteprotocol.BgpcommImRouteProtocolEnum.DIRECT;
import static org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.nebgpcommtype.bgpcommimrouteprotocol.BgpcommImRouteProtocolEnum.OSPF;
import static org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.nebgpcommtype.bgpcommimrouteprotocol.BgpcommImRouteProtocolEnum.RIP;
import static org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.nebgpcommtype.bgpcommimrouteprotocol.BgpcommImRouteProtocolEnum.RIPNG;
import static org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.nebgpcommtype.bgpcommimrouteprotocol.BgpcommImRouteProtocolEnum.STATIC;
import static org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.nebgpcommtype.bgpcommprefixtype.BgpcommPrefixTypeEnum.IPV4UNI;
import static org.onosproject.yang.gen.v1.nebgpcommtype.rev20141225.nebgpcommtype.bgpcommprefixtype.BgpcommPrefixTypeEnum.IPV6UNI;

/**
 * Representation of utility for BGP creation and deletion.
 */
public final class BgpConstructionUtil {

    /**
     * Error message for unsupported protocol type.
     */
    private static final String UNSUPPORTED_PRO_TYPE = "Unsupported route " +
            "protocol type is found.";

    // No instantiation.
    private BgpConstructionUtil() {
    }

    /**
     * Returns the created model object data of BGP info of huawei device
     * from the standard model object data.
     *
     * @param bgpInfo device BGP info
     * @param config  BGP driver info
     * @return driver model object data
     */
    static ModelObjectData getCreateBgp(BgpInfo bgpInfo,
                                        BgpDriverInfo config) {
        String devId = config.devId();
        BgpModelIdLevel modIdLevel = config.modIdLevel();

        Bgp bgp = new DefaultBgp();
        Bgpcomm bgpBuilder = new DefaultBgpcomm();
        BgpVrfs bgpVrfs = new DefaultBgpVrfs();
        BgpVrf bgpVrf = new DefaultBgpVrf();
        BgpVrfAfs bgpVrfAfs = new DefaultBgpVrfAfs();
        List<BgpVrf> bgpVrfList = new LinkedList<>();

        bgpVrf.vrfName(bgpInfo.vpnName());
        Map<RouteProtocol, ProtocolInfo> proMap = bgpInfo.protocolInfo();
        ImportRoute route = addRouteProtocol(proMap, bgpVrfAfs);

        bgpVrf.bgpVrfAfs(bgpVrfAfs);
        bgpVrfList.add(bgpVrf);
        bgpVrfs.bgpVrf(bgpVrfList);
        bgpBuilder.bgpVrfs(bgpVrfs);
        bgp.bgpcomm(bgpBuilder);
        return getModObjData(modIdLevel, bgp, devId, bgpVrf, route);
    }

    /**
     * Adds route protocol from the standard device model to the BGP address
     * family respectively.
     * //TODO: IPV6 has to be handled.
     *
     * @param proMap    protocol map
     * @param bgpVrfAfs BGP address family
     * @return import route
     */
    private static ImportRoute addRouteProtocol(Map<RouteProtocol, ProtocolInfo> proMap,
                                                BgpVrfAfs bgpVrfAfs) {
        BgpVrfAf ipv4 = new DefaultBgpVrfAf();
        ImportRoutes ipv4Routes = new DefaultImportRoutes();
        ipv4.afType(of(IPV4UNI));

        ImportRoute route = null;

        BgpVrfAf ipv6 = new DefaultBgpVrfAf();
        ImportRoutes ipv6Routes = new DefaultImportRoutes();
        ipv6.afType(of(IPV6UNI));
        for (Map.Entry<RouteProtocol, ProtocolInfo> info : proMap.entrySet()) {
            RouteProtocol protocol = info.getKey();
            ProtocolInfo proInfo = info.getValue();
            if (proInfo.isIpv4Af()) {
                route = addImportRoute(ipv4Routes, proInfo, protocol);
            }
            if (proInfo.isIpv6Af()) {
                addImportRoute(ipv6Routes, proInfo, protocol);
            }
        }
        if (ipv4Routes.importRoute() != null &&
                !ipv4Routes.importRoute().isEmpty()) {
            addToBgpVrf(ipv4Routes, ipv4, bgpVrfAfs);
        }
        if (ipv6Routes.importRoute() != null &&
                !ipv6Routes.importRoute().isEmpty()) {
            addToBgpVrf(ipv6Routes, ipv6, bgpVrfAfs);
        }
        return route;
    }

    /**
     * Adds the routes to BGP VRF in driver model.
     *
     * @param routes    routes
     * @param vrfAf     VRF address family
     * @param bgpVrfAfs BGP address family
     */
    private static void addToBgpVrf(ImportRoutes routes, BgpVrfAf vrfAf,
                                    BgpVrfAfs bgpVrfAfs) {
        List<BgpVrfAf> ipList = new LinkedList<>();
        vrfAf.importRoutes(routes);
        ipList.add(vrfAf);
        bgpVrfAfs.bgpVrfAf(ipList);
    }

    /**
     * Adds the import route to the routes, according to the protocol info
     * from the standard device model.
     *
     * @param routes   routes object
     * @param proInfo  protocol info
     * @param protocol route protocol
     * @return import route object
     */
    private static ImportRoute addImportRoute(ImportRoutes routes,
                                              ProtocolInfo proInfo,
                                              RouteProtocol protocol) {
        List<ImportRoute> routeList = new LinkedList<>();
        ImportRoute route = buildAfBgp(proInfo, protocol);
        routeList.add(route);
        routes.importRoute(routeList);
        return route;
    }

    /**
     * Builds the import route details from the route protocol and the
     * process id.
     *
     * @param proInfo  protocol info
     * @param protocol route protocol
     * @return import route object
     */
    private static ImportRoute buildAfBgp(ProtocolInfo proInfo,
                                          RouteProtocol protocol) {
        BgpcommImRouteProtocolEnum rpEnum = getProtocolType(protocol);
        ImportRoute impRoute = new DefaultImportRoute();
        impRoute.importProcessId(proInfo.processId());
        impRoute.importProtocol(BgpcommImRouteProtocol.of(rpEnum));
        return impRoute;
    }

    /**
     * Returns the huawei route protocol corresponding to standard device
     * route protocol.
     *
     * @param protocol device route protocol
     * @return driver route protocol
     */
    private static BgpcommImRouteProtocolEnum getProtocolType(RouteProtocol protocol) {
        switch (protocol) {
            case DIRECT:
                return DIRECT;

            case OSPF:
                return OSPF;

            case RIP:
                return RIP;

            case RIP_NG:
                return RIPNG;

            case STATIC:
                return STATIC;

            case BGP:
            case VRRP:
            default:
                throw new IllegalArgumentException(UNSUPPORTED_PRO_TYPE);
        }
    }

    /**
     * Returns the driver model object data, according to the levels it has
     * to be constructed.
     *
     * @param modIdLevel model id level
     * @param bgp        driver BGP object
     * @param devId      device id
     * @param bgpVrf     driver BGP VRF object
     * @param route      import route object
     * @return model object data
     */
    public static ModelObjectData getModObjData(BgpModelIdLevel modIdLevel,
                                                Bgp bgp, String devId,
                                                BgpVrf bgpVrf, ImportRoute route) {
        switch (modIdLevel) {

            case VPN:
                return getRouteProModObj(bgpVrf.vrfName(), route, devId);

            case ROOT:
                return getRootModObj(bgp, devId);

            case DEVICES:
                return getDevicesModObj(bgp, devId);

            case DEVICE:
                return getDevModObj(bgpVrf, devId);

            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODEL_LVL);
        }
    }

    /**
     * Returns the model object data of route protocol.
     *
     * @param vpnName VPN name
     * @param route   import route
     * @param devId   device id
     * @return model object data
     */
    private static ModelObjectData getRouteProModObj(String vpnName,
                                                     ImportRoute route,
                                                     String devId) {
        DeviceKeys key = new DeviceKeys();
        key.deviceid(devId);
        BgpVrfKeys vpnKey = new BgpVrfKeys();
        vpnKey.vrfName(vpnName);
        BgpVrfAfKeys afKey = new BgpVrfAfKeys();
        afKey.afType(of(IPV4UNI));
        ModelObjectId id = ModelObjectId.builder()
                .addChild(DefaultDevices.class)
                .addChild(DefaultDevice.class, key)
                .addChild(DefaultBgp.class)
                .addChild(DefaultBgpcomm.class)
                .addChild(DefaultBgpVrfs.class)
                .addChild(DefaultBgpVrf.class, vpnKey)
                .addChild(DefaultBgpVrfAfs.class)
                .addChild(DefaultBgpVrfAf.class, afKey)
                .addChild(DefaultImportRoutes.class).build();
        return getData(id, (InnerModelObject) route);
    }

    /**
     * Returns the driver model object data with device in model object id,
     * till BGP VRF.
     *
     * @param bgpVrf BGP VRF object
     * @param devId  device id
     * @return model object data
     */
    private static ModelObjectData getDevModObj(BgpVrf bgpVrf, String devId) {
        DeviceKeys key = new DeviceKeys();
        key.deviceid(devId);
        ModelObjectId id = ModelObjectId.builder()
                .addChild(DefaultDevices.class)
                .addChild(DefaultDevice.class, key)
                .addChild(DefaultBgp.class)
                .addChild(DefaultBgpcomm.class)
                .addChild(DefaultBgpVrfs.class).build();
        return getData(id, (InnerModelObject) bgpVrf);
    }

    /**
     * Returns the driver model object data with devices in model object id.
     *
     * @param bgp   BGP object
     * @param devId device id
     * @return model object data
     */
    private static ModelObjectData getDevicesModObj(Bgp bgp, String devId) {
        ModelObjectId modelId = ModelObjectId.builder()
                .addChild(DefaultDevices.class).build();
        Device device = getDevInfo(bgp, devId);
        return getData(modelId, (InnerModelObject) device);
    }

    /**
     * Returns the driver root model object without model object id.
     *
     * @param bgp   driver BGP
     * @param devId device id
     * @return model object data
     */
    private static ModelObjectData getRootModObj(Bgp bgp, String devId) {
        Devices devices = new DefaultDevices();
        List<Device> devList = new LinkedList<>();
        Device device = getDevInfo(bgp, devId);
        devList.add(device);
        devices.device(devList);
        return getData(null, (InnerModelObject) devices);
    }

    /**
     * Returns the driver BGP from the device object.
     *
     * @param bgp   BGP object
     * @param devId device id
     * @return device object
     */
    private static Device getDevInfo(Bgp bgp, String devId) {
        Device device = new DefaultDevice();
        device.deviceid(devId);
        device.bgp(bgp);
        return device;
    }

    /**
     * Returns the BGP model object data for deletion.
     *
     * @param bgpInfo   BGP info
     * @param bgpConfig BGP driver config
     * @return model object data
     */
    static ModelObjectData getDeleteBgp(BgpInfo bgpInfo,
                                        BgpDriverInfo bgpConfig) {
        BgpModelIdLevel modIdLvl = bgpConfig.modIdLevel();
        switch (modIdLvl) {
            case ROOT:
                return getDelRootModObj();

            case DEVICES:
                return getDelDevicesModObj(bgpConfig.devId());

            case DEVICE:
                return getDelDeviceModObj(bgpConfig.devId(), bgpInfo.vpnName());

            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODEL_LVL);
        }
    }

    /**
     * Returns the model object data with respect to device level.
     *
     * @param devId   device id
     * @param vpnName VPN name
     * @return model object data
     */
    private static ModelObjectData getDelDeviceModObj(String devId,
                                                      String vpnName) {
        DeviceKeys devKeys = new DeviceKeys();
        devKeys.deviceid(devId);
        BgpVrfKeys vpnKey = new BgpVrfKeys();
        vpnKey.vrfName(vpnName);
        ModelObjectId modelId = ModelObjectId.builder()
                .addChild(DefaultDevices.class)
                .addChild(DefaultDevice.class, devKeys)
                .addChild(DefaultBgp.class)
                .addChild(DefaultBgpcomm.class)
                .addChild(DefaultBgpVrfs.class)
                .addChild(DefaultBgpVrf.class, vpnKey).build();
        DefaultBgpVrfAfs afs = new DefaultBgpVrfAfs();
        return getData(modelId, afs);
    }

    /**
     * Returns the model object data with respect to root level.
     *
     * @return model object data
     */
    private static ModelObjectData getDelRootModObj() {
        DefaultDevice device = new DefaultDevice();
        ModelObjectId modelId = ModelObjectId.builder().addChild(
                DefaultDevices.class).build();
        return getData(modelId, device);
    }

    /**
     * Returns the model object data for devices level.
     *
     * @param id device id
     * @return model object data
     */
    private static ModelObjectData getDelDevicesModObj(String id) {
        DeviceKeys devKeys = new DeviceKeys();
        devKeys.deviceid(id);
        ModelObjectId modelId = ModelObjectId.builder()
                .addChild(DefaultDevices.class)
                .addChild(DefaultDevice.class, devKeys).build();
        DefaultBgp bgp = new DefaultBgp();
        return getData(modelId, bgp);
    }
}
