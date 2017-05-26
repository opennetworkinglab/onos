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

import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.DefaultDevices;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.Device;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.device.Interfaces;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.device.interfaces.DefaultYangAutoPrefixInterface;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.device.interfaces.YangAutoPrefixInterface;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.Ipv4;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.ipv4.Address;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.ipv4.address.subnet.PrefixLength;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.interfaces.yangautoprefixinterface.DefaultAugmentedIfInterface;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.DeviceKeys;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.DefaultL3Vpn;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.DefaultL3Vpncomm;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.DefaultL3VpnInstances;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.DefaultL3VpnInstance;
import org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225.nel3vpnapi.devices.device.l3vpn.l3vpncomm.l3vpninstances.L3VpnInstanceKeys;
import org.onosproject.yang.gen.v1.nel3vpncomm.rev20141225.nel3vpncomm.l3vpnifs.DefaultL3VpnIfs;
import org.onosproject.yang.gen.v1.nel3vpncomm.rev20141225.nel3vpncomm.l3vpnifs.L3VpnIfs;
import org.onosproject.yang.gen.v1.nel3vpncomm.rev20141225.nel3vpncomm.l3vpnifs.l3vpnifs.DefaultL3VpnIf;
import org.onosproject.yang.gen.v1.nel3vpncomm.rev20141225.nel3vpncomm.l3vpnifs.l3vpnifs.L3VpnIf;
import org.onosproject.yang.gen.v1.nel3vpncommtype.rev20141225.nel3vpncommtype.Ipv4Address;
import org.onosproject.yang.model.InnerModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.onosproject.drivers.huawei.DriverUtil.DEVICE_NULL;
import static org.onosproject.drivers.huawei.DriverUtil.OBJECT_NULL;
import static org.onosproject.drivers.huawei.DriverUtil.getData;
import static org.onosproject.drivers.huawei.DriverUtil.getIdFromModId;
import static org.onosproject.drivers.huawei.DriverUtil.getObjFromModData;
import static org.onosproject.yang.gen.v1.nel3vpncommtype.rev20141225.nel3vpncommtype.Ipv4Address.fromString;

/**
 * Representation of utility for interface creation and deletion.
 */
public final class IntConstructionUtil {

    /**
     * Error message for illegal length of mask.
     */
    private static final String ILLEGAL_MASK_LENGTH = "Illegal length of mask" +
            " is not allowed.";

    // No instantiation.
    private IntConstructionUtil() {
    }

    /**
     * Returns the created model object data of VPN bounded interface of huawei
     * device from the standard model object data.
     *
     * @param modObj model object data
     * @return driver interface model object data
     */
    static ModelObjectData getCreateInt(ModelObjectData modObj) {
        boolean isModIdAvail = true;
        String id = getIdFromModId(modObj.identifier(), false);
        Object obj = getObjFromModData(modObj);
        if (id == null) {
            isModIdAvail = false;
            id = getIdFromRootObj(obj);
            obj = getObjFromDevObj(obj);
        }
        if (obj == null) {
            throw new IllegalArgumentException(OBJECT_NULL);
        }
        List<YangAutoPrefixInterface> intList = ((Interfaces) obj)
                .yangAutoPrefixInterface();
        YangAutoPrefixInterface l3Int = intList.get(0);
        DefaultAugmentedIfInterface ifInt =
                ((DefaultYangAutoPrefixInterface) l3Int).augmentation(
                        DefaultAugmentedIfInterface.class);
        String insName = ifInt.bindNetworkInstanceName();
        L3VpnIfs l3VpnIfs = getDriverInterfaces(l3Int);
        return getDriModObj(id, insName, l3VpnIfs, isModIdAvail);
    }

