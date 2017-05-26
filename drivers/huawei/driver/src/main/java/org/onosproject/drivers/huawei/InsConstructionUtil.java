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

import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.devices.device.networkinstances.networkinstance.DefaultAugmentedNiNetworkInstance;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.l3vpnvrfparams.ipv4.Unicast;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetset.Rts;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetset.rts.RtTypeEnum;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.DefaultDevices;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.Device;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.NetworkInstances;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.networkinstances.DefaultNetworkInstance;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.networkinstances.NetworkInstance;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.DefaultDevice;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.DeviceKeys;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.DefaultL3Vpn;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.L3Vpn;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.DefaultL3Vpncomm;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.L3Vpncomm;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.DefaultL3VpnInstances;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.L3VpnInstances;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.DefaultL3VpnInstance;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.L3VpnInstance;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.L3VpnInstanceKeys;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.DefaultVpnInstAfs;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.VpnInstAfs;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.vpninstafs.DefaultVpnInstAf;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.vpninstafs.VpnInstAf;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.vpninstafs.vpninstaf.DefaultVpnTargets;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.vpninstafs.vpninstaf.VpnTargets;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.vpninstafs.vpninstaf.vpntargets.DefaultVpnTarget;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.l3vpninstance.vpninstafs.vpninstaf.vpntargets.VpnTarget;
import org.onosproject.yang.gen.v1.nel3vpncomm.rev20141225.nel3vpncomm.l3vpnifs.DefaultL3VpnIfs;
import org.onosproject.yang.model.InnerModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.onosproject.drivers.huawei.DriverUtil.CONS_DEVICES;
import static org.onosproject.drivers.huawei.DriverUtil.DEVICE_NULL;
import static org.onosproject.drivers.huawei.DriverUtil.INS_NULL;
import static org.onosproject.drivers.huawei.DriverUtil.OBJECT_NULL;
import static org.onosproject.drivers.huawei.DriverUtil.UNSUPPORTED_MODEL_LVL;
import static org.onosproject.drivers.huawei.DriverUtil.getData;
import static org.onosproject.drivers.huawei.DriverUtil.getIdFromModId;
import static org.onosproject.drivers.huawei.DriverUtil.getModObjIdDriDevices;
import static org.onosproject.drivers.huawei.DriverUtil.getObjFromModData;
import static org.onosproject.drivers.huawei.ModelIdLevel.DEVICE;
import static org.onosproject.drivers.huawei.ModelIdLevel.DEVICES;
import static org.onosproject.drivers.huawei.ModelIdLevel.ROOT;
import static org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetset.rts.RtTypeEnum.BOTH;
import static org.onosproject.yang.gen.v1.nel3vpncommtype.rev20141225.nel3vpncommtype.L3VpncommonL3VpnPrefixType.of;
import static org.onosproject.yang.gen.v1.nel3vpncommtype.rev20141225.nel3vpncommtype.L3VpncommonVrfRtType.fromString;
import static org.onosproject.yang.gen.v1.nel3vpncommtype.rev20141225.nel3vpncommtype.l3vpncommonl3vpnprefixtype.L3VpncommonL3VpnPrefixTypeEnum.IPV4UNI;

/**
 * Representation of utility for instance creation and deletion.
 */
public final class InsConstructionUtil {

    /**
     * Static constant for route target export.
     */
    private static final String EXP_COMM = "export_extcommunity";

    /**
     * Static constant for route target import.
     */
    private static final String IMP_COMM = "import_extcommunity";

    /**
     * Error message for unsupported RT type.
     */
    private static final String UNSUPPORTED_RT_TYPE = "The RT type is not " +
            "supported";

    // No instantiation.
    private InsConstructionUtil() {
    }

