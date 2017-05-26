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

import org.onosproject.l3vpn.netl3vpn.FullMeshVpnConfig;
import org.onosproject.l3vpn.netl3vpn.HubSpokeVpnConfig;
import org.onosproject.l3vpn.netl3vpn.VpnInstance;
import org.onosproject.l3vpn.netl3vpn.VpnSiteRole;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.devices.device.networkinstances.networkinstance.AugmentedNiNetworkInstance;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.devices.device.networkinstances.networkinstance.DefaultAugmentedNiNetworkInstance;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.devices.device.networkinstances.networkinstance.augmentedninetworkinstance.DefaultL3Vpn;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.devices.device.networkinstances.networkinstance.augmentedninetworkinstance.L3Vpn;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.l3vpnvrfparams.DefaultIpv4;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.l3vpnvrfparams.DefaultIpv6;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.l3vpnvrfparams.Ipv4;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.l3vpnvrfparams.Ipv6;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.l3vpnvrfparams.ipv4.DefaultUnicast;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.l3vpnvrfparams.ipv4.Unicast;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routedistinguisherparams.DefaultRouteDistinguisher;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routedistinguisherparams.RouteDistinguisher;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetparams.DefaultRouteTargets;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetparams.RouteTargets;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetparams.routetargets.Config;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetparams.routetargets.DefaultConfig;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetset.DefaultRts;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetset.Rts;
import org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetset.rts.RtTypeEnum;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siteattachmentipconnection.IpConnection;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.DefaultNetworkInstances;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.NetworkInstances;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.networkinstances.DefaultNetworkInstance;
import org.onosproject.yang.gen.v1.ietfnetworkinstance.rev20160623.ietfnetworkinstance.devices.device.networkinstances.NetworkInstance;

import java.util.LinkedList;
import java.util.List;

import static org.onosproject.l3vpn.netl3vpn.VpnType.ANY_TO_ANY;
import static org.onosproject.l3vpn.netl3vpn.VpnType.HUB;
import static org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetset.rts.RtTypeEnum.BOTH;
import static org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetset.rts.RtTypeEnum.EXPORT;
import static org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.routetargetset.rts.RtTypeEnum.IMPORT;

/**
 * Representation of utility for instance creation and deletion.
 */
public final class InsConstructionUtil {

    // No instantiation.
    private InsConstructionUtil() {
    }

    /**
     * Creates network instance with augmented info such as RD and RT.
     *
     * @param vpnIns  VPN instance
     * @param role    VPN role
     * @param connect ip connection
     * @return network instance
     */
    public static NetworkInstances createInstance(VpnInstance vpnIns,
                                                  VpnSiteRole role,
                                                  IpConnection connect) {
        NetworkInstance ins = new DefaultNetworkInstance();
        NetworkInstances instances = new DefaultNetworkInstances();
        List<NetworkInstance> insList = new LinkedList<>();

        L3Vpn l3Vpn = buildRd(vpnIns);
        DefaultAugmentedNiNetworkInstance augIns =
                buildRt(connect, role, l3Vpn, vpnIns);
        ins.name(vpnIns.vpnName());
        insList.add(ins);
        ((DefaultNetworkInstance) ins).addAugmentation(augIns);
        instances.networkInstance(insList);
        return instances;
    }

    /**
     * Builds RT from l3 VPN according to the address family VPN belongs to.
     * It returns built aug network instance from l3 VPN.
     *
     * @param con   ip connection
     * @param role  site VPN role
     * @param l3Vpn l3 VPN
     * @param ins   VPN instance
     * @return aug network instance
     */
    private static DefaultAugmentedNiNetworkInstance buildRt(IpConnection con,
                                                             VpnSiteRole role,
                                                             L3Vpn l3Vpn,
                                                             VpnInstance ins) {
        Ipv4 ipv4 = null;
        Ipv6 ipv6 = null;
        if (con.ipv4() != null && con.ipv4().addresses()
                .providerAddress() != null) {
            ipv4 = buildIpv4Rt(role, ins);
        }
        if (con.ipv6() != null && con.ipv6()
                .addresses().providerAddress() != null) {
            ipv6 = buildIpv6Rt(role, ins);
        }
        l3Vpn.ipv4(ipv4);
        l3Vpn.ipv6(ipv6);

        AugmentedNiNetworkInstance augInst =
                new DefaultAugmentedNiNetworkInstance();
        augInst.l3Vpn(l3Vpn);
        return (DefaultAugmentedNiNetworkInstance) augInst;
    }

