/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.config.DynamicConfigService;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.l3vpn.netl3vpn.DeviceInfo;
import org.onosproject.l3vpn.netl3vpn.ModelIdLevel;
import org.onosproject.l3vpn.netl3vpn.NetL3VpnException;
import org.onosproject.l3vpn.netl3vpn.NetL3VpnStore;
import org.onosproject.l3vpn.netl3vpn.TunnelInfo;
import org.onosproject.l3vpn.netl3vpn.VpnType;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.pce.pceservice.api.PceService;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ResourceData;

import java.util.List;
import java.util.Map;

import static org.onosproject.l3vpn.netl3vpn.ModelIdLevel.DEVICE;
import static org.onosproject.l3vpn.netl3vpn.ModelIdLevel.DEVICES;
import static org.onosproject.l3vpn.netl3vpn.ModelIdLevel.TNL_M;
import static org.onosproject.l3vpn.netl3vpn.ModelIdLevel.TNL_POL;
import static org.onosproject.l3vpn.netl3vpn.ModelIdLevel.TP_HOP;
import static org.onosproject.l3vpn.netl3vpn.VpnType.SPOKE;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.NEW_NAME;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getId;
import static org.onosproject.l3vpn.netl3vpn.impl.NetL3VpnUtil.getIpFromDevId;
import static org.onosproject.pce.pceservice.LspType.WITH_SIGNALLING;

/**
 * Represents net l3VPN tunnel handler, which handles tunnel operations like
 * creation and deletion and updating it to the driver.
 */
public class NetL3VpnTunnelHandler {

    private PceService pceSvc;
    private DriverService driSvc;
    private DynamicConfigService configSvc;
    private NetL3VpnStore store;
    private DeviceService devSvc;
    private IdGenerator tnlIdGen;
    private ModelConverter modelCon;
    private String sIp;
    private String vpnName;
    private DeviceInfo sInfo;
    private VpnType type;

    /**
     * Constructs net l3VPN tunnel handler with required services.
     *
     * @param p   pce service
     * @param d   driver service
     * @param c   dynamic config service
     * @param s   net l3VPN store
     * @param dev device service
     * @param id  ID generator
     * @param m   model converter
     */
    public NetL3VpnTunnelHandler(PceService p, DriverService d,
                                 DynamicConfigService c,
                                 NetL3VpnStore s, DeviceService dev,
                                 IdGenerator id, ModelConverter m) {
        pceSvc = p;
        driSvc = d;
        configSvc = c;
        store = s;
        devSvc = dev;
        tnlIdGen = id;
        modelCon = m;
    }

    /**
     * Creates the source information for tunnel creation. It creates from
     * source device info and VPN name.
     *
     * @param vName   VPN name
     * @param devInfo device info
     */
    public void createSrcInfo(String vName, DeviceInfo devInfo) {
        vpnName = vName;
        sInfo = devInfo;
        type = devInfo.type();
        sIp = getIpFromDevId(sInfo.deviceId());
    }

    /**
     * Creates tunnel between source and destination devices.
     *
     * @param dInfo destination device
     */
    public void createSrcDesTunnel(DeviceInfo dInfo) {
        VpnType dType = dInfo.type();
        if (type == SPOKE && dType == SPOKE) {
            return;
        }
        String dIp = getIpFromDevId(dInfo.deviceId());
        createTunnelInfo(sIp, dIp, sInfo);
        createTunnelInfo(dIp, sIp, dInfo);
    }

    /**
     * Creates tunnel info and tunnel based on source and destination ip
     * address and configures it in the source device.
     *
     * @param sIp   source ip address
     * @param dIp   destination ip address
     * @param sInfo source device info
     */
    private void createTunnelInfo(String sIp, String dIp, DeviceInfo sInfo) {
        DeviceId id = sInfo.deviceId();
        Map<DeviceId, Integer> tnlMap = store.getTunnelInfo();
        int count = 0;
        if (tnlMap.containsKey(id)) {
            count = tnlMap.get(id);
        }
        String tnlName = createTunnel(sIp, dIp);
        sInfo.addTnlName(tnlName);
        store.addTunnelInfo(id, count + 1);
        TunnelInfo tnl = new TunnelInfo(dIp, tnlName, vpnName, id.toString());
        configureDevTnl(sInfo, tnl, tnlMap);
    }

    /**
     * Creates tunnel between source ip address and destination ip address
     * with pce service.
     *
     * @param srcIp source ip address
     * @param desIp destination ip address
     * @return tunnel name
     */
    private String createTunnel(String srcIp, String desIp) {
        Iterable<Device> devices = devSvc.getAvailableDevices();
        DeviceId srcDevId = getId(srcIp, false, devices);
        DeviceId desDevId = getId(desIp, false, devices);
        String name = getNewName();
        boolean isCreated = pceSvc.setupPath(srcDevId, desDevId, name,
                                             null, WITH_SIGNALLING);
        if (!isCreated) {
            throw new NetL3VpnException("Tunnel is not created between " +
                                                srcDevId.toString() + " and " +
                                                desDevId.toString());
        }
        return name;
    }

