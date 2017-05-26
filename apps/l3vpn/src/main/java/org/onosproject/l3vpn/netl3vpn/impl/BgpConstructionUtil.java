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
import org.onosproject.l3vpn.netl3vpn.BgpInfo;
import org.onosproject.l3vpn.netl3vpn.DeviceInfo;
import org.onosproject.l3vpn.netl3vpn.NetL3VpnException;
import org.onosproject.l3vpn.netl3vpn.ProtocolInfo;
import org.onosproject.l3vpn.netl3vpn.RouteProtocol;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.RoutingProtocolType;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siteattachmentipconnection.IpConnection;
import org.onosproject.yang.gen.v1.ietfl3vpnsvc.rev20160730.ietfl3vpnsvc.siterouting.routingprotocols.RoutingProtocol;

import java.util.List;
import java.util.Map;

import static org.onosproject.l3vpn.netl3vpn.RouteProtocol.DIRECT;
import static org.onosproject.l3vpn.netl3vpn.RouteProtocol.STATIC;
import static org.onosproject.l3vpn.netl3vpn.RouteProtocol.getProType;

/**
 * Representation of utility for BGP info creation and deletion.
 */
public final class BgpConstructionUtil {

    private static final String ZERO = "0";

    // No instantiation.
    private BgpConstructionUtil() {
    }

    /**
     * Creates the BGP info instance, from the routing protocols available.
     * It returns BGP info if for the first time, else returns null.
     *
     * @param routes  route protocol
     * @param info    device info
     * @param vpnName VPN name
     * @param connect ip connection
     * @param access  access info
     * @return BGP info instance
     */
    public static BgpInfo createBgpInfo(List<RoutingProtocol> routes,
                                        DeviceInfo info, String vpnName,
                                        IpConnection connect, AccessInfo access) {
        BgpInfo devBgp = info.bgpInfo();
        BgpInfo infoBgp = new BgpInfo();
        infoBgp.vpnName(vpnName);
        if (devBgp != null) {
            infoBgp = updateDevBgpInfo(devBgp, infoBgp, connect, routes, access);
        } else {
            infoBgp = updateDevBgpInfo(null, infoBgp, connect, routes, access);
            info.bgpInfo(infoBgp);
        }
        if (infoBgp == null || infoBgp.protocolInfo() == null) {
            return null;
        }
        return infoBgp;
    }

    /**
     * Updates the device BGP info and also creates the BGP info which has to
     * be sent to driver, if it is called for the first time.
     *
     * @param devBgp  device BGP info
     * @param driBgp  driver BGP info
     * @param connect ip connection
     * @param routes  route protocols
     * @param access  access info
     * @return driver BGP info
     */
    private static BgpInfo updateDevBgpInfo(BgpInfo devBgp, BgpInfo driBgp,
                                            IpConnection connect,
                                            List<RoutingProtocol> routes,
                                            AccessInfo access) {
        for (RoutingProtocol route : routes) {
            ProtocolInfo ifInfo = getRoutePro(route.type(), connect, access);
            if (ifInfo != null) {
                if (devBgp != null) {
                    ProtocolInfo info = addToDevBgp(ifInfo, devBgp, access);
                    ifInfo = getUpdatedProInfo(info, ifInfo);
                }
                if (ifInfo != null) {
                    driBgp.addProtocolInfo(ifInfo.routeProtocol(), ifInfo);
                }
            }
        }
        return driBgp;
    }

