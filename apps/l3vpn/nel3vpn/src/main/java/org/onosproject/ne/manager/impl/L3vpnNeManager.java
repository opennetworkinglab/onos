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
package org.onosproject.ne.manager.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.ne.NeData;
import org.onosproject.ne.VpnAc;
import org.onosproject.ne.VpnInstance;
import org.onosproject.ne.VrfEntity;
import org.onosproject.ne.manager.L3vpnNeService;
import org.onosproject.ne.util.DataConvertUtil;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.L3vpnConfig;
import org.onosproject.net.behaviour.NetconfBgp;
import org.onosproject.net.behaviour.NetconfBgpVrf;
import org.onosproject.net.behaviour.NetconfBgpVrfAF;
import org.onosproject.net.behaviour.NetconfBgpVrfAFs;
import org.onosproject.net.behaviour.NetconfBgpVrfs;
import org.onosproject.net.behaviour.NetconfBgpcomm;
import org.onosproject.net.behaviour.NetconfImportRoute;
import org.onosproject.net.behaviour.NetconfImportRoutes;
import org.onosproject.net.behaviour.NetconfL3vpn;
import org.onosproject.net.behaviour.NetconfL3vpnComm;
import org.onosproject.net.behaviour.NetconfL3vpnIf;
import org.onosproject.net.behaviour.NetconfL3vpnIfs;
import org.onosproject.net.behaviour.NetconfL3vpnInstance;
import org.onosproject.net.behaviour.NetconfL3vpnInstances;
import org.onosproject.net.behaviour.NetconfVpnInstAF;
import org.onosproject.net.behaviour.NetconfVpnInstAFs;
import org.onosproject.net.behaviour.NetconfVpnTarget;
import org.onosproject.net.behaviour.NetconfVpnTargets;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.LogicalClockService;
import org.onosproject.store.service.StorageService;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.Ipv4Address;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.L3VpncommonL3VpnPrefixType;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.L3VpncommonVrfRtType;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.Bgpcomm;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.BgpVrf;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.BgpVrfAf;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf.importroutes.ImportRoute;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.L3VpnInstances;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.L3VpnInstancesBuilder.L3VpnInstancesImpl;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.L3VpnInstance;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs.VpnInstAf;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs.vpninstaf.vpntargets.VpnTarget;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs.vpninstaf.vpntargets.VpnTargetBuilder.VpnTargetImpl;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.l3vpnifs.L3VpnIf;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.l3vpnifs.L3VpnIfBuilder.L3VpnIfImpl;
import org.slf4j.Logger;

/**
 * Provides implementation of L3vpnNeService.
 */
@Component(immediate = true)
@Service
public class L3vpnNeManager implements L3vpnNeService {
    private final Logger log = getLogger(getClass());
    private static final String APP_ID = "org.onosproject.app.l3vpn.ne";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LogicalClockService clockService;

    private ApplicationId appId;

    private static final String CONTENT_VERSION = "1.0";
    private static final String FORMAT_VERSION = "1.0";
    private static final String EDIT_OPERATION_CREATE = "create";
    private static final String EDIT_OPERATION_MERGE = "merge";
    private static final String EDIT_OPERATION_REPLACE = "replace";
    private static final String EDIT_OPERATION_DELETE = "delete";
    private static final String OK = "ok";

