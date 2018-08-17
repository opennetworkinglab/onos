/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.models.openconfig;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import org.osgi.service.component.annotations.Component;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.ianaiftype.rev20170330.IanaIfType;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.IetfInetTypes;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.IetfInterfaces;
import org.onosproject.yang.gen.v1.ietfyangtypes.rev20130715.IetfYangTypes;
import org.onosproject.yang.gen.v1.openconfigaaa.rev20170706.OpenconfigAaa;
import org.onosproject.yang.gen.v1.openconfigaaa.rev20170706.OpenconfigAaaRadius;
import org.onosproject.yang.gen.v1.openconfigaaa.rev20170706.OpenconfigAaaTacacs;
import org.onosproject.yang.gen.v1.openconfigaaatypes.rev20170706.OpenconfigAaaTypes;
import org.onosproject.yang.gen.v1.openconfigacl.rev20170526.OpenconfigAcl;
import org.onosproject.yang.gen.v1.openconfigaft.rev20170510.OpenconfigAft;
import org.onosproject.yang.gen.v1.openconfigaft.rev20170510.OpenconfigAftCommon;
import org.onosproject.yang.gen.v1.openconfigaft.rev20170510.OpenconfigAftEthernet;
import org.onosproject.yang.gen.v1.openconfigaft.rev20170510.OpenconfigAftIpv4;
import org.onosproject.yang.gen.v1.openconfigaft.rev20170510.OpenconfigAftIpv6;
import org.onosproject.yang.gen.v1.openconfigaft.rev20170510.OpenconfigAftMpls;
import org.onosproject.yang.gen.v1.openconfigaft.rev20170510.OpenconfigAftPf;
import org.onosproject.yang.gen.v1.openconfigaftnetworkinstance.rev20170113.OpenconfigAftNetworkInstance;
import org.onosproject.yang.gen.v1.openconfigafttypes.rev20170510.OpenconfigAftTypes;
import org.onosproject.yang.gen.v1.openconfigbgp.rev20170730.OpenconfigBgp;
import org.onosproject.yang.gen.v1.openconfigbgp.rev20170730.OpenconfigBgpCommon;
import org.onosproject.yang.gen.v1.openconfigbgp.rev20170730.OpenconfigBgpCommonMultiprotocol;
import org.onosproject.yang.gen.v1.openconfigbgp.rev20170730.OpenconfigBgpCommonStructure;
import org.onosproject.yang.gen.v1.openconfigbgp.rev20170730.OpenconfigBgpGlobal;
import org.onosproject.yang.gen.v1.openconfigbgp.rev20170730.OpenconfigBgpNeighbor;
import org.onosproject.yang.gen.v1.openconfigbgp.rev20170730.OpenconfigBgpPeerGroup;
import org.onosproject.yang.gen.v1.openconfigbgppolicy.rev20170730.OpenconfigBgpPolicy;
import org.onosproject.yang.gen.v1.openconfigbgptypes.rev20170730.OpenconfigBgpErrors;
import org.onosproject.yang.gen.v1.openconfigbgptypes.rev20170730.OpenconfigBgpTypes;
import org.onosproject.yang.gen.v1.openconfigcatalogtypes.rev20170501.OpenconfigCatalogTypes;
import org.onosproject.yang.gen.v1.openconfigchannelmonitor.rev20170708.OpenconfigChannelMonitor;
import org.onosproject.yang.gen.v1.openconfigifaggregate.rev20170714.OpenconfigIfAggregate;
import org.onosproject.yang.gen.v1.openconfigifethernet.rev20170714.OpenconfigIfEthernet;
import org.onosproject.yang.gen.v1.openconfigifip.rev20170714.OpenconfigIfIp;
import org.onosproject.yang.gen.v1.openconfigifipext.rev20170714.OpenconfigIfIpExt;
import org.onosproject.yang.gen.v1.openconfigiftypes.rev20161114.OpenconfigIfTypes;
import org.onosproject.yang.gen.v1.openconfiginettypes.rev20170706.OpenconfigInetTypes;
import org.onosproject.yang.gen.v1.openconfiginterfaces.rev20170714.OpenconfigInterfaces;
import org.onosproject.yang.gen.v1.openconfigisis.rev20170726.OpenconfigIsis;
import org.onosproject.yang.gen.v1.openconfigisis.rev20170726.OpenconfigIsisLsp;
import org.onosproject.yang.gen.v1.openconfigisis.rev20170726.OpenconfigIsisRouting;
import org.onosproject.yang.gen.v1.openconfigisislsdbtypes.rev20170726.OpenconfigIsisLsdbTypes;
import org.onosproject.yang.gen.v1.openconfigisispolicy.rev20170726.OpenconfigIsisPolicy;
import org.onosproject.yang.gen.v1.openconfigisistypes.rev20170726.OpenconfigIsisTypes;
import org.onosproject.yang.gen.v1.openconfiglacp.rev20170505.OpenconfigLacp;
import org.onosproject.yang.gen.v1.openconfiglldp.rev20160516.OpenconfigLldp;
import org.onosproject.yang.gen.v1.openconfiglldptypes.rev20160516.OpenconfigLldpTypes;
import org.onosproject.yang.gen.v1.openconfiglocalrouting.rev20170515.OpenconfigLocalRouting;
import org.onosproject.yang.gen.v1.openconfigmodulecatalog.rev20170501.OpenconfigModuleCatalog;
import org.onosproject.yang.gen.v1.openconfigmpls.rev20170621.OpenconfigMpls;
import org.onosproject.yang.gen.v1.openconfigmpls.rev20170621.OpenconfigMplsIgp;
import org.onosproject.yang.gen.v1.openconfigmpls.rev20170621.OpenconfigMplsStatic;
import org.onosproject.yang.gen.v1.openconfigmpls.rev20170621.OpenconfigMplsTe;
import org.onosproject.yang.gen.v1.openconfigmplsldp.rev20170621.OpenconfigMplsLdp;
import org.onosproject.yang.gen.v1.openconfigmplsrsvp.rev20170621.OpenconfigMplsRsvp;
import org.onosproject.yang.gen.v1.openconfigmplssr.rev20170621.OpenconfigMplsSr;
import org.onosproject.yang.gen.v1.openconfigmplstypes.rev20170621.OpenconfigMplsTypes;
import org.onosproject.yang.gen.v1.openconfignetworkinstance.rev20170228.OpenconfigNetworkInstance;
import org.onosproject.yang.gen.v1.openconfignetworkinstance.rev20170228.OpenconfigNetworkInstanceL2;
import org.onosproject.yang.gen.v1.openconfignetworkinstancel3.rev20170228.OpenconfigNetworkInstanceL3;
import org.onosproject.yang.gen.v1.openconfignetworkinstancepolicy.rev20170215.OpenconfigNetworkInstancePolicy;
import org.onosproject.yang.gen.v1.openconfignetworkinstancetypes.rev20170228.OpenconfigNetworkInstanceTypes;
import org.onosproject.yang.gen.v1.openconfigopenflow.rev20170601.OpenconfigOpenflow;
import org.onosproject.yang.gen.v1.openconfigopenflowtypes.rev20170601.OpenconfigOpenflowTypes;
import org.onosproject.yang.gen.v1.openconfigopticalamplifier.rev20170708.OpenconfigOpticalAmplifier;
import org.onosproject.yang.gen.v1.openconfigospfpolicy.rev20160822.OpenconfigOspfPolicy;
import org.onosproject.yang.gen.v1.openconfigospftypes.rev20170228.OpenconfigOspfTypes;
import org.onosproject.yang.gen.v1.openconfigospfv2.rev20170228.OpenconfigOspfv2;
import org.onosproject.yang.gen.v1.openconfigospfv2.rev20170228.OpenconfigOspfv2Area;
import org.onosproject.yang.gen.v1.openconfigospfv2.rev20170228.OpenconfigOspfv2AreaInterface;
import org.onosproject.yang.gen.v1.openconfigospfv2.rev20170228.OpenconfigOspfv2Common;
import org.onosproject.yang.gen.v1.openconfigospfv2.rev20170228.OpenconfigOspfv2Global;
import org.onosproject.yang.gen.v1.openconfigospfv2.rev20170228.OpenconfigOspfv2Lsdb;
import org.onosproject.yang.gen.v1.openconfigpacketmatch.rev20170526.OpenconfigPacketMatch;
import org.onosproject.yang.gen.v1.openconfigpacketmatchtypes.rev20170526.OpenconfigPacketMatchTypes;
import org.onosproject.yang.gen.v1.openconfigplatform.rev20161222.OpenconfigPlatform;
import org.onosproject.yang.gen.v1.openconfigplatformlinecard.rev20170803.OpenconfigPlatformLinecard;
import org.onosproject.yang.gen.v1.openconfigplatformport.rev20161024.OpenconfigPlatformPort;
import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20170708.OpenconfigPlatformTransceiver;
import org.onosproject.yang.gen.v1.openconfigplatformtypes.rev20170816.OpenconfigPlatformTypes;
import org.onosproject.yang.gen.v1.openconfigpolicyforwarding.rev20170621.OpenconfigPfForwardingPolicies;
import org.onosproject.yang.gen.v1.openconfigpolicyforwarding.rev20170621.OpenconfigPfInterfaces;
import org.onosproject.yang.gen.v1.openconfigpolicyforwarding.rev20170621.OpenconfigPfPathGroups;
import org.onosproject.yang.gen.v1.openconfigpolicyforwarding.rev20170621.OpenconfigPolicyForwarding;
import org.onosproject.yang.gen.v1.openconfigpolicytypes.rev20170714.OpenconfigPolicyTypes;
import org.onosproject.yang.gen.v1.openconfigprocmon.rev20170706.OpenconfigProcmon;
import org.onosproject.yang.gen.v1.openconfigrelayagent.rev20160516.OpenconfigRelayAgent;
import org.onosproject.yang.gen.v1.openconfigribbgp.rev20161017.OpenconfigRibBgp;
import org.onosproject.yang.gen.v1.openconfigribbgp.rev20161017.OpenconfigRibBgpAttributes;
import org.onosproject.yang.gen.v1.openconfigribbgp.rev20161017.OpenconfigRibBgpSharedAttributes;
import org.onosproject.yang.gen.v1.openconfigribbgp.rev20161017.OpenconfigRibBgpTableAttributes;
import org.onosproject.yang.gen.v1.openconfigribbgp.rev20161017.OpenconfigRibBgpTables;
import org.onosproject.yang.gen.v1.openconfigribbgpext.rev20161017.OpenconfigRibBgpExt;
import org.onosproject.yang.gen.v1.openconfigribbgptypes.rev20161017.OpenconfigRibBgpTypes;
import org.onosproject.yang.gen.v1.openconfigroutingpolicy.rev20170714.OpenconfigRoutingPolicy;
import org.onosproject.yang.gen.v1.openconfigrsvpsrext.rev20170306.OpenconfigRsvpSrExt;
import org.onosproject.yang.gen.v1.openconfigsegmentrouting.rev20170112.OpenconfigSegmentRouting;
import org.onosproject.yang.gen.v1.openconfigspanningtree.rev20170714.OpenconfigSpanningTree;
import org.onosproject.yang.gen.v1.openconfigspanningtreetypes.rev20170714.OpenconfigSpanningTreeTypes;
import org.onosproject.yang.gen.v1.openconfigsystem.rev20170706.OpenconfigSystem;
import org.onosproject.yang.gen.v1.openconfigsystemlogging.rev20170706.OpenconfigSystemLogging;
import org.onosproject.yang.gen.v1.openconfigsystemterminal.rev20170706.OpenconfigSystemTerminal;
import org.onosproject.yang.gen.v1.openconfigtelemetry.rev20170220.OpenconfigTelemetry;
import org.onosproject.yang.gen.v1.openconfigtelemetrytypes.rev20170220.OpenconfigTelemetryTypes;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.OpenconfigTerminalDevice;
import org.onosproject.yang.gen.v1.openconfigtransportlinecommon.rev20170708.OpenconfigTransportLineCommon;
import org.onosproject.yang.gen.v1.openconfigtransportlineprotection.rev20170708.OpenconfigTransportLineProtection;
import org.onosproject.yang.gen.v1.openconfigtransporttypes.rev20170816.OpenconfigTransportTypes;
import org.onosproject.yang.gen.v1.openconfigtypes.rev20170816.OpenconfigTypes;
import org.onosproject.yang.gen.v1.openconfigvlan.rev20170714.OpenconfigVlan;
import org.onosproject.yang.gen.v1.openconfigvlantypes.rev20170714.OpenconfigVlanTypes;
import org.onosproject.yang.gen.v1.openconfigwavelengthrouter.rev20170708.OpenconfigWavelengthRouter;
import org.onosproject.yang.gen.v1.openconfigyangtypes.rev20170730.OpenconfigYangTypes;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