    /**
     * Returns the created model object data of VPN instance of huawei device
     * from the standard model object data.
     *
     * @param modObj     model object data
     * @param isDevAvail if devices available
     * @return driver model object data
     */
    static ModelObjectData getCreateVpnIns(ModelObjectData modObj,
                                           boolean isDevAvail) {
        ModelIdLevel modIdLvl = DEVICE;
        String id = getIdFromModId(modObj.identifier(), true);
        Object obj = getObjFromModData(modObj);

        if (obj == null) {
            throw new IllegalArgumentException(OBJECT_NULL);
        }

        if (id == null) {
            id = getDevIdFromRootObj(obj);
            obj = getObjFromRootObj(obj);
            if (isDevAvail) {
                modIdLvl = DEVICES;
            } else {
                modIdLvl = ROOT;
            }
        } else if (id.equals(CONS_DEVICES)) {
            modIdLvl = DEVICES;
            id = ((Device) obj).deviceid();
            obj = ((Device) obj).networkInstances();
        }
        org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi
                .DefaultDevices devices = getDriverDevices(
                id, (NetworkInstances) obj);
        return getCreateModObjData(modIdLvl, id, devices);
    }

    /**
     * Returns the driver model object data, according to the levels it has
     * to be constructed.
     *
     * @param modIdLvl model id level
     * @param devId    device id
     * @param devices  devices object
     * @return model object data
     */
    private static ModelObjectData getCreateModObjData(ModelIdLevel modIdLvl,
                                                       String devId,
                                                       org.onosproject.yang.gen
                                                               .v1.nel3vpnapi
                                                               .rev20141225
                                                               .nel3vpnapi
                                                               .DefaultDevices
                                                               devices) {
        ModelObjectId id;
        List<org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi
                .devices.Device> devList = devices.device();
        Iterator<org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi
                .devices.Device> it = devList.iterator();
        org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi
                .devices.Device device = it.next();
        Iterator<L3VpnInstance> instIt = device.l3Vpn().l3Vpncomm()
                .l3VpnInstances().l3VpnInstance().iterator();
        L3VpnInstance ins = instIt.next();

        switch (modIdLvl) {

            case ROOT:
                return getData(null, devices);

            case DEVICES:
                id = getModObjIdDriDevices();
                return getData(id, (InnerModelObject) device);

            case DEVICE:
                id = getModelObjIdForIns(devId).build();
                return getData(id, (InnerModelObject) ins);

            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODEL_LVL);
        }
    }

    /**
     * Returns the devices object of the huawei VPN instance model. This
     * constructs all the required information in the device for L3VPN
     * instance creation.
     *
     * @param id  device id
     * @param obj network instances object
     * @return driver VPN instance's devices
     */
    private static org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225
            .nel3vpnapi.DefaultDevices getDriverDevices(String id,
                                                        NetworkInstances obj) {

        org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.
                DefaultDevices devices = new org.onosproject.yang.gen.v1
                .nel3vpnapi.rev20141225.nel3vpnapi.DefaultDevices();
        org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices
                .Device device = new org.onosproject.yang.gen.v1.nel3vpnapi
                .rev20141225.nel3vpnapi.devices.DefaultDevice();

        L3Vpn l3Vpn = new DefaultL3Vpn();
        L3Vpncomm l3VpnComm = new DefaultL3Vpncomm();
        L3VpnInstances instances = new DefaultL3VpnInstances();
        L3VpnInstance ins = new DefaultL3VpnInstance();
        List<L3VpnInstance> insList = new LinkedList<>();
        List<org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi
                .devices.Device> devList = new LinkedList<>();

        createDriIns(obj, ins);
        insList.add(ins);
        instances.l3VpnInstance(insList);
        l3VpnComm.l3VpnInstances(instances);
        l3Vpn.l3Vpncomm(l3VpnComm);

        device.deviceid(id);
        device.l3Vpn(l3Vpn);
        devList.add(device);
        devices.device(devList);
        return devices;
    }

    /**
     * Creates driver instance value from standard device instance.
     *
     * @param ins    standard device instance
     * @param driIns driver instance
     */
    private static void createDriIns(NetworkInstances ins,
                                     L3VpnInstance driIns) {
        NetworkInstance networkInstance = ins.networkInstance().iterator()
                .next();
        driIns.vrfName(networkInstance.name());
        DefaultAugmentedNiNetworkInstance augIns =
                ((DefaultNetworkInstance) networkInstance).augmentation(
                        DefaultAugmentedNiNetworkInstance.class);
        VpnInstAfs vpnInstAfs = processL3VpnAf(augIns.l3Vpn());
        driIns.vpnInstAfs(vpnInstAfs);
    }

    /**
     * Returns the device id from the root object.
     *
     * @param obj root object
     * @return device id
     */
    private static String getDevIdFromRootObj(Object obj) {
        Device dev = getDevFromRootObj(obj);
        return dev.deviceid();
    }

    /**
     * Returns the first device from the root object. If no device is
     * present, it returns null.
     *
     * @param obj root object
     * @return device object
     */
    private static Device getDevFromRootObj(Object obj) {
        List<Device> deviceList = ((DefaultDevices) obj).device();
        Iterator<Device> it = deviceList.iterator();
        if (it.hasNext()) {
            return it.next();
        }
        throw new IllegalArgumentException(DEVICE_NULL);
    }

    /**
     * Returns the network instances object from the root object.
     *
     * @param obj root object
     * @return network instances
     */
    private static NetworkInstances getObjFromRootObj(Object obj) {
        Device dev = getDevFromRootObj(obj);
        return dev.networkInstances();
    }

    /**
     * Returns model object id builder that has to be constructed for driver
     * instance level addition, with the device id.
     *
     * @param id device id
     * @return model object id
     */
    private static ModelObjectId.Builder getModelObjIdForIns(String id) {
        ModelObjectId.Builder device = getModObjIdDriDevice(id);
        return device.addChild(DefaultL3Vpn.class)
                .addChild(DefaultL3Vpncomm.class)
                .addChild(DefaultL3VpnInstances.class);
    }

    /**
     * Processes standard device model L3VPN address family and returns the
     * driver L3VPN address family.
     *
     * @param l3Vpn standard device L3VPN
     * @return driver address family
     */
    private static VpnInstAfs processL3VpnAf(org.onosproject.yang.gen.v1
                                                     .ietfbgpl3vpn.rev20160909
                                                     .ietfbgpl3vpn.devices
                                                     .device.networkinstances
                                                     .networkinstance
                                                     .augmentedninetworkinstance
                                                     .L3Vpn l3Vpn) {
        // TODO: Need to handle the ipv6 case
        Unicast ipv4Unicast = l3Vpn.ipv4().unicast();
        VpnInstAfs vpnInstAfs = new DefaultVpnInstAfs();
        VpnInstAf vpnInstAf = new DefaultVpnInstAf();
        VpnTargets vpnTargets = new DefaultVpnTargets();
        List<VpnInstAf> afList = new LinkedList<>();

        vpnInstAf.vrfRd(l3Vpn.routeDistinguisher().config().rd());
        vpnInstAf.afType(of(IPV4UNI));

        List<Rts> rts = ipv4Unicast.routeTargets().config().rts();
        addVpnTarget(vpnTargets, rts);

        vpnInstAf.vpnTargets(vpnTargets);
        afList.add(vpnInstAf);
        vpnInstAfs.vpnInstAf(afList);
        return vpnInstAfs;
    }

    /**
     * Adds VPN target to the target list from the list of RTs available in
     * the standard device model.
     *
     * @param vpnTgts VPN targets
     * @param rts     rts
     */
    private static void addVpnTarget(VpnTargets vpnTgts, List<Rts> rts) {
        List<VpnTarget> tgtList = new LinkedList<>();
        for (Rts rt : rts) {
            if (rt == null) {
                continue;
            }
            if (rt.rtType() == BOTH) {
                VpnTarget expTgt = addRt(rt.rt(), EXP_COMM);
                VpnTarget impTgt = addRt(rt.rt(), IMP_COMM);
                tgtList.add(expTgt);
                tgtList.add(impTgt);
            } else {
                String rtType = getRtVal(rt.rtType());
                VpnTarget tgt = addRt(rt.rt(), rtType);
                tgtList.add(tgt);
            }
        }
        vpnTgts.vpnTarget(tgtList);
    }

    /**
     * Returns the RT value according to the RT type available.
     *
     * @param type RT type
     * @return RT value
     */
    private static String getRtVal(RtTypeEnum type) {
        switch (type) {

            case EXPORT:
                return EXP_COMM;

            case IMPORT:
                return IMP_COMM;

            default:
                throw new IllegalArgumentException(UNSUPPORTED_RT_TYPE);
        }
    }

    /**
     * Adds RT to the VPN target with the RT value and RT type.
     *
     * @param rt     RT value
     * @param rtType RT type
     * @return VPN target
     */
    private static VpnTarget addRt(String rt, String rtType) {
        VpnTarget vpnTarget = new DefaultVpnTarget();
        vpnTarget.vrfRtvalue(rt);
        vpnTarget.vrfRttype(fromString(rtType));
        return vpnTarget;
    }

    /**
     * Returns the deletable model object data of VPN instance of huawei device
     * from the standard model object data.
     *
     * @param modObj model object data
     * @return driver model object data
     */
    static Object getDeleteVpnIns(ModelObjectData modObj) {
        ModelIdLevel modIdLvl = DEVICE;
        String id = getIdFromModId(modObj.identifier(), true);
        Object obj = getObjFromModData(modObj);

        if (obj == null) {
            throw new IllegalArgumentException(OBJECT_NULL);
        }

        if (id == null) {
            modIdLvl = ROOT;
            id = getDevIdFromRootObj(obj);
            obj = getObjFromRootObj(obj);
        } else if (id.equals(CONS_DEVICES)) {
            modIdLvl = DEVICES;
            id = ((Device) obj).deviceid();
            obj = ((Device) obj).networkInstances();
        }
        List<NetworkInstance> ins = ((NetworkInstances) obj).networkInstance();
        Iterator<NetworkInstance> it = ins.iterator();
        NetworkInstance instance;
        if (it.hasNext()) {
            instance = it.next();
        } else {
            throw new IllegalArgumentException(INS_NULL);
        }
        return getDelModObjData(modIdLvl, id, instance.name());
    }

    /**
     * Returns the driver model object data for delete, according to the
     * levels it has to be constructed.
     *
     * @param modIdLvl model id level
     * @param id       device id
     * @param name     VPN name
     * @return driver model object data
     */
    private static ModelObjectData getDelModObjData(ModelIdLevel modIdLvl,
                                                    String id, String name) {
        ModelObjectId modId;
        switch (modIdLvl) {
            case ROOT:
                modId = getModObjIdDriDevices();
                DefaultDevice device = new DefaultDevice();
                return getData(modId, device);

            case DEVICES:
                DefaultL3Vpn l3Vpn = new DefaultL3Vpn();
                modId = getModObjIdDriDevice(id).build();
                return getData(modId, l3Vpn);

            case DEVICE:
                DefaultL3VpnIfs l3VpnIfs = new DefaultL3VpnIfs();
                modId = getModObjIdDriVpn(id, name);
                return getData(modId, l3VpnIfs);

            default:
                throw new IllegalArgumentException(UNSUPPORTED_MODEL_LVL);
        }
    }

    /**
     * Returns the model object id, with device id and VPN name.
     *
     * @param id   device id
     * @param name VPN name
     * @return model object id
     */
    private static ModelObjectId getModObjIdDriVpn(String id, String name) {
        ModelObjectId.Builder ins = getModelObjIdForIns(id);
        L3VpnInstanceKeys key = new L3VpnInstanceKeys();
        key.vrfName(name);
        return ins.addChild(DefaultL3VpnInstance.class, key).build();
    }

    /**
     * Returns the model object id builder of the driver with respect to device.
     *
     * @param id device id
     * @return model object id builder
     */
    private static ModelObjectId.Builder getModObjIdDriDevice(String id) {
        DeviceKeys key = new DeviceKeys();
        key.deviceid(id);
        return ModelObjectId.builder()
                .addChild(org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225
                                  .nel3vpnapi.DefaultDevices.class)
                .addChild(DefaultDevice.class, key);
    }
}
