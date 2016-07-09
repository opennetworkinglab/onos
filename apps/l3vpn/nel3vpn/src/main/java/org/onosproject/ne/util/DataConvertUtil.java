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
package org.onosproject.ne.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.onosproject.ne.Bgp;
import org.onosproject.ne.BgpImportProtocol;
import org.onosproject.ne.VpnAc;
import org.onosproject.ne.VpnInstance;
import org.onosproject.ne.VrfEntity;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.Ipv4Address;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.L3VpncommonL3VpnPrefixType;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.L3VpncommonVrfRtType;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.l3vpncommonl3vpnprefixtype.L3VpncommonL3VpnPrefixTypeEnum;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.l3vpncommonvrfrttype.L3VpncommonVrfRtTypeEnum;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.Bgpcomm;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.BgpcommBuilder;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.BgpVrfsBuilder;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.BgpVrf;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.BgpVrfBuilder;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.BgpVrfAfsBuilder;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.BgpVrfAf;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.BgpVrfAfBuilder;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf.ImportRoutesBuilder;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf.importroutes.ImportRoute;
import org.onosproject.yang.gen.v1.ne.bgpcomm.rev20141225.nebgpcomm.bgpcomm.bgpvrfs.bgpvrf.bgpvrfafs.bgpvrfaf.importroutes.ImportRouteBuilder;
import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.BgpcommImRouteProtocol;
import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.BgpcommPrefixType;
import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.bgpcommimrouteprotocol.BgpcommImRouteProtocolEnum;
import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.bgpcommprefixtype.BgpcommPrefixTypeEnum;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.L3VpnInstances;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.L3VpnInstancesBuilder;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.L3VpnInstance;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.L3VpnInstanceBuilder;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.VpnInstAfs;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.VpnInstAfsBuilder;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs.VpnInstAf;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs.VpnInstAfBuilder;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs.vpninstaf.VpnTargets;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs.vpninstaf.VpnTargetsBuilder;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs.vpninstaf.vpntargets.VpnTarget;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs.vpninstaf.vpntargets.VpnTargetBuilder;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.L3VpnIfs;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.L3VpnIfsBuilder;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.l3vpnifs.L3VpnIf;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.l3vpnifs.L3VpnIfBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataConvertUtil data convert util.
 */
public final class DataConvertUtil {
    private static final Logger log = LoggerFactory
            .getLogger(DataConvertUtil.class);

    /**
     * Constructs a DataConvertUtil object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully.
     * This class should not be instantiated.
     */
    private DataConvertUtil() {
    }

    /**
     * Convert To yang object L3vpnInstances.
     *
     * @param vpnAcForVrfMap vpnAcForVrfMap
     * @return L3VpnInstances
     */
    public static L3VpnInstances convertToL3vpnInstances(Map<VrfEntity, HashSet<VpnAc>> vpnAcForVrfMap) {
        L3VpnInstancesBuilder l3VpnInstancesBuilder = new L3VpnInstancesBuilder();
        List<L3VpnInstance> l3VpnInstanceList = new ArrayList<L3VpnInstance>();
        for (Map.Entry<VrfEntity, HashSet<VpnAc>> entry : vpnAcForVrfMap
                .entrySet()) {
            VrfEntity vrfEntity = entry.getKey();
            HashSet<VpnAc> vpnAcs = entry.getValue();
            L3VpnInstanceBuilder l3vpnInstanceBuilder = new L3VpnInstanceBuilder();
            l3vpnInstanceBuilder.vrfName(vrfEntity.vrfName());
            VpnInstAfs vpnInstAfs = convertToVpnInstAfs(vrfEntity);
            l3vpnInstanceBuilder.vpnInstAfs(vpnInstAfs);
            L3VpnIfs l3VpnIfs = convertToL3VpnIfs(vpnAcs);
            l3vpnInstanceBuilder.l3VpnIfs(l3VpnIfs);
            l3VpnInstanceList.add(l3vpnInstanceBuilder.build());
        }
        l3VpnInstancesBuilder.l3VpnInstance(l3VpnInstanceList);
        return l3VpnInstancesBuilder.build();
    }

    /**
     * Convert To yang object VpnInstAfs.
     *
     * @param vrfEntity virtual routing forwarding entity
     * @return VpnInstAfs
     */
    public static VpnInstAfs convertToVpnInstAfs(VrfEntity vrfEntity) {
        VpnInstAfsBuilder vpnInstAfsBuilder = new VpnInstAfsBuilder();
        List<VpnInstAf> vpnInstAfList = new ArrayList<VpnInstAf>();
        VpnInstAfBuilder vpnInstAfBuilder = new VpnInstAfBuilder();
        vpnInstAfBuilder.afType(L3VpncommonL3VpnPrefixType
                .of(L3VpncommonL3VpnPrefixTypeEnum.IPV4UNI));
        // Vrf Route Distinguisher
        vpnInstAfBuilder.vrfRd(vrfEntity.routeDistinguisher());
        // Vpn Targets
        List<VpnTarget> vpnTargetList = new ArrayList<VpnTarget>();
        List<String> erts = vrfEntity.exportTargets();
        if (erts != null && !erts.isEmpty()) {
            for (String ert : erts) {
                VpnTargetBuilder vpnErtTargetBuilder = new VpnTargetBuilder();
                vpnErtTargetBuilder.vrfRttype(L3VpncommonVrfRtType
                        .of(L3VpncommonVrfRtTypeEnum.EXPORT_EXTCOMMUNITY));
                vpnErtTargetBuilder.vrfRtvalue(ert);
                vpnTargetList.add(vpnErtTargetBuilder.build());
            }
        }
        List<String> irts = vrfEntity.importTargets();
        if (irts != null && !irts.isEmpty()) {
            for (String irt : irts) {
                VpnTargetBuilder vpnIrtTargetBuilder = new VpnTargetBuilder();
                vpnIrtTargetBuilder.vrfRttype(L3VpncommonVrfRtType
                        .of(L3VpncommonVrfRtTypeEnum.IMPORT_EXTCOMMUNITY));
                vpnIrtTargetBuilder.vrfRtvalue(irt);
                vpnTargetList.add(vpnIrtTargetBuilder.build());
            }
        }
        VpnTargets vpnTargets = new VpnTargetsBuilder().vpnTarget(vpnTargetList)
                .build();
        vpnInstAfBuilder.vpnTargets(vpnTargets);
        vpnInstAfList.add(vpnInstAfBuilder.build());
        vpnInstAfsBuilder.vpnInstAf(vpnInstAfList);
        return vpnInstAfsBuilder.build();
    }