@Component(immediate = true)
public class OpenConfigModelRegistrator extends AbstractYangModelRegistrator {

    private static final Logger log = getLogger(OpenConfigModelRegistrator.class);

    public OpenConfigModelRegistrator() {
        super(OpenConfigModelRegistrator.class, getAppInfo());
    }


    @SuppressWarnings("checkstyle:MethodLength")
    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {

        return ImmutableMap.<YangModuleId, AppModuleInfo>builder()
                .put(new DefaultYangModuleId("iana-if-type", "2017-03-30"),
                        new DefaultAppModuleInfo(IanaIfType.class, null))
                .put(new DefaultYangModuleId("ietf-inet-types", "2013-07-15"),
                        new DefaultAppModuleInfo(IetfInetTypes.class, null))
                .put(new DefaultYangModuleId("ietf-interfaces", "2014-05-08"),
                        new DefaultAppModuleInfo(IetfInterfaces.class, null))
                .put(new DefaultYangModuleId("openconfig-aaa-radius", "2017-07-06"),
                        new DefaultAppModuleInfo(OpenconfigAaaRadius.class, null))
                .put(new DefaultYangModuleId("openconfig-aaa-tacacs", "2017-07-06"),
                        new DefaultAppModuleInfo(OpenconfigAaaTacacs.class, null))
                .put(new DefaultYangModuleId("openconfig-aaa-types", "2017-07-06"),
                        new DefaultAppModuleInfo(OpenconfigAaaTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-aaa", "2017-07-06"),
                        new DefaultAppModuleInfo(OpenconfigAaa.class, null))
                .put(new DefaultYangModuleId("openconfig-acl", "2017-05-26"),
                        new DefaultAppModuleInfo(OpenconfigAcl.class, null))
                .put(new DefaultYangModuleId("openconfig-aft-common", "2017-05-10"),
                        new DefaultAppModuleInfo(OpenconfigAftCommon.class, null))
                .put(new DefaultYangModuleId("openconfig-aft-ethernet", "2017-05-10"),
                        new DefaultAppModuleInfo(OpenconfigAftEthernet.class, null))
                .put(new DefaultYangModuleId("openconfig-aft-ipv4", "2017-05-10"),
                        new DefaultAppModuleInfo(OpenconfigAftIpv4.class, null))
                .put(new DefaultYangModuleId("openconfig-aft-ipv6", "2017-05-10"),
                        new DefaultAppModuleInfo(OpenconfigAftIpv6.class, null))
                .put(new DefaultYangModuleId("openconfig-aft-mpls", "2017-05-10"),
                        new DefaultAppModuleInfo(OpenconfigAftMpls.class, null))
                .put(new DefaultYangModuleId("openconfig-aft-network-instance", "2017-01-13"),
                        new DefaultAppModuleInfo(OpenconfigAftNetworkInstance.class, null))
                .put(new DefaultYangModuleId("openconfig-aft-pf", "2017-05-10"),
                        new DefaultAppModuleInfo(OpenconfigAftPf.class, null))
                .put(new DefaultYangModuleId("openconfig-aft-types", "2017-05-10"),
                        new DefaultAppModuleInfo(OpenconfigAftTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-aft", "2017-05-10"),
                        new DefaultAppModuleInfo(OpenconfigAft.class, null))
                .put(new DefaultYangModuleId("openconfig-bgp-common-multiprotocol", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigBgpCommonMultiprotocol.class, null))
                .put(new DefaultYangModuleId("openconfig-bgp-common-structure", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigBgpCommonStructure.class, null))
                .put(new DefaultYangModuleId("openconfig-bgp-common", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigBgpCommon.class, null))
                .put(new DefaultYangModuleId("openconfig-bgp-errors", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigBgpErrors.class, null))
                .put(new DefaultYangModuleId("openconfig-bgp-global", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigBgpGlobal.class, null))
                .put(new DefaultYangModuleId("openconfig-bgp-neighbor", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigBgpNeighbor.class, null))
                .put(new DefaultYangModuleId("openconfig-bgp-peer-group", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigBgpPeerGroup.class, null))
                .put(new DefaultYangModuleId("openconfig-bgp-policy", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigBgpPolicy.class, null))
                .put(new DefaultYangModuleId("openconfig-bgp-types", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigBgpTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-bgp", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigBgp.class, null))
                .put(new DefaultYangModuleId("openconfig-catalog-types", "2017-05-01"),
                        new DefaultAppModuleInfo(OpenconfigCatalogTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-channel-monitor", "2017-07-08"),
                        new DefaultAppModuleInfo(OpenconfigChannelMonitor.class, null))
                .put(new DefaultYangModuleId("openconfig-if-aggregate", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigIfAggregate.class, null))
                .put(new DefaultYangModuleId("openconfig-if-ethernet", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigIfEthernet.class, null))
                .put(new DefaultYangModuleId("openconfig-if-ip-ext", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigIfIpExt.class, null))
                .put(new DefaultYangModuleId("openconfig-if-ip", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigIfIp.class, null))
                .put(new DefaultYangModuleId("openconfig-if-types", "2016-11-14"),
                        new DefaultAppModuleInfo(OpenconfigIfTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-inet-types", "2017-07-06"),
                        new DefaultAppModuleInfo(OpenconfigInetTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-isis-lsdb-types", "2017-07-26"),
                        new DefaultAppModuleInfo(OpenconfigIsisLsdbTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-isis-lsp", "2017-07-26"),
                        new DefaultAppModuleInfo(OpenconfigIsisLsp.class, null))
                .put(new DefaultYangModuleId("openconfig-isis-policy", "2017-07-26"),
                        new DefaultAppModuleInfo(OpenconfigIsisPolicy.class, null))
                .put(new DefaultYangModuleId("openconfig-isis-routing", "2017-07-26"),
                        new DefaultAppModuleInfo(OpenconfigIsisRouting.class, null))
                .put(new DefaultYangModuleId("openconfig-isis-types", "2017-07-26"),
                        new DefaultAppModuleInfo(OpenconfigIsisTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-isis", "2017-07-26"),
                        new DefaultAppModuleInfo(OpenconfigIsis.class, null))
                .put(new DefaultYangModuleId("openconfig-lacp", "2017-05-05"),
                        new DefaultAppModuleInfo(OpenconfigLacp.class, null))
                .put(new DefaultYangModuleId("openconfig-lldp-types", "2016-05-16"),
                        new DefaultAppModuleInfo(OpenconfigLldpTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-lldp", "2016-05-16"),
                        new DefaultAppModuleInfo(OpenconfigLldp.class, null))
                .put(new DefaultYangModuleId("openconfig-local-routing", "2017-05-15"),
                        new DefaultAppModuleInfo(OpenconfigLocalRouting.class, null))
                .put(new DefaultYangModuleId("openconfig-module-catalog", "2017-05-01"),
                        new DefaultAppModuleInfo(OpenconfigModuleCatalog.class, null))
                .put(new DefaultYangModuleId("openconfig-mpls-igp", "2017-05-15"),
                        new DefaultAppModuleInfo(OpenconfigMplsIgp.class, null))
                .put(new DefaultYangModuleId("openconfig-mpls-ldp", "2017-05-15"),
                        new DefaultAppModuleInfo(OpenconfigMplsLdp.class, null))
                .put(new DefaultYangModuleId("openconfig-mpls-rsvp", "2017-05-15"),
                        new DefaultAppModuleInfo(OpenconfigMplsRsvp.class, null))
                .put(new DefaultYangModuleId("openconfig-mpls-sr", "2017-05-15"),
                        new DefaultAppModuleInfo(OpenconfigMplsSr.class, null))
                .put(new DefaultYangModuleId("openconfig-mpls-static", "2017-05-15"),
                        new DefaultAppModuleInfo(OpenconfigMplsStatic.class, null))
                .put(new DefaultYangModuleId("openconfig-mpls-te", "2017-05-15"),
                        new DefaultAppModuleInfo(OpenconfigMplsTe.class, null))
                .put(new DefaultYangModuleId("openconfig-mpls-types", "2017-05-15"),
                        new DefaultAppModuleInfo(OpenconfigMplsTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-mpls", "2017-05-15"),
                        new DefaultAppModuleInfo(OpenconfigMpls.class, null))
                .put(new DefaultYangModuleId("openconfig-network-instance-l2", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigNetworkInstanceL2.class, null))
                .put(new DefaultYangModuleId("openconfig-network-instance-l3", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigNetworkInstanceL3.class, null))
                .put(new DefaultYangModuleId("openconfig-network-instance-policy", "2017-02-15"),
                        new DefaultAppModuleInfo(OpenconfigNetworkInstancePolicy.class, null))
                .put(new DefaultYangModuleId("openconfig-network-instance-types", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigNetworkInstanceTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-network-instance", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigNetworkInstance.class, null))
                .put(new DefaultYangModuleId("openconfig-openflow-types", "2017-06-01"),
                        new DefaultAppModuleInfo(OpenconfigOpenflowTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-openflow", "2017-06-01"),
                        new DefaultAppModuleInfo(OpenconfigOpenflow.class, null))
                .put(new DefaultYangModuleId("openconfig-optical-amplifier", "2017-07-08"),
                        new DefaultAppModuleInfo(OpenconfigOpticalAmplifier.class, null))
                .put(new DefaultYangModuleId("openconfig-ospf-policy", "2016-08-22"),
                        new DefaultAppModuleInfo(OpenconfigOspfPolicy.class, null))
                .put(new DefaultYangModuleId("openconfig-ospf-types", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigOspfTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-ospfv2-area-interface", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigOspfv2AreaInterface.class, null))
                .put(new DefaultYangModuleId("openconfig-ospfv2-area", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigOspfv2Area.class, null))
                .put(new DefaultYangModuleId("openconfig-ospfv2-common", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigOspfv2Common.class, null))
                .put(new DefaultYangModuleId("openconfig-ospfv2-global", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigOspfv2Global.class, null))
                .put(new DefaultYangModuleId("openconfig-ospfv2-lsdb", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigOspfv2Lsdb.class, null))
                .put(new DefaultYangModuleId("openconfig-ospfv2", "2017-02-28"),
                        new DefaultAppModuleInfo(OpenconfigOspfv2.class, null))
                .put(new DefaultYangModuleId("openconfig-packet-match-types", "2017-05-26"),
                        new DefaultAppModuleInfo(OpenconfigPacketMatchTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-packet-match", "2017-05-26"),
                        new DefaultAppModuleInfo(OpenconfigPacketMatch.class, null))
                .put(new DefaultYangModuleId("openconfig-pf-forwarding-policies", "2017-06-21"),
                        new DefaultAppModuleInfo(OpenconfigPfForwardingPolicies.class, null))
                .put(new DefaultYangModuleId("openconfig-pf-interfaces", "2017-06-21"),
                        new DefaultAppModuleInfo(OpenconfigPfInterfaces.class, null))
                .put(new DefaultYangModuleId("openconfig-pf-path-groups", "2017-06-21"),
                        new DefaultAppModuleInfo(OpenconfigPfPathGroups.class, null))
                .put(new DefaultYangModuleId("openconfig-platform-types", "2017-08-16"),
                        new DefaultAppModuleInfo(OpenconfigPlatformTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-policy-forwarding", "2017-06-21"),
                        new DefaultAppModuleInfo(OpenconfigPolicyForwarding.class, null))
                .put(new DefaultYangModuleId("openconfig-policy-types", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigPolicyTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-procmon", "2017-07-06"),
                        new DefaultAppModuleInfo(OpenconfigProcmon.class, null))
                .put(new DefaultYangModuleId("openconfig-relay-agent", "2016-05-16"),
                        new DefaultAppModuleInfo(OpenconfigRelayAgent.class, null))
                .put(new DefaultYangModuleId("openconfig-rib-bgp-attributes", "2016-10-17"),
                        new DefaultAppModuleInfo(OpenconfigRibBgpAttributes.class, null))
                .put(new DefaultYangModuleId("openconfig-rib-bgp-ext", "2016-10-17"),
                        new DefaultAppModuleInfo(OpenconfigRibBgpExt.class, null))
                .put(new DefaultYangModuleId("openconfig-rib-bgp-shared-attributes", "2016-10-17"),
                        new DefaultAppModuleInfo(OpenconfigRibBgpSharedAttributes.class, null))
                .put(new DefaultYangModuleId("openconfig-rib-bgp-table-attributes", "2016-10-17"),
                        new DefaultAppModuleInfo(OpenconfigRibBgpTableAttributes.class, null))
                .put(new DefaultYangModuleId("openconfig-rib-bgp-tables", "2016-10-17"),
                        new DefaultAppModuleInfo(OpenconfigRibBgpTables.class, null))
                .put(new DefaultYangModuleId("openconfig-rib-bgp-types", "2016-10-17"),
                        new DefaultAppModuleInfo(OpenconfigRibBgpTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-rib-bgp", "2016-10-17"),
                        new DefaultAppModuleInfo(OpenconfigRibBgp.class, null))
                .put(new DefaultYangModuleId("openconfig-routing-policy", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigRoutingPolicy.class, null))
                .put(new DefaultYangModuleId("openconfig-rsvp-sr-ext", "2017-03-06"),
                        new DefaultAppModuleInfo(OpenconfigRsvpSrExt.class, null))
                .put(new DefaultYangModuleId("openconfig-segment-routing", "2017-01-12"),
                        new DefaultAppModuleInfo(OpenconfigSegmentRouting.class, null))
                .put(new DefaultYangModuleId("openconfig-spanning-tree-types", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigSpanningTreeTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-spanning-tree", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigSpanningTree.class, null))
                .put(new DefaultYangModuleId("openconfig-system-logging", "2017-07-06"),
                        new DefaultAppModuleInfo(OpenconfigSystemLogging.class, null))
                .put(new DefaultYangModuleId("openconfig-system-terminal", "2017-07-06"),
                        new DefaultAppModuleInfo(OpenconfigSystemTerminal.class, null))
                .put(new DefaultYangModuleId("openconfig-system", "2017-07-06"),
                        new DefaultAppModuleInfo(OpenconfigSystem.class, null))
                .put(new DefaultYangModuleId("openconfig-telemetry-types", "2017-02-20"),
                        new DefaultAppModuleInfo(OpenconfigTelemetryTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-telemetry", "2017-02-20"),
                        new DefaultAppModuleInfo(OpenconfigTelemetry.class, null))
                .put(new DefaultYangModuleId("openconfig-transport-line-protection", "2017-07-08"),
                        new DefaultAppModuleInfo(OpenconfigTransportLineProtection.class, null))
                .put(new DefaultYangModuleId("openconfig-vlan-types", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigVlanTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-vlan", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigVlan.class, null))
                .put(new DefaultYangModuleId("openconfig-wavelength-router", "2017-07-08"),
                        new DefaultAppModuleInfo(OpenconfigWavelengthRouter.class, null))
                .put(new DefaultYangModuleId("openconfig-yang-types", "2017-07-30"),
                        new DefaultAppModuleInfo(OpenconfigYangTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-platform", "2016-12-22"),
                        new DefaultAppModuleInfo(OpenconfigPlatform.class, null))
                .put(new DefaultYangModuleId("openconfig-interfaces", "2017-07-14"),
                        new DefaultAppModuleInfo(OpenconfigInterfaces.class, null))
                .put(new DefaultYangModuleId("openconfig-transport-types", "2017-08-16"),
                        new DefaultAppModuleInfo(OpenconfigTransportTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-types", "2017-08-16"),
                        new DefaultAppModuleInfo(OpenconfigTypes.class, null))
//                .put(new DefaultYangModuleId("openconfig-extensions", "2017-08-16"),
//                     new DefaultAppModuleInfo(OpenconfigEx.class, null))
                .put(new DefaultYangModuleId("ietf-yang-types", "2013-07-15"),
                        new DefaultAppModuleInfo(IetfYangTypes.class, null))

                // minimum required for the example
                .put(new DefaultYangModuleId("openconfig-platform-linecard", "2017-08-03"),
                        new DefaultAppModuleInfo(OpenconfigPlatformLinecard.class, null))
                .put(new DefaultYangModuleId("openconfig-platform-port", "2016-10-24"),
                        new DefaultAppModuleInfo(OpenconfigPlatformPort.class, null))
                .put(new DefaultYangModuleId("openconfig-platform-transceiver", "2017-07-08"),
                        new DefaultAppModuleInfo(OpenconfigPlatformTransceiver.class, null))
                .put(new DefaultYangModuleId("openconfig-transport-line-common", "2017-07-08"),
                        new DefaultAppModuleInfo(OpenconfigTransportLineCommon.class, null))
                .put(new DefaultYangModuleId("openconfig-terminal-device", "2017-07-08"),
                        new DefaultAppModuleInfo(OpenconfigTerminalDevice.class, null))
                .build();
    }
}
