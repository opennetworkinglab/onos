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

import org.onosproject.l3vpn.netl3vpn.NetL3VpnException;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.ietfinettypes.Ipv4Address;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.ietfinettypes.Ipv4AddressNoZone;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.ietfinettypes.Ipv6Address;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.ietfinettypes.Ipv6AddressNoZone;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.device.DefaultInterfaces;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.device.Interfaces;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.device.interfaces.DefaultYangAutoPrefixInterface;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.ietfinterfaces.devices.device.interfaces.YangAutoPrefixInterface;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.AugmentedIfInterface;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.DefaultAugmentedIfInterface;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.DefaultIpv4;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.DefaultIpv6;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.Ipv4;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.Ipv6;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.ipv4.Address;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.ipv4.DefaultAddress;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.ipv4.address.Subnet;
import org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device.interfaces.yangautoprefixinterface.augmentedifinterface.ipv4.address.subnet.DefaultPrefixLength;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siteattachmentipconnection.IpConnection;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.interfaces.yangautoprefixinterface.ipv4.AugmentedIpIpv4;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.interfaces.yangautoprefixinterface.ipv4.DefaultAugmentedIpIpv4;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.interfaces.yangautoprefixinterface.ipv6.AugmentedIpIpv6;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.interfaces.yangautoprefixinterface.ipv6.DefaultAugmentedIpIpv6;
import org.onosproject.yang.model.InnerModelObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Representation of utility for interface creation and deletion.
 */
public final class IntConstructionUtil {

    private static final String IP_ADD_NULL = "Vpn binding to an interface " +
            "requires ip address.";

    // No instantiation.
    private IntConstructionUtil() {
    }

    /**
     * Creates device model interface by building its parameters with port
     * name, VPN name and ip connection.
     *
     * @param pName   port name
     * @param vpnName VPN name
     * @param connect ip connection
     * @return interface device model
     */
    public static Interfaces createInterface(String pName, String vpnName,
                                             IpConnection connect) {
        Interfaces interfaces = new DefaultInterfaces();
        List<YangAutoPrefixInterface> intList = new LinkedList<>();
        YangAutoPrefixInterface inter = buildInterface(vpnName, pName, connect);
        intList.add(inter);
        interfaces.yangAutoPrefixInterface(intList);
        return interfaces;
    }

    /**
     * Builds augmented info of ip address to the interface.
     *
     * @param vpnName VPN name
     * @param pName   port name
     * @param connect ip connection
     * @return interface
     */
    private static YangAutoPrefixInterface buildInterface(String vpnName,
                                                          String pName,
                                                          IpConnection connect) {
        // Bind vpn name in the augmented info of interface.
        org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623
                .ietfnetworkinstance.devices.device.interfaces
                .yangautoprefixinterface.AugmentedIfInterface augIf = new org
                .onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623
                .ietfnetworkinstance.devices.device.interfaces
                .yangautoprefixinterface.DefaultAugmentedIfInterface();
        augIf.bindNetworkInstanceName(vpnName);

        // Bind ip address to the interface as augmented info.
        AugmentedIfInterface intAug = buildIpAddress(connect, vpnName);
        YangAutoPrefixInterface inter = new DefaultYangAutoPrefixInterface();
        inter.name(pName);
        ((DefaultYangAutoPrefixInterface) inter).addAugmentation(
                (InnerModelObject) augIf);
        ((DefaultYangAutoPrefixInterface) inter).addAugmentation(
                (InnerModelObject) intAug);

        return inter;
    }

    /**
     * Returns ipv6 address filled with attached VPN, ipv6 address and mask.
     *
     * @param vpnName VPN name
     * @param mask    mask
     * @param ipv6Add ipv6 address
     * @return device ipv6 address
     */
    private static Ipv6 getIpv6Aug(String vpnName, short mask, String ipv6Add) {
        AugmentedIpIpv6 augIpv6 = new DefaultAugmentedIpIpv6();
        org.onosproject.yang.gen.v1.ietfip.rev20140616.ietfip.devices.device
                .interfaces.yangautoprefixinterface.augmentedifinterface.ipv6
                .Address add = new org.onosproject.yang.gen.v1.ietfip
                .rev20140616.ietfip.devices.device.interfaces
                .yangautoprefixinterface.augmentedifinterface.ipv6
                .DefaultAddress();
        Ipv6 ipv6 = new DefaultIpv6();
        List<org.onosproject.yang.gen.v1.ietfip
                .rev20140616.ietfip.devices.device.interfaces
                .yangautoprefixinterface.augmentedifinterface.ipv6
                .Address> addList = new LinkedList<>();
        add.ip(Ipv6AddressNoZone.of(Ipv6Address.of(ipv6Add)));
        augIpv6.bindNetworkInstanceName(vpnName);
        add.prefixLength(mask);
        addList.add(add);
        ipv6.address(addList);
        ((DefaultIpv6) ipv6).addAugmentation((DefaultAugmentedIpIpv6) augIpv6);
        return ipv6;
    }

    /**
     * Returns ipv4 address filled with attached VPN, ipv4 address and mask.
     *
     * @param vpnName VPN name
     * @param mask    mask
     * @param ipv4Add ipv4 address
     * @return device ipv4 address
     */
    private static Ipv4 getIpv4Aug(String vpnName, short mask, String ipv4Add) {
        AugmentedIpIpv4 augIpv4 = new DefaultAugmentedIpIpv4();
        Subnet net = new DefaultPrefixLength();
        Address add = new DefaultAddress();
        Ipv4 ipv4 = new DefaultIpv4();
        List<Address> addList = new LinkedList<>();

        augIpv4.bindNetworkInstanceName(vpnName);
        ((DefaultPrefixLength) net).prefixLength(mask);
        add.ip(Ipv4AddressNoZone.of(Ipv4Address.of(ipv4Add)));
        add.subnet(net);
        addList.add(add);
        ipv4.address(addList);
        ((DefaultIpv4) ipv4).addAugmentation((DefaultAugmentedIpIpv4) augIpv4);
        return ipv4;
    }

    /**
     * Builds ip address according to the existence of ip address in ip
     * connection of device model.
     *
     * @param connect ip connection
     * @param vpnName VPN name
     * @return augmented interface
     */
    public static AugmentedIfInterface buildIpAddress(IpConnection connect,
                                                      String vpnName) {
        if (connect == null || (connect.ipv4() == null
                && connect.ipv6() == null)) {
            throw new NetL3VpnException(IP_ADD_NULL);
        }
        AugmentedIfInterface intAug = new DefaultAugmentedIfInterface();
        short mask;
        if (connect.ipv4() != null) {
            mask = connect.ipv4().addresses().mask();
            Ipv4Address peIpv4 = connect.ipv4().addresses().providerAddress();
            Ipv4 v4 = getIpv4Aug(vpnName, mask, peIpv4.string());
            intAug.ipv4(v4);
        }

        if (connect.ipv6() != null) {
            mask = connect.ipv6().addresses().mask();
            Ipv6Address peIpv6 = connect.ipv6().addresses().providerAddress();
            Ipv6 v6 = getIpv6Aug(vpnName, mask, peIpv6.string());
            intAug.ipv6(v6);
        }
        return intAug;
    }
}