    /**
     * Builds ipv6 RT in the device model.
     *
     * @param role   site VPN role
     * @param vpnIns VPN instance
     * @return ipv6
     */
    private static Ipv6 buildIpv6Rt(VpnSiteRole role, VpnInstance vpnIns) {
        RouteTargets rts6 = new DefaultRouteTargets();
        Ipv6 v6 = new DefaultIpv6();
        org.onosproject.yang.gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn
                .l3vpnvrfparams.ipv6.Unicast uni6 = new org.onosproject.yang
                .gen.v1.ietfbgpl3vpn.rev20160909.ietfbgpl3vpn.l3vpnvrfparams
                .ipv6.DefaultUnicast();

        Config configV6 = configRouteTarget(vpnIns, role);
        rts6.config(configV6);
        uni6.routeTargets(rts6);
        v6.unicast(uni6);
        return v6;
    }

    /**
     * Builds ipv4 RT in the device model.
     *
     * @param role   site VPN role
     * @param vpnIns VPN instance
     * @return ipv4
     */
    private static Ipv4 buildIpv4Rt(VpnSiteRole role, VpnInstance vpnIns) {
        RouteTargets rts4 = new DefaultRouteTargets();
        Unicast uni4 = new DefaultUnicast();
        Ipv4 v4 = new DefaultIpv4();

        Config configV4 = configRouteTarget(vpnIns, role);
        rts4.config(configV4);
        uni4.routeTargets(rts4);
        v4.unicast(uni4);
        return v4;
    }

    /**
     * Configures route target according to the site VPN role from the stored
     * VPN instance.
     *
     * @param ins  VPN instance
     * @param role site VPN role
     * @return route target config
     */
    private static Config configRouteTarget(VpnInstance ins,
                                            VpnSiteRole role) {
        Rts rts1;
        Config config = new DefaultConfig();
        List<Rts> rtsList = new LinkedList<>();

        if (ins.type() == ANY_TO_ANY) {
            String rtVal = ((FullMeshVpnConfig) ins.vpnConfig()).rt();
            rts1 = getRtsVal(rtVal, BOTH);
        } else {
            String rtVal1;
            String rtVal2;
            HubSpokeVpnConfig conf = (HubSpokeVpnConfig) ins.vpnConfig();
            if (role.role() == HUB) {
                rtVal1 = conf.hubImpRt();
                rtVal2 = conf.hubExpRt();
            } else {
                rtVal1 = conf.spokeImpRt();
                rtVal2 = conf.spokeExpRt();
            }
            rts1 = getRtsVal(rtVal1, IMPORT);
            Rts rts2 = getRtsVal(rtVal2, EXPORT);
            rtsList.add(rts2);
        }
        rtsList.add(rts1);
        config.rts(rtsList);
        return config;
    }

    /**
     * Returns the device model RT from the RT type and RT value after
     * building it.
     *
     * @param rtVal RT value
     * @param type  RT type
     * @return device model RT
     */
    private static Rts getRtsVal(String rtVal, RtTypeEnum type) {
        Rts rts = new DefaultRts();
        rts.rt(rtVal);
        rts.rtType(type);
        return rts;
    }

    /**
     * Builds RD from the stored device model VPN instance.
     *
     * @param vpn VPN instance
     * @return l3 VPN object
     */
    private static L3Vpn buildRd(VpnInstance vpn) {
        String rd = vpn.vpnConfig().rd();
        org.onosproject.yang.gen.v1.ietfbgpl3vpn
                .rev20160909.ietfbgpl3vpn.routedistinguisherparams
                .routedistinguisher.Config config = new org.onosproject.yang
                .gen.v1.ietfbgpl3vpn.rev20160909
                .ietfbgpl3vpn.routedistinguisherparams.routedistinguisher
                .DefaultConfig();
        config.rd(rd);
        RouteDistinguisher dist = new DefaultRouteDistinguisher();
        dist.config(config);
        L3Vpn l3vpn = new DefaultL3Vpn();
        l3vpn.routeDistinguisher(dist);
        return l3vpn;
    }

    /**
     * Constructs network instance for delete of VPN instance.
     *
     * @param vpnName VPN name
     * @return network instances
     */
    static NetworkInstances deleteInstance(String vpnName) {
        NetworkInstance nwInstance = new DefaultNetworkInstance();
        List<NetworkInstance> insList = new LinkedList<>();
        NetworkInstances instances = new DefaultNetworkInstances();
        nwInstance.name(vpnName);
        insList.add(nwInstance);
        instances.networkInstance(insList);
        return instances;
    }
}