    /**
     * Returns the updated protocol info that has to be sent to driver. If
     * the protocol info is for the second time or more, the driver info's
     * protocol info will not be sent. It will return null if no info is
     * present or nothing to be sent to driver.
     *
     * @param devInfo device protocol info
     * @param driInfo driver protocol info
     * @return updated driver protocol info
     */
    private static ProtocolInfo getUpdatedProInfo(ProtocolInfo devInfo,
                                                  ProtocolInfo driInfo) {
        if (driInfo.isIpv4Af() && driInfo.isIpv6Af()) {
            if ((getV4Size(devInfo) > 1) && (getV6Size(devInfo) > 1)) {
                return null;
            }
            if ((getV4Size(devInfo) > 1) && !(getV6Size(devInfo) > 1)) {
                driInfo.ipv4Af(false);
            } else if (!(getV4Size(devInfo) > 1) && (getV6Size(devInfo) > 1)) {
                driInfo.ipv6Af(false);
            }
        }
        if (driInfo.isIpv4Af() && !driInfo.isIpv6Af()) {
            if (getV4Size(devInfo) > 1) {
                return null;
            }
        }
        if (!driInfo.isIpv4Af() && driInfo.isIpv6Af()) {
            if (getV6Size(devInfo) > 1) {
                return null;
            }
        }
        return driInfo;
    }

    private static int getV4Size(ProtocolInfo proInfo) {
        return proInfo.v4Accesses().size();
    }

    private static int getV6Size(ProtocolInfo proInfo) {
        return proInfo.v6Accesses().size();
    }

    /**
     * Adds the protocol info to the device BGP info.
     *
     * @param proInfo protocol info
     * @param devBgp  device BGP
     * @param access  access info
     * @return protocol info
     */
    private static ProtocolInfo addToDevBgp(ProtocolInfo proInfo,
                                            BgpInfo devBgp, AccessInfo access) {
        Map<RouteProtocol, ProtocolInfo> devMap = devBgp.protocolInfo();
        ProtocolInfo devInfo = devMap.get(proInfo.routeProtocol());
        if (devInfo != null) {
            if (proInfo.isIpv4Af()) {
                devInfo.ipv4Af(proInfo.isIpv4Af());
                devInfo.addV4Access(access);
            }
            if (proInfo.isIpv6Af()) {
                devInfo.ipv6Af(proInfo.isIpv6Af());
                devInfo.addV6Access(access);
            }
        } else {
            devInfo = proInfo;
            devBgp.addProtocolInfo(proInfo.routeProtocol(), devInfo);
        }
        return devInfo;
    }


    /**
     * Returns the protocol info of BGP by taking values from the service files.
     *
     * @param type    protocol type
     * @param connect IP connection
     * @param access  access info
     * @return protocol info
     */
    private static ProtocolInfo getRoutePro(Class<? extends RoutingProtocolType> type,
                                            IpConnection connect, AccessInfo access) {
        ProtocolInfo protocolInfo = new ProtocolInfo();
        RouteProtocol protocol = getProType(type.getSimpleName());
        switch (protocol) {
            case DIRECT:
                protocolInfo.routeProtocol(DIRECT);
                protocolInfo.processId(ZERO);
                setAddressFamily(protocolInfo, connect, access);
                return protocolInfo;

            case STATIC:
                protocolInfo.routeProtocol(STATIC);
                protocolInfo.processId(ZERO);
                setAddressFamily(protocolInfo, connect, access);
                return protocolInfo;

            case BGP:
            case OSPF:
            case RIP:
            case RIP_NG:
            case VRRP:
            default:
                throw new NetL3VpnException(getRouteProErr(
                        type.getSimpleName()));
        }
    }

    /**
     * Returns the route protocol error message for unsupported type.
     *
     * @param type route protocol type
     * @return error message
     */
    private static String getRouteProErr(String type) {
        return type + " routing protocol is not supported.";
    }

    /**
     * Sets the address family of the protocol info.
     *
     * @param proInfo protocol info
     * @param connect ip connection
     * @param access  access info
     */
    private static void setAddressFamily(ProtocolInfo proInfo,
                                         IpConnection connect, AccessInfo access) {
        if (connect.ipv4() != null && connect.ipv4().addresses() != null &&
                connect.ipv4().addresses().providerAddress() != null) {
            proInfo.ipv4Af(true);
            proInfo.addV4Access(access);
        }
        if (connect.ipv6() != null && connect.ipv6().addresses() != null &&
                connect.ipv6().addresses().providerAddress() != null) {
            proInfo.ipv6Af(true);
            proInfo.addV6Access(access);
        }
    }
}