    private EventuallyConsistentMap<String, L3VpnInstances> l3VpnInstancesStore;
    private EventuallyConsistentMap<String, Bgpcomm> bgpcommStore;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API).register(L3VpnInstancesImpl.class)
                .register(L3VpnIfImpl.class).register(Ipv4Address.class)
                .register(L3VpncommonL3VpnPrefixType.class)
                .register(VpnTargetImpl.class)
                .register(L3VpncommonVrfRtType.class);
        l3VpnInstancesStore = storageService
                .<String, L3VpnInstances>eventuallyConsistentMapBuilder()
                .withName("").withSerializer(serializer)
                .withTimestampProvider((k, v) -> clockService.getTimestamp())
                .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean createL3vpn(NeData nedata) {
        if (nedata == null) {
            log.error("L3vpn ne data is null");
            return false;
        }
        List<VpnInstance> vpnInstanceList = nedata.vpnInstanceList();
        List<VpnAc> vpnAcList = nedata.vpnAcList();
        for (VpnInstance vpnInstance : vpnInstanceList) {
            Map<VrfEntity, HashSet<VpnAc>> vpnAcForVrfMap = new HashMap<VrfEntity, HashSet<VpnAc>>();
            String neId = vpnInstance.neId();
            DeviceId deviceId = DeviceId.deviceId(neId);
            if (mastershipService.isLocalMaster(deviceId)) {
                List<VrfEntity> vrfList = vpnInstance.vrfList();
                for (VrfEntity vrfEntity : vrfList) {
                    HashSet<VpnAc> vrfAcs = new HashSet<VpnAc>();
                    List<String> acIdList = vrfEntity.acIdList();
                    for (String acId : acIdList) {
                        for (VpnAc vpnAc : vpnAcList) {
                            if (vpnAc.acId().equals(acId)) {
                                vrfAcs.add(vpnAc);
                            }
                        }
                    }
                    vpnAcForVrfMap.put(vrfEntity, vrfAcs);
                }
                boolean vrfFlag = createVrf(deviceId, vpnAcForVrfMap);
                if (!vrfFlag) {
                    return false;
                }
                boolean bgpFlag = createBgpImportProtocol(deviceId,
                                                          vpnInstance);
                if (!bgpFlag) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Create virtual routing forwarding.
     */
    private boolean createVrf(DeviceId deviceId,
                              Map<VrfEntity, HashSet<VpnAc>> vpnAcForVrfMap) {
        L3VpnInstances l3VpnInstances = DataConvertUtil
                .convertToL3vpnInstances(vpnAcForVrfMap);
        String contentVersion = CONTENT_VERSION;
        String formatVersion = FORMAT_VERSION;
        List<NetconfL3vpnInstance> netconfInstanceList = new ArrayList<NetconfL3vpnInstance>();
        for (L3VpnInstance l3VpnInstance : l3VpnInstances.l3VpnInstance()) {
            String operation = EDIT_OPERATION_CREATE;
            String vrfName = l3VpnInstance.vrfName();
            // NetconfVpnInstAFs
            List<NetconfVpnInstAF> netconfAFList = new ArrayList<NetconfVpnInstAF>();
            for (VpnInstAf vpnInstAf : l3VpnInstance.vpnInstAfs().vpnInstAf()) {
                String afType = vpnInstAf.afType().toString()
                        .replaceAll("_", "").toLowerCase();
                String vrfRD = vpnInstAf.vrfRd();
                List<NetconfVpnTarget> netconfTargetList = new ArrayList<NetconfVpnTarget>();
                for (VpnTarget vpnTarget : vpnInstAf.vpnTargets().vpnTarget()) {
                    String vrfRTType = vpnTarget.vrfRttype().enumeration()
                            .name().toLowerCase();
                    String vrfRTValue = vpnTarget.vrfRtvalue();
                    NetconfVpnTarget netconfTarget = new NetconfVpnTarget(vrfRTType,
                                                                          vrfRTValue);
                    netconfTargetList.add(netconfTarget);
                }
                NetconfVpnTargets netconfTargets = new NetconfVpnTargets(netconfTargetList);
                NetconfVpnInstAF netconfAF = new NetconfVpnInstAF(afType, vrfRD,
                                                                  netconfTargets);
                netconfAFList.add(netconfAF);
            }
            NetconfVpnInstAFs netconfAFs = new NetconfVpnInstAFs(netconfAFList);
            // NetconfL3vpnIfs
            List<NetconfL3vpnIf> netconfIfList = new ArrayList<NetconfL3vpnIf>();
            for (L3VpnIf l3VpnIf : l3VpnInstance.l3VpnIfs().l3VpnIf()) {
                String ifOperation = EDIT_OPERATION_CREATE;
                String ifName = l3VpnIf.ifName();
                String ipv4Addr = l3VpnIf.ipv4Addr().string();
                String subnetMask = l3VpnIf.subnetMask().string();
                NetconfL3vpnIf netconfIf = new NetconfL3vpnIf(ifOperation,
                                                              ifName, ipv4Addr,
                                                              subnetMask);
                netconfIfList.add(netconfIf);
            }
            NetconfL3vpnIfs netconfIfs = new NetconfL3vpnIfs(netconfIfList);
            NetconfL3vpnInstance netconfL3vpnInstance = new NetconfL3vpnInstance(operation,
                                                                                 vrfName,
                                                                                 netconfAFs,
                                                                                 netconfIfs);
            netconfInstanceList.add(netconfL3vpnInstance);
        }
        NetconfL3vpnInstances netconfL3vpnInstances = new NetconfL3vpnInstances(netconfInstanceList);
        NetconfL3vpnComm netconfComm = new NetconfL3vpnComm(netconfL3vpnInstances);
        NetconfL3vpn netconfL3vpn = new NetconfL3vpn(contentVersion,
                                                     formatVersion,
                                                     netconfComm);
        // invoke driver L3vpnConfig
        DriverHandler handler = driverService.createHandler(deviceId);
        L3vpnConfig l3vpnConfig = handler.behaviour(L3vpnConfig.class);
        return l3vpnConfig.createVrf(deviceId, netconfL3vpn);
    }

    /**
     * Create Bgp Import Protocol.
     */
    private boolean createBgpImportProtocol(DeviceId deviceId,
                                            VpnInstance vpnInstance) {
        Bgpcomm bgpcomm = DataConvertUtil.convertToBgpComm(vpnInstance);
        String contentVersion = CONTENT_VERSION;
        String formatVersion = FORMAT_VERSION;
        List<NetconfBgpVrf> netconfBgpvrfList = new ArrayList<NetconfBgpVrf>();
        for (BgpVrf bgpVrf : bgpcomm.bgpVrfs().bgpVrf()) {
            String operation = EDIT_OPERATION_CREATE;
            String vrfName = bgpVrf.vrfName();
            List<NetconfBgpVrfAF> netconfBgpVrfAFList = new ArrayList<NetconfBgpVrfAF>();
            for (BgpVrfAf bgpVrfAf : bgpVrf.bgpVrfAfs().bgpVrfAf()) {
                String afType = bgpVrfAf.afType().enumeration().name()
                        .toLowerCase();
                List<NetconfImportRoute> netconfImportRouteList = new ArrayList<NetconfImportRoute>();
                for (ImportRoute importRoute : bgpVrfAf.importRoutes()
                        .importRoute()) {
                    String importRouteOperation = EDIT_OPERATION_CREATE;
                    String importProtocol = importRoute.importProtocol()
                            .enumeration().name().toLowerCase();
                    NetconfImportRoute netconfImportRoute = new NetconfImportRoute(importRouteOperation,
                                                                                   importProtocol);
                    netconfImportRouteList.add(netconfImportRoute);
                }
                NetconfImportRoutes netconfImportRoutes = new NetconfImportRoutes(netconfImportRouteList);
                NetconfBgpVrfAF netconfBgpVrfAF = new NetconfBgpVrfAF(afType,
                                                                      netconfImportRoutes);
                netconfBgpVrfAFList.add(netconfBgpVrfAF);
            }
            NetconfBgpVrfAFs netconfBgpVrfAFs = new NetconfBgpVrfAFs(netconfBgpVrfAFList);
            NetconfBgpVrf netconfBgpVrf = new NetconfBgpVrf(operation, vrfName,
                                                            netconfBgpVrfAFs);
            netconfBgpvrfList.add(netconfBgpVrf);
        }
        NetconfBgpVrfs netconfBgpVrfs = new NetconfBgpVrfs(netconfBgpvrfList);
        NetconfBgpcomm netconfBgpcomm = new NetconfBgpcomm(netconfBgpVrfs);
        NetconfBgp netconfBgp = new NetconfBgp(contentVersion, formatVersion,
                                               netconfBgpcomm);
        // invoke driver L3vpnConfig
        DriverHandler handler = driverService.createHandler(deviceId);
        L3vpnConfig l3vpnConfig = handler.behaviour(L3vpnConfig.class);
        return l3vpnConfig.createBgpImportProtocol(deviceId, netconfBgp);
    }
}