    /**
     * Convert To yang object L3VpnIfs.
     *
     * @param vpnAcs set of VpnAc
     * @return L3VpnIf List of L3vpn interface
     */
    public static L3VpnIfs convertToL3VpnIfs(HashSet<VpnAc> vpnAcs) {
        List<L3VpnIf> l3vpnIfList = new ArrayList<L3VpnIf>();
        for (VpnAc vpnAc : vpnAcs) {
            L3VpnIfBuilder l3VpnIfBuilder = new L3VpnIfBuilder();
            l3VpnIfBuilder.ifName(vpnAc.acName());
            Ipv4Address ipv4Address = new Ipv4Address(vpnAc.ipAddress()
                    .split("/")[0]);
            l3VpnIfBuilder.ipv4Addr(ipv4Address);
            Ipv4Address subnetMask = new Ipv4Address(IpUtil
                    .getMask(String.valueOf(vpnAc.subNetMask())));
            l3VpnIfBuilder.subnetMask(subnetMask);
            l3vpnIfList.add(l3VpnIfBuilder.build());
        }
        L3VpnIfs l3VpnIfs = new L3VpnIfsBuilder().l3VpnIf(l3vpnIfList).build();
        return l3VpnIfs;
    }

    /**
     * Convert To yang object Bgpcomm.
     *
     * @param vpnInstance VpnInstance
     * @return Bgpcomm
     */
    public static Bgpcomm convertToBgpComm(VpnInstance vpnInstance) {
        BgpcommBuilder bgpcommBuilder = new BgpcommBuilder();
        BgpVrfsBuilder bgpVrfsBuilder = new BgpVrfsBuilder();
        List<BgpVrf> bgpVrfList = new ArrayList<BgpVrf>();
        for (VrfEntity vrfEntity : vpnInstance.vrfList()) {
            Bgp bgp = vrfEntity.bgp();
            BgpVrfBuilder bgpVrfBuilder = new BgpVrfBuilder();
            BgpVrfAfsBuilder bgpVrfAfsBuilder = new BgpVrfAfsBuilder();
            List<BgpVrfAf> bgpVrfAfList = new ArrayList<BgpVrfAf>();
            BgpVrfAfBuilder bgpVrfAfBuilder = new BgpVrfAfBuilder();
            ImportRoutesBuilder importRoutesBuilder = new ImportRoutesBuilder();
            List<ImportRoute> importRouteList = new ArrayList<ImportRoute>();
            for (BgpImportProtocol importPro : bgp.importProtocols()) {
                ImportRouteBuilder importRouteBuilder = new ImportRouteBuilder();
                switch (importPro.protocolType()) {
                case Direct:
                    importRouteBuilder
                            .importProtocol(new BgpcommImRouteProtocol(BgpcommImRouteProtocolEnum.DIRECT));
                    break;
                case Bgp:
                    importRouteBuilder
                            .importProtocol(new BgpcommImRouteProtocol(BgpcommImRouteProtocolEnum.STATIC));
                    break;
                case Isis:
                    importRouteBuilder
                            .importProtocol(new BgpcommImRouteProtocol(BgpcommImRouteProtocolEnum.ISIS));
                    break;
                case Ospf:
                    importRouteBuilder
                            .importProtocol(new BgpcommImRouteProtocol(BgpcommImRouteProtocolEnum.OSPF));
                    break;
                default:
                    break;
                }
                importRouteList.add(importRouteBuilder.build());
            }
            importRoutesBuilder.importRoute(importRouteList);
            bgpVrfAfBuilder.importRoutes(importRoutesBuilder.build());
            bgpVrfAfBuilder
                    .afType(new BgpcommPrefixType(BgpcommPrefixTypeEnum.IPV4UNI));
            bgpVrfAfList.add(bgpVrfAfBuilder.build());
            bgpVrfAfsBuilder.bgpVrfAf(bgpVrfAfList);
            bgpVrfBuilder.bgpVrfAfs(bgpVrfAfsBuilder.build());
            bgpVrfBuilder.vrfName(vrfEntity.vrfName());
            bgpVrfList.add(bgpVrfBuilder.build());
        }
        bgpVrfsBuilder.bgpVrf(bgpVrfList);
        bgpcommBuilder.bgpVrfs(bgpVrfsBuilder.build());
        return bgpcommBuilder.build();
    }

}