    /**
     * Returns a unique name for tunnel to be created.
     *
     * @return unique tunnel name
     */
    private String getNewName() {
        return NEW_NAME + String.valueOf(tnlIdGen.getNewId());
    }

    /**
     * Configures the created tunnel to the device by processing it at the
     * proper level and sending it to the driver.
     *
     * @param info    source device info
     * @param tnlInfo tunnel info
     * @param tnlMap  store tunnel map
     */
    private void configureDevTnl(DeviceInfo info, TunnelInfo tnlInfo,
                                 Map<DeviceId, Integer> tnlMap) {
        DeviceId id = info.deviceId();
        int count = 0;
        if (tnlMap.containsKey(id)) {
            count = tnlMap.get(id);
        }
        if (tnlMap.size() == 0) {
            tnlInfo.level(DEVICES);
        } else if (count == 0) {
            tnlInfo.level(DEVICE);
        }

        if (tnlInfo.level() != null) {
            ModelObjectData mod = info.processCreateTnlDev(driSvc, tnlInfo);
            addDataNodeToStore(mod);
            tnlInfo.level(TNL_M);
            tnlPolToStore(info, tnlInfo);
        }
        if (!info.isTnlPolCreated()) {
            tnlInfo.level(TNL_POL);
            tnlPolToStore(info, tnlInfo);
        }
        if (tnlInfo.level() == null) {
            tnlInfo.level(TP_HOP);
        }

        ModelObjectData tnlMod = info.processCreateTnl(driSvc, tnlInfo);
        addDataNodeToStore(tnlMod);
        if (tnlInfo.level() != TP_HOP) {
            ModelObjectData mod = info.processBindTnl(driSvc, tnlInfo);
            addDataNodeToStore(mod);
        }
    }

    /**
     * Adds data node to the store after converting it to the resource data.
     *
     * @param driMod driver model object data
     */
    private void addDataNodeToStore(ModelObjectData driMod) {
        ResourceData resData = modelCon.createDataNode(driMod);
        addToStore(resData);
    }

    /**
     * Adds resource data to the store.
     *
     * @param resData resource data
     */
    private void addToStore(ResourceData resData) {
        if (resData != null && resData.dataNodes() != null) {
            List<DataNode> dataNodes = resData.dataNodes();
            for (DataNode node : dataNodes) {
                configSvc.createNode(resData.resourceId(), node);
            }
        }
    }

    /**
     * Creates tunnel policy from driver and adds it to the store.
     *
     * @param info    device info
     * @param tnlInfo tunnel info
     */
    private void tnlPolToStore(DeviceInfo info, TunnelInfo tnlInfo) {
        ModelObjectData mod = info.processCreateTnlPol(driSvc, tnlInfo);
        addDataNodeToStore(mod);
        info.setTnlPolCreated(true);
    }

    /**
     * Deletes the tunnel with the source tunnel info and VPN name.
     * //FIXME: PCE does'nt have api, which can give tunnel by providing the
     * tunnel name.
     *
     * @param info  device info
     * @param vName VPN name
     */
    public void deleteTunnel(DeviceInfo info, String vName) {
        List<String> tnlNames = info.tnlNames();
        for (String tnlName : tnlNames) {
            Iterable<Tunnel> path = pceSvc.queryAllPath();
            for (Tunnel tnl : path) {
                if (tnl.tunnelName().toString().equals(tnlName)) {
                    pceSvc.releasePath(tnl.tunnelId());
                    break;
                }
            }
        }
        deleteFromDevice(info, vName);
    }

    /**
     * Deletes tunnel configuration from the device by updating various
     * levels in the store.
     *
     * @param info  device info
     * @param vName VPN name
     */
    private void deleteFromDevice(DeviceInfo info, String vName) {
        Map<DeviceId, Integer> map = store.getTunnelInfo();
        DeviceId id = info.deviceId();
        Integer count = map.get(id);
        int tnlCount = info.tnlNames().size();
        int upCount = count - tnlCount;
        ModelIdLevel level;
        TunnelInfo tnlInfo = new TunnelInfo(null, null, vName, id.toString());
        if (upCount == 0) {
            if (map.size() == 1) {
                level = DEVICES;
            } else {
                level = DEVICE;
            }
        } else {
            if (map.size() > 1) {
                level = TNL_POL;
            } else {
                return;
            }
        }
        tnlInfo.level(level);
        ModelObjectData mod = info.processDeleteTnl(driSvc, tnlInfo);
        deleteFromStore(mod);
        info.tnlNames(null);
        info.setTnlPolCreated(false);
        if (upCount == 0) {
            store.removeTunnelInfo(id);
        } else {
            store.addTunnelInfo(id, upCount);
        }
    }

    /**
     * Deletes the data node from the store.
     *
     * @param mod driver model object data
     */
    private void deleteFromStore(ModelObjectData mod) {
        ResourceData resData = modelCon.createDataNode(mod);
        if (resData != null) {
            configSvc.deleteNode(resData.resourceId());
        }
    }
}