    /**
     * Returns the driver model object data, according to the levels it has
     * to be constructed.
     *
     * @param id           device id
     * @param insName      VPN name
     * @param l3VpnIfs     driver VPN if object
     * @param isModIdAvail model id availability
     * @return driver model object data
     */
    private static ModelObjectData getDriModObj(String id, String insName,
                                                L3VpnIfs l3VpnIfs,
                                                boolean isModIdAvail) {
        List<L3VpnIf> intList = l3VpnIfs.l3VpnIf();
        Iterator<L3VpnIf> it = intList.iterator();
        L3VpnIf l3VpnIf = it.next();
        ModelObjectData data;
        ModelObjectId.Builder objId = getModIdBuilder(id, insName);
        if (isModIdAvail) {
            objId.addChild(DefaultL3VpnIfs.class);
            data = getData(objId.build(), (InnerModelObject) l3VpnIf);
        } else {
            data = getData(objId.build(), (InnerModelObject) l3VpnIfs);
        }
        return data;
    }

    private static ModelObjectId.Builder getModIdBuilder(String id,
                                                         String vpnName) {
        DeviceKeys key = new DeviceKeys();
        key.deviceid(id);
        L3VpnInstanceKeys insKey = new L3VpnInstanceKeys();
        insKey.vrfName(vpnName);
        return ModelObjectId.builder()
                .addChild(org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225
                                  .nel3vpnapi.DefaultDevices.class)
                .addChild(org.onosproject.yang.gen.v1.nel3vpnapi.rev20141225
                                  .nel3vpnapi.devices.DefaultDevice.class, key)
                .addChild(DefaultL3Vpn.class)
                .addChild(DefaultL3Vpncomm.class)
                .addChild(DefaultL3VpnInstances.class)
                .addChild(DefaultL3VpnInstance.class, insKey);
    }

    /**
     * Returns the driver interfaces from the standard device model interfaces.
     *
     * @param ifs standard device model interfaces
     * @return driver interfaces
     */
    private static L3VpnIfs getDriverInterfaces(YangAutoPrefixInterface ifs) {
        L3VpnIfs l3VpnIfs = new DefaultL3VpnIfs();
        L3VpnIf l3VpnIf = new DefaultL3VpnIf();
        List<L3VpnIf> l3VpnIfList = new LinkedList<>();
        l3VpnIf.ifName(ifs.name());
        org.onosproject.yang.gen.v1.ietfip
                .rev20140616.ietfip.devices.device.interfaces
                .yangautoprefixinterface.DefaultAugmentedIfInterface ipAug =
                ((DefaultYangAutoPrefixInterface) ifs).augmentation(
                        org.onosproject.yang.gen.v1.ietfip.rev20140616
                                .ietfip.devices.device.interfaces
                                .yangautoprefixinterface
                                .DefaultAugmentedIfInterface.class);

        if (ipAug != null && ipAug.ipv4() != null) {
            Ipv4 ipAddress = ipAug.ipv4();
            for (Address add : ipAddress.address()) {
                Ipv4Address v4Add = fromString(add.ip().ipv4Address()
                                                       .toString());
                Ipv4Address subnet = fromString(getSubnet(
                        ((PrefixLength) add.subnet()).prefixLength()));
                l3VpnIf.ipv4Addr(v4Add);
                l3VpnIf.subnetMask(subnet);
            }
        }
        l3VpnIfList.add(l3VpnIf);
        l3VpnIfs.l3VpnIf(l3VpnIfList);
        return l3VpnIfs;
    }

    /**
     * Returns the device id from the root object.
     *
     * @param obj root object
     * @return device id
     */
    private static String getIdFromRootObj(Object obj) {
        Device dev = getDevFromRootObj(obj);
        return dev.deviceid();
    }

    /**
     * Returns the device from the root object.
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
     * Returns the interfaces object from the root object.
     *
     * @param obj root object
     * @return interfaces object
     */
    private static Interfaces getObjFromDevObj(Object obj) {
        Device dev = getDevFromRootObj(obj);
        return dev.interfaces();
    }

    /**
     * Returns the subnet address from the mask value.
     *
     * @param mask mask value
     * @return subnet address
     */
    private static String getSubnet(short mask) {
        int value = 0xffffffff << (32 - mask);
        byte[] bytes = new byte[]{
                (byte) (value >>> 24), (byte) (value >> 16 & 0xff),
                (byte) (value >> 8 & 0xff), (byte) (value & 0xff)};
        InetAddress netAdd;
        try {
            netAdd = InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(ILLEGAL_MASK_LENGTH);
        }
        return netAdd.getHostAddress();
    }
}
