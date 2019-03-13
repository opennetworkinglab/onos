package org.ctpd.closfwd;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.ctpd.closfwd.Endpoint;
import org.ctpd.closfwd.ClosDeviceService;
import org.ctpd.closfwd.VpdcHostEndpoint;
import org.ctpd.closfwd.ServiceEndpoint;
import org.ctpd.closfwd.ClientServiceBypassEndpoint;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.ConnectPoint;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FlowDriverOvs extends FlowDriver {

    private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");

    private static ServiceDirectory services = new DefaultServiceDirectory();

    public static <T> T get(Class<T> serviceClass) {
        return services.get(serviceClass);
    }

    public FlowDriverOvs(){}


    @Override
    public  void installL1Flows(Endpoint endpoint, boolean create){
    }

    @Override
    public  void installL2L3Flows(Endpoint endpoint, DeviceId deviceId, boolean create){
    }

    @Override
    public void installL4Flows(Endpoint endpoint, DeviceId deviceId, boolean create){
    }

    @Override
    public void installSpineFlows(Endpoint endpoint,  DeviceId deviceId, boolean create){
    }

    @Override
    public void createIntent(Endpoint ingressEndpoint, Endpoint egressEndpoint, boolean create){
    }

    @Override
    public void installBypassTemporaryFlows(ClientServiceBypassEndpoint clientEndpoint, ServiceEndpoint serviceEndpoint, MacAddress macAddress, IpPrefix ipPrefix, boolean create){
    }

//     @Override
//     public ObjectiveError installSpineRules(DeviceId deviceId, PortNumber inputPort, PortNumber outputPort, VlanId innerVlan, VlanId vlan, IpPrefix ipSrc, IpPrefix ipDst, int typeOfPath, MacAddress srcMac, MacAddress dstMac, VlanId extraVlan, boolean create, String pathId, MacAddress hostSrcMac,MacAddress hostDstMac) {

//         ClosDeviceService service = get(ClosDeviceService.class);
//         FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();

//         //ObjectiveError error = null;
//         ObjectiveError returnedError = null;
//         //int nextId;
//         VlanId emptyVlanId = VlanId.vlanId((short) service.getEmptyVlanIdP());
//         //List<Integer> nextIds = new ArrayList<Integer>();

//         /*Pedro*/
//         String ctpdFakeInternalMacAddress = service.getCtpdFakeInternalMacAddress();
//         String ctpdFakeExternalMacAddress = service.getCtpdFakeExternalMacAddress();
//         boolean monoetiqueta = service.getMonoetiqueta();

//         if(typeOfPath == PathType.OLT_VOLT) {
//         log.debug("[spine] OLT to VOLT ");
//             /*  VOLT to OLT */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     .matchEthDst(srcMac)
//                     //.matchVlanId(vlan);
//                     .matchVlanId(VlanId.NONE);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder().setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, makeSpecificForwardingObjective(selector, treatment, 40001, create));

//             /* IP client to Portal*/
//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     .matchEthDst(dstMac)
//                     //.matchVlanId(vlan);
//                     .matchVlanId(VlanId.NONE);

//             treatment = DefaultTrafficTreatment.builder().setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, makeSpecificForwardingObjective(selector, treatment, 40001, create));


//             String installed = String.format(
//                     "[installRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);
//             log.debug(installed);


//         } else if(typeOfPath == PathType.CLIENT_SERVICE_INT) {

//             /*  Client to INT Service */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     //.matchEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst)
//                     .matchIPv6Src(ipSrc)
//                     .matchVlanId(extraVlan);


//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setOutput(outputPort)
//                     .setVlanId(extraVlan);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*  INT Service to Client  */
//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     //.matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchVlanId(extraVlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipSrc)
//                     .matchIPv6Src(ipDst);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));


//             String installed = String.format(
//                     "[installRules] rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);
//             log.debug(installed);

//         } else if (typeOfPath == PathType.OLT_VPDC) {
//             log.debug("[spine] OLT to VPDC ");

//             /* VPDC to OLT */
//             /*  IP VPDC to Client */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     //.matchEthSrc(srcMac)
//                     .matchVlanId(vlan); // vlan = srcHost.getVlan

//         //     if (!monoetiqueta && innerVlan != null && !innerVlan.equals(VlanId.vlanId(VlanId.UNTAGGED)))
//         //         selector.matchInnerVlanId(innerVlan);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, 40001, create));

//             /* Multicast Support */

//         //     /* NDP client to VPDC */
//         //     MacAddress multicastNDMac = service.getNDMacAddressFromIpAddress(ipSrc.address());
//         //     selector = DefaultTrafficSelector.builder()
//         //             .matchInPort(outputPort)
//         //             .matchEthDst(multicastNDMac)
//         //             .matchVlanId(vlan);

//         //     if (!monoetiqueta && innerVlan != null && !innerVlan.equals(VlanId.vlanId(VlanId.UNTAGGED)))
//         //         selector.matchInnerVlanId(innerVlan);

//         //     treatment = DefaultTrafficTreatment.builder().setOutput(inputPort);

//         //     flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, 40001, create));

//         //     /*	DHCP Client to Vpdc */
//         //     MacAddress multicastDHCPMac = MacAddress.valueOf("33:33:00:01:00:02");
//         //     selector = DefaultTrafficSelector.builder()
//         //             .matchInPort(outputPort)
//         //             .matchEthDst(multicastDHCPMac)
//         //             .matchVlanId(vlan);

//         //     if (!monoetiqueta && innerVlan != null && !innerVlan.equals(VlanId.vlanId(VlanId.UNTAGGED)))
//         //         selector.matchInnerVlanId(innerVlan);

//         //     treatment = DefaultTrafficTreatment.builder().setOutput(inputPort);

//         //     flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, 40001, create));

//         //     /*	DHCPv4 Client to Vpdc */
//         //     MacAddress broadcastMac = MacAddress.BROADCAST;
//         //     selector = DefaultTrafficSelector.builder()
//         //             .matchInPort(outputPort)
//         //             .matchEthDst(broadcastMac)
//         //             .matchVlanId(vlan);

//         //     if (!monoetiqueta && innerVlan != null && !innerVlan.equals(VlanId.vlanId(VlanId.UNTAGGED)))
//         //         selector.matchInnerVlanId(innerVlan);

//         //     treatment = DefaultTrafficTreatment.builder().setOutput(inputPort);

//         //     flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, 40001, create));

//         //     /*	Router Solicitation Client to Vpdc*/
//         //     MacAddress multicastRSMac = MacAddress.valueOf("33:33:00:00:00:02");
//         //     selector = DefaultTrafficSelector.builder()
//         //             .matchInPort(outputPort)
//         //             .matchEthDst(multicastRSMac)
//         //             .matchVlanId(vlan);

//         //     if (!monoetiqueta && innerVlan != null && !innerVlan.equals(VlanId.vlanId(VlanId.UNTAGGED)))
//         //         selector.matchInnerVlanId(innerVlan);

//         //     treatment = DefaultTrafficTreatment.builder().setOutput(inputPort);

//         //     flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, 40001, create));


//             /* OLT to VPDC */
//             /* IP Client to VPDC */
//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     //.matchEthDst(dstMac)
//                     .matchVlanId(vlan);

//         //     if (!monoetiqueta && innerVlan != null && !innerVlan.equals(VlanId.vlanId(VlanId.UNTAGGED)))
//         //         selector.matchInnerVlanId(innerVlan);

//             treatment = DefaultTrafficTreatment.builder().setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, 40001, create));

//         //     /*  Link-local NDP */
//         //     IpAddress ipAddress = IpAddress.valueOf(IpAddress.Version.INET6, getLinkLocalAddress(dstMac.toBytes()));
//         //     MacAddress multicastNDPLinkLocal = service.getNDMacAddressFromIpAddress(ipAddress);

//         //     selector = DefaultTrafficSelector.builder()
//         //             .matchInPort(outputPort)
//         //             .matchEthDst(multicastNDPLinkLocal)
//         //             .matchVlanId(vlan);

//         //     if (!monoetiqueta && innerVlan != null && !innerVlan.equals(VlanId.vlanId(VlanId.UNTAGGED)))
//         //         selector.matchInnerVlanId(innerVlan);

//         //     treatment = DefaultTrafficTreatment.builder().setOutput(inputPort);

//         //     flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, 40001, create));

//             String installed = String.format(
//                     "[installRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);
//             log.debug(installed);

//         }
//         else if(typeOfPath==PathType.VPDC_SERVICE_EXT){
//             log.debug("[spine] VPDC to SERVICE EXT ");

//             /*  VPDC to External Service */

//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     //.matchInPort(inputPort)
//                     .matchEthDst(dstMac)
//                     .matchVlanId(vlan);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder().setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*  IP External Service to  VPDC */

//             selector = DefaultTrafficSelector.builder()
//                     //.matchInPort(outputPort)
//                     .matchEthDst(hostSrcMac)
//                     .matchVlanId(vlan);

//             treatment = DefaultTrafficTreatment.builder().setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if (typeOfPath == PathType.VPDC_SERVICE_INT || typeOfPath == PathType.VPDC_SERVICE_INT_VLAN) {
//             log.debug("[spine] VPDC to SERVICE INT or SERVICE INT VLAN ");
//             /*  VPDC to INT Service */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     //.matchInPort(inputPort)
//                     .matchVlanId(vlan)
//                     .matchEthDst(hostDstMac);


//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*  INT Service to VPDC  */
//             selector = DefaultTrafficSelector.builder()
//                     //.matchInPort(outputPort)
//                     .matchEthDst(hostSrcMac)
//                     .matchVlanId(vlan);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         }
//         else if (typeOfPath == PathType.SERVICE_INT_SERVICE_EXT)
//         {
//             log.debug("[spine] SERVICE INT to SERVICE EXT ");
//             /*  IP Service int to Service ext (including NDP)*/

//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchEthDst(dstMac)
//                     .matchVlanId(vlan);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP service ext to service int (including NDP)*/

//             selector = DefaultTrafficSelector.builder()
//                     .matchEthDst(hostSrcMac)
//                     .matchVlanId(vlan);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath == PathType.SERVICE_INT_SERVICE_INT) {
//             log.debug("[spine] SERVICE INT to SERVICE INT ");
//             /*	VMNBX to Service */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchEthDst(hostDstMac)
//                     .matchVlanId(vlan);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder().setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*	Service to VMNBX */
//             selector = DefaultTrafficSelector.builder()
//                     .matchEthDst(hostSrcMac)
//                     .matchVlanId(vlan);

//             treatment = DefaultTrafficTreatment.builder().setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                         "[installRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                         srcMac, outputPort, dstMac, inputPort);
//                 log.debug(installed);


//         } else if(typeOfPath == PathType.SERVICE_INT_ROUTER || typeOfPath == PathType.SERVICE_INT_ROUTER_VLAN) {
//             log.debug("[spine] SERVICE INT to ROUTER or ROUTER VLAN");
//             /*	BGP Speaker to Router */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .matchVlanId(vlan);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder().setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*	Router to BGP Speaker */
//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     .matchEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchVlanId(vlan);

//             treatment = DefaultTrafficTreatment.builder().setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                         "[installRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                         srcMac, outputPort, dstMac, inputPort);
//                 log.debug(installed);

//         } else {
//             log.warn("No use case match in installRules method in Spine");
//         }

//         return returnedError;

//     }

//     @Override
//     public ObjectiveError installFirstLeafRules(DeviceId deviceId, PortNumber inputPort, PortNumber outputPort, VlanId innerVlan,  VlanId vlan, IpPrefix ipSrc, IpPrefix ipDst, IpPrefix clientPrefix, int typeOfPath, MacAddress srcMac, MacAddress dstMac, VlanId extraVlan, boolean keepExternalVlan, boolean create, String pathId, MacAddress hostSrcMac,MacAddress hostDstMac){

//         ClosDeviceService service = get(ClosDeviceService.class);
//         FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();

//         //ObjectiveError error = null;
//         ObjectiveError returnedError = null;
//         //int nextId;
//         VlanId emptyVlanId = VlanId.vlanId((short)service.getEmptyVlanIdP());
//         //List<Integer> nextIds = new ArrayList<Integer>();

//         /*Pedro*/
//         String ctpdFakeInternalMacAddress = service.getCtpdFakeInternalMacAddress();
//         String ctpdFakeExternalMacAddress = service.getCtpdFakeExternalMacAddress();
//         int internetServicesFlowPriority = service.getInternetServicesFlowPriority();
//         int defaultVlan = service.getDefaultVlan();

//         if(typeOfPath == PathType.OLT_VOLT) {
//                 log.debug("[firstleaf] OLT to VOLT ");

//                 /*  VOLT to OLT */
//                 TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                         .matchInPort(outputPort)
//                         .matchEthDst(srcMac)
//                         .matchVlanId(vlan);

//                 TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                         .setOutput(inputPort)
//                         .popVlan();

//                 flowObjectiveService.forward(deviceId, makeSpecificForwardingObjective(selector, treatment, 40001, create));

//                 /* OLT to VOLT */
//                 selector = DefaultTrafficSelector.builder()
//                         .matchInPort(inputPort)
//                         .matchEthDst(dstMac)
//                         .matchVlanId(vlan);

//                 treatment = DefaultTrafficTreatment.builder()
//                         .setOutput(outputPort);

//                 flowObjectiveService.forward(deviceId, makeSpecificForwardingObjective(selector, treatment, 40001, create));


//                 String installed = String.format(
//                         "[installRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                         srcMac, outputPort, dstMac, inputPort);
//                 log.debug(installed);


//             } else if(typeOfPath==PathType.VPDC_SERVICE_INT) {
//                 log.debug("[firstleaf] VPDC to SERVICE INT ");
//             /*  IP VPDC to INT Service */

//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchEthSrc(srcMac)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst)
//                     .matchVlanId(VlanId.NONE);


//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthDst(hostDstMac)
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .pushVlan()
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);


//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP INT Service to VPDC */
//             treatment = DefaultTrafficTreatment.builder()
//             //.setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//             .setEthDst(srcMac)
//             .popVlan()
//             .setOutput(inputPort);

//             IpPrefix servicePrefix = null;
//             if (ipDst.prefixLength()>64)
//                 servicePrefix = IpPrefix.valueOf(ipDst.address(), 64);
//             else
//                 servicePrefix = ipDst;

//             /* IP INT Service to VPDC Using client prefix */
//             selector = DefaultTrafficSelector.builder()
//                     .matchEthDst(hostSrcMac)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(clientPrefix)
//                     .matchIPv6Src(servicePrefix);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP INT Service to VPDC vpdc service side prefix */
//             selector = DefaultTrafficSelector.builder()
//                     .matchEthDst(hostSrcMac)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipSrc)
//                     .matchIPv6Src(servicePrefix)
// ;

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));


//             /* IP INT Service to VPDC */

// //            selector = DefaultTrafficSelector.builder()
// //                /*Pedro*/ //.matchInPort(outputPort)
// //                    .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
// //                    .matchVlanId(vlan)
// //                    .matchEthType((short)0x86dd)
// //                    .matchIPv6Dst(clientPrefix);
// //
// //            treatment = DefaultTrafficTreatment.builder()
// //                    .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
// //                    .setEthDst(dstMac)
// //                    .popVlan()
// //                    .setOutput(inputPort);
// //
// //            flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath==PathType.VPDC_SERVICE_INT_VLAN) {
//             log.debug("[firstleaf] VPDC to SERVICE INT VLAN ");

//             /*  IP VPDC to INT Service, Vlan goes from Vpdc*/

//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     .matchEthSrc(srcMac)
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchVlanId(vlan);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);


//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));


//             /* IP INT Service to VPDC */
//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             /* IP INT Service to VPDC, using Vpdc Service side IP, do not popVlan as we want it to be received at Vpdc */

//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipSrc);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP INT Service to VPDC, using Vpdc Client side IP, do not popVlan as we want it to be received at Vpdc */

//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(clientPrefix);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath==PathType.VPDC_SERVICE_EXT) {
//             log.debug("[firstleaf] VPDC to SERVICE EXT ");

//             /*  VPDC to EXT Service */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     //.matchInPort(inputPort)
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .matchEthSrc(srcMac)
//                     .matchVlanId(VlanId.NONE);
//             //.matchIPv6Dst(IpPrefix.valueOf(ipDst, 128));

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setEthDst(dstMac)
//                     .pushVlan()
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*  EXT Service to  VPDC by looking Client Ip*/

//             selector = DefaultTrafficSelector.builder()
//                     .matchEthDst(hostSrcMac)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(clientPrefix);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthDst(srcMac)
//                     .popVlan()
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*  EXT Service to  VPDC by looking Vpdc Ip*/

// //            selector = DefaultTrafficSelector.builder()
// //                    .matchInPort(outputPort)
// //                    .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                    .matchVlanId(vlan)
// //                    .matchEthType((short)0x86dd)
// //                    .matchIPv6Dst(ipSrc);
// //
// //            treatment = DefaultTrafficTreatment.builder()
// //                    .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                    .setEthDst(dstMac)
// //                    .popVlan()
// //                    .setOutput(inputPort);
// //
// //            flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         }
//         else if (typeOfPath == PathType.SERVICE_INT_SERVICE_EXT) {
//             log.debug("[firstleaf] SERVICE INT to SERVICE EXT ");

//            /*  IP Service int to Service ext (including NDP)*/

//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     //.matchInPort(inputPort)
//                     .matchEthSrc(srcMac)
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     //.matchIPv6Dst(ipDst)
//                     .matchVlanId(vlan);


//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthDst(dstMac)
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setOutput(outputPort)
//                     .setVlanId(vlan);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));

// //            if (keepExternalVlan) {
// //                treatment = DefaultTrafficTreatment.builder()
// //                        .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                        .setEthDst(dstMac)
// //                        .setOutput(outputPort);
// //
// //                flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
// //            } else {
// //                treatment = DefaultTrafficTreatment.builder()
// //                        .setVlanId(VlanId.vlanId((short) defaultVlan))
// //                        .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                        .setEthDst(dstMac)
// //                        .setOutput(outputPort);
// //
// //              flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));
// //            }

//             /* IP service ext to service int (including NDP)*/
//             if(ipSrc.isIp6()) {
//                 selector = DefaultTrafficSelector.builder()
//                         .matchEthDst(hostSrcMac)
//                         .matchEthType((short)0x86dd)
//                         .matchIPv6Dst(ipSrc)
//                         .matchVlanId(vlan);
//             }
//             else{
//                 selector = DefaultTrafficSelector.builder()
//                         .matchEthDst(hostSrcMac)
//                         .matchIPDst(ipSrc)
//                         .matchEthType((short)0x0800)
//                         .matchVlanId(vlan);
//             }

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthDst(srcMac)
//                     //.setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

// //            if (keepExternalVlan) {
// //                if (ipDst.isIp6()) {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(vlan)
// //                            .matchEthType((short) 0x86dd)
// //                            .matchIPv6Dst(ipSrc);
// //                } else {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(vlan)
// //                            .matchEthType((short) 0x0800)
// //                            .matchIPv6Dst(ipSrc);
// //                }
// //
// //                treatment = DefaultTrafficTreatment.builder()
// //                        .setEthDst(dstMac)
// //                        .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                        .setOutput(inputPort);
// //
// //                flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
// //            } else {
// //                if (ipDst.isIp6()) {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(VlanId.vlanId((short) defaultVlan))
// //                            .matchEthType((short) 0x86dd)
// //                            .matchIPv6Dst(ipSrc);
// //                } else {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(VlanId.vlanId((short) defaultVlan))
// //                            .matchEthType((short) 0x0800)
// //                            .matchIPv6Dst(ipSrc);
// //                }
// //
// //                treatment = DefaultTrafficTreatment.builder()
// //                        .setEthDst(dstMac)
// //                        .setVlanId(vlan)
// //                        .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                        .setOutput(inputPort);
// //
// //                flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));
// //            }
// //
// //            if (clientPrefix != null) {
// //                /* IP service ext to service int using the Client IP prefix (including NDP)*/
// //                if (keepExternalVlan) {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(vlan)
// //                            .matchEthType((short)0x86dd)
// //                            .matchIPv6Dst(clientPrefix);
// //
// //                    treatment = DefaultTrafficTreatment.builder()
// //                            .setEthDst(dstMac)
// //                            .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                            .setOutput(inputPort);
// //
// //                    flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
// //                } else {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(VlanId.vlanId((short) defaultVlan))
// //                            .matchEthType((short)0x86dd)
// //                            .matchIPv6Dst(clientPrefix);
// //
// //                    treatment = DefaultTrafficTreatment.builder()
// //                            .setEthDst(dstMac)
// //                            .setVlanId(vlan)
// //                            .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                            .setOutput(inputPort);
// //
// //                    flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));
// //                }
// //            }

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath == PathType.SERVICE_INT_SERVICE_INT) {
//             /*	VMNBX to Service */
//             TrafficSelector.Builder selector;
//             log.debug("[firstleaf] SERVICE INT to SERVICE INT ");

//             if (ipDst.isIp6()) {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(inputPort)
//                         .matchEthSrc(srcMac)
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                         .matchEthType((short)0x86dd)
//                         .matchIPv6Dst(ipDst)
//                         .matchVlanId(vlan);
//             } else {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(inputPort)
//                         .matchEthSrc(srcMac)
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                         .matchEthType((short)0x0800)
//                         .matchIPDst(ipDst)
//                         .matchVlanId(vlan);
//             }

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(hostDstMac)
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*	Service to VMNBX  */
//             if (ipSrc.isIp6()) {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(outputPort)
//                         .matchEthDst(hostSrcMac)
//                         .matchVlanId(vlan)
//                         .matchEthType((short)0x86dd)
//                         .matchIPv6Dst(ipSrc);
//             } else {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(outputPort)
//                         .matchEthDst(hostSrcMac)
//                         .matchVlanId(vlan)
//                         .matchEthType((short)0x0800)
//                         .matchIPDst(ipSrc);
//             }
//             treatment = DefaultTrafficTreatment.builder()
//                         .setEthDst(srcMac)
//                         .setVlanId(vlan)
//                         .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath == PathType.SERVICE_INT_ROUTER) {
//             log.debug("[firstleaf] SERVIE INT to ROUTER ");

//             /* Router to BGP Speaker */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     //.matchVlanId(VlanId.NONE)
//                     .matchVlanId(VlanId.vlanId((short) defaultVlan))
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setEthDst(srcMac)
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));

//             /*  BGP Speaker to Router */
//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(VlanId.vlanId((short) defaultVlan))
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath == PathType.SERVICE_INT_ROUTER_VLAN) {
//             log.debug("[firstleaf] SERVICE INT to ROUTER VLAN ");

//             /* Router to BGP Speaker */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setEthDst(srcMac)
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*  BGP Speaker to Router */
//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Src(ipDst);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);


//         } else if (typeOfPath == PathType.CLIENT_SERVICE_INT) {
//             log.debug("[firstleaf] CLIENT to SERVICE INT ");
//             /*  IP Client to INT Service */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst)
//                     .matchIPv6Src(ipSrc);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(extraVlan)
//                     .setOutput(outputPort);


//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP INT Service to Client */
//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     // .matchVlanId(vlanIdMac.vlan)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPDst(ipSrc)
//                     .matchIPSrc(ipDst);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(srcMac)
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else {
//             log.warn("No use case match in installLeafRules method in First");

//         }

//         return returnedError;
//     }

//     @Override
//     public ObjectiveError installLastLeafRules(DeviceId deviceId, PortNumber inputPort, PortNumber outputPort, VlanId innerVlan, VlanId vlan, IpPrefix ipSrc, IpPrefix ipDst, IpPrefix clientPrefix, int typeOfPath, MacAddress srcMac, MacAddress dstMac, VlanId extraVlan, boolean keepExternalVlan, boolean create, String pathId, UUID hostId, MacAddress hostSrcMac,MacAddress hostDstMac){

//         ClosDeviceService service = get(ClosDeviceService.class);
//         FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();

//         //ObjectiveError error = null;
//         ObjectiveError returnedError = null;
//         //int nextId;
//         VlanId emptyVlanId = VlanId.vlanId((short) service.getEmptyVlanIdP());
//         //List<Integer> nextIds = new ArrayList<Integer>();

//         /*Pedro*/
//         String ctpdFakeInternalMacAddress = service.getCtpdFakeInternalMacAddress();
//         String ctpdFakeExternalMacAddress = service.getCtpdFakeExternalMacAddress();
//         int internetServicesFlowPriority = service.getInternetServicesFlowPriority();
//         int defaultVlan = service.getDefaultVlan();


//         if(typeOfPath == PathType.OLT_VOLT) {
//                 log.debug("[lastleaf] OLT to VOLT ");
//                 /*  VOLT to OLT */
//                 TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                         .matchInPort(outputPort)
//                         .matchEthDst(srcMac)
//                         .matchVlanId(vlan);

//                 TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                         .setOutput(inputPort)
//                         .popVlan();

//                 flowObjectiveService.forward(deviceId, makeSpecificForwardingObjective(selector, treatment, 40001, create));

//                 /* OLT to VOLT */
//                 selector = DefaultTrafficSelector.builder()
//                         .matchInPort(inputPort)
//                         .matchEthDst(dstMac)
//                         .matchVlanId(vlan);

//                 treatment = DefaultTrafficTreatment.builder()
//                         .setOutput(outputPort);

//                 flowObjectiveService.forward(deviceId, makeSpecificForwardingObjective(selector, treatment, 40001, create));


//                 String installed = String.format(
//                         "[installRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                         srcMac, outputPort, dstMac, inputPort);
//                 log.debug(installed);


//             } else if(typeOfPath==PathType.VPDC_SERVICE_INT) {
//             log.debug("[lastleaf] VPDC to SERVICE_INT ");
//             /*  IP VPDC to INT Service */

//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     //.matchInPort(inputPort)
//                     //.matchEthSrc(srcMac)
//                     .matchEthDst(hostDstMac)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     //.setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);


//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP INT Service to VPDC */

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(hostSrcMac)
//                     .setVlanId(vlan)
//                     //.popVlan()
//                     .setOutput(inputPort);

//             ConsistentMap<UUID, Endpoint> registry = service.getRegistry();

//             VpdcHost vpdcHost = (VpdcHost) registry.get(hostId).value();

//             if(vpdcHost != null) {
//                 for (IpPrefix ipPrefix: vpdcHost.ipList) {

//                 /* IP INT Service to VPDC  Host Prefix*/
//                     selector = DefaultTrafficSelector.builder()
//                             //.matchInPort(outputPort)
//                             .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                             .matchVlanId(vlan)
//                             .matchEthType((short) 0x86dd)
//                             .matchIPv6Dst(ipPrefix);

//                     flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//                 /* IP INT Service to VPDC Using vpdc service side prefix */
//                     selector = DefaultTrafficSelector.builder()
//                             //.matchInPort(outputPort)
//                             .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                             .matchVlanId(vlan)
//                             .matchEthType((short) 0x86dd)
//                             .matchIPv6Dst(ipSrc);

//                     flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
//                 }
//             }

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath==PathType.VPDC_SERVICE_INT_VLAN) {
//             log.debug("[lastleaf] VPDC to SERVICE INT VLAN ");

//             /*  IP VPDC to INT Service, Vlan goes from Vpdc*/

//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     .matchEthSrc(srcMac)
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchVlanId(vlan);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);


//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP INT Service to VPDC */
//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             /* IP INT Service to VPDC, using Vpdc Service side IP, do not popVlan as we want it to be received at Vpdc */

//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipSrc);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP INT Service to VPDC, using Vpdc Client side IP, do not popVlan as we want it to be received at Vpdc */

//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(clientPrefix);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath==PathType.VPDC_SERVICE_EXT) {
//             log.debug("[lastleaf] VPDC to SERVICE EXT ");

//             /*  VPDC to EXT Service */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     //.matchInPort(inputPort)
//                     //.matchEthSrc(srcMac)
//                     .matchEthDst(dstMac)
//                     .matchVlanId(vlan);
//             //.matchIPv6Dst(IpPrefix.valueOf(ipDst, 128));

//             TrafficTreatment.Builder treatment;
//             if(keepExternalVlan) {
//                 treatment = DefaultTrafficTreatment.builder()
//                         .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                         .setVlanId(vlan)
//                         .setOutput(outputPort);
//             }
//             else{
//                 treatment = DefaultTrafficTreatment.builder()
//                         //.pushVlan()
//                         .setVlanId(VlanId.vlanId((short) defaultVlan))
//                         .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                         .setOutput(outputPort);
//             }

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*  EXT Service to  VPDC by looking Client Ip*/


//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setEthDst(hostSrcMac)
//                     .setVlanId(vlan)
//                     //.popVlan()
//                     .setOutput(inputPort);

//             ConsistentMap<UUID, Endpoint> registry = service.getRegistry();

//             VpdcHost vpdcHost = (VpdcHost) registry.get(hostId).value();

//             if (vpdcHost != null) {
//                 for (IpPrefix ipPrefix : vpdcHost.ipList) {

//                     /* IP INT Service to VPDC  Host Prefix*/
//                     if (ipPrefix.isIp6()) {
//                         if(keepExternalVlan) {
//                         selector = DefaultTrafficSelector.builder()
//                                 //.matchInPort(outputPort)
//                                 .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                                 .matchVlanId(vlan)
//                                 .matchEthType((short) 0x86dd)
//                                 .matchIPv6Dst(ipPrefix);
//                         }
//                         else{
//                         selector = DefaultTrafficSelector.builder()
//                                 //.matchInPort(outputPort)
//                                 .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                                 .matchVlanId(VlanId.vlanId((short) defaultVlan))
//                                 .matchEthType((short) 0x86dd)
//                                 .matchIPv6Dst(ipPrefix);
//                         }
//                     } else {
//                         if(keepExternalVlan) {
//                         selector = DefaultTrafficSelector.builder()
//                                 //.matchInPort(outputPort)
//                                 .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                                 .matchVlanId(vlan)
//                                 .matchEthType((short) 0x86dd)
//                                 .matchIPDst(ipPrefix);
//                                 }
//                                 else{
//                                 selector = DefaultTrafficSelector.builder()
//                                 //.matchInPort(outputPort)
//                                 .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                                 .matchVlanId(VlanId.vlanId((short) defaultVlan))
//                                 .matchEthType((short) 0x86dd)
//                                 .matchIPDst(ipPrefix);
//                                 }
//                         }

//                     flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
//                 }
//             }



//             /*  EXT Service to  VPDC by looking Vpdc Ip*/

// //            selector = DefaultTrafficSelector.builder()
// //                    .matchInPort(outputPort)
// //                    .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                    .matchVlanId(vlan)
// //                    .matchEthType((short)0x86dd)
// //                    .matchIPv6Dst(ipSrc);
// //
// //            treatment = DefaultTrafficTreatment.builder()
// //                    .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                    .setEthDst(dstMac)
// //                    .popVlan()
// //                    .setOutput(inputPort);
// //
// //            flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         }
//         else if (typeOfPath == PathType.SERVICE_INT_SERVICE_EXT) {
//             log.debug("[lastleaf] SERVICE INT to SERVICE EXT ");

//             /*  IP Service int to Service ext (including NDP)*/

//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     //.matchInPort(inputPort)
//                     //.matchEthSrc(srcMac)
//                     .matchEthDst(dstMac)
//                     //.matchEthType((short)0x86dd)
//                     //.matchIPv6Dst(ipDst)
//                     .matchVlanId(vlan);

//             TrafficTreatment.Builder treatment;
//             if(keepExternalVlan) {
//                 treatment = DefaultTrafficTreatment.builder()
//                         .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                         .setVlanId(vlan)
//                         .setOutput(outputPort);
//             }
//             else{
//                 treatment = DefaultTrafficTreatment.builder()
//                         .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                         .setVlanId(VlanId.vlanId((short) defaultVlan))
//                         .setOutput(outputPort);
//             }
//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));



//             /* IP service ext to service int (including NDP) */

//             if(keepExternalVlan) {
//                 if (ipDst.isIp6()){
//                 selector = DefaultTrafficSelector.builder()
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                         .matchEthType((short)0x86dd)
//                         .matchIPv6Dst(ipSrc)
//                         .matchVlanId(vlan);
//                 } else {
//                 selector = DefaultTrafficSelector.builder()
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                         .matchEthType((short)0x0800)
//                         .matchIPDst(ipSrc)
//                         .matchVlanId(vlan);
//                 }

//             } else{
//                 if (ipDst.isIp6()){
//                 selector = DefaultTrafficSelector.builder()
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                         .matchEthType((short)0x86dd)
//                         .matchIPv6Dst(ipSrc)
//                         .matchVlanId(VlanId.vlanId((short) defaultVlan));
//                 } else {
//                 selector = DefaultTrafficSelector.builder()
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                         .matchEthType((short)0x0800)
//                         .matchIPDst(ipSrc)
//                         .matchVlanId(VlanId.vlanId((short) defaultVlan));
//                 }
//             }

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthDst(hostSrcMac)
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));

// //            if (keepExternalVlan) {
// //                treatment = DefaultTrafficTreatment.builder()
// //                        .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                        .setEthDst(dstMac)
// //                        .setOutput(outputPort);
// //
// //                flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
// //            } else {
// //                treatment = DefaultTrafficTreatment.builder()
// //                        .setVlanId(VlanId.vlanId((short) defaultVlan))
// //                        .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                        .setEthDst(dstMac)
// //                        .setOutput(outputPort);
// //
// //                flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));
// //            }

//             /* IP service ext to service int (including NDP)*/
// //            if (keepExternalVlan) {
// //                if (ipDst.isIp6()) {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(vlan)
// //                            .matchEthType((short) 0x86dd)
// //                            .matchIPv6Dst(ipSrc);
// //                } else {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(vlan)
// //                            .matchEthType((short) 0x0800)
// //                            .matchIPv6Dst(ipSrc);
// //                }
// //
// //                treatment = DefaultTrafficTreatment.builder()
// //                        .setEthDst(dstMac)
// //                        .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                        .setOutput(inputPort);
// //
// //                flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
// //            } else {
// //                if (ipDst.isIp6()) {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(VlanId.vlanId((short) defaultVlan))
// //                            .matchEthType((short) 0x86dd)
// //                            .matchIPv6Dst(ipSrc);
// //                } else {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(VlanId.vlanId((short) defaultVlan))
// //                            .matchEthType((short) 0x0800)
// //                            .matchIPv6Dst(ipSrc);
// //                }
// //
// //                treatment = DefaultTrafficTreatment.builder()
// //                        .setEthDst(dstMac)
// //                        .setVlanId(vlan)
// //                        .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                        .setOutput(inputPort);
// //
// //                flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));
// //            }
// //
// //            if (clientPrefix != null) {
// //                /* IP service ext to service int using the Client IP prefix (including NDP)*/
// //                if (keepExternalVlan) {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(vlan)
// //                            .matchEthType((short)0x86dd)
// //                            .matchIPv6Dst(clientPrefix);
// //
// //                    treatment = DefaultTrafficTreatment.builder()
// //                            .setEthDst(dstMac)
// //                            .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                            .setOutput(inputPort);
// //
// //                    flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
// //                } else {
// //                    selector = DefaultTrafficSelector.builder()
// //                            .matchInPort(outputPort)
// //                            .matchVlanId(VlanId.vlanId((short) defaultVlan))
// //                            .matchEthType((short)0x86dd)
// //                            .matchIPv6Dst(clientPrefix);
// //
// //                    treatment = DefaultTrafficTreatment.builder()
// //                            .setEthDst(dstMac)
// //                            .setVlanId(vlan)
// //                            .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
// //                            .setOutput(inputPort);
// //
// //                    flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));
// //                }
// //            }

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath == PathType.SERVICE_INT_SERVICE_INT) {
//             log.debug("[lastleaf] SERVICE INT to SERVICE INT ");
//             /*	VMNBX to Service */
//             TrafficSelector.Builder selector;

//             if (ipDst.isIp6()) {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(inputPort)
//                         //.matchEthSrc(srcMac)
//                         .matchEthDst(hostDstMac)
//                         .matchEthType((short)0x86dd)
//                         .matchIPv6Dst(ipDst)
//                         .matchVlanId(vlan);
//             } else {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(inputPort)
//                         //.matchEthSrc(srcMac)
//                         .matchEthDst(hostDstMac)
//                         .matchEthType((short)0x0800)
//                         .matchIPDst(ipDst)
//                         .matchVlanId(vlan);
//             }

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthDst(dstMac)
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*	Service to VMNBX  */
//             selector = DefaultTrafficSelector.builder()
//                     .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipSrc);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(hostSrcMac)
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath == PathType.SERVICE_INT_ROUTER) {
//             log.debug("[lastleaf] SERVICE INT to ROUTER ");

//             /* Router to BGP Speaker */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     //.matchVlanId(VlanId.NONE)
//                     .matchVlanId(VlanId.vlanId((short) defaultVlan))
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setEthDst(srcMac)
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));

//             /*  BGP Speaker to Router */
//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(VlanId.vlanId((short) defaultVlan))
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, internetServicesFlowPriority, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath == PathType.SERVICE_INT_ROUTER_VLAN) {
//             log.debug("[lastleaf] SERVICE INT to ROUTER VLAN ");

//             /* Router to BGP Speaker */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setEthDst(srcMac)
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*  BGP Speaker to Router */
//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Src(ipDst);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeExternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);


//         } else if (typeOfPath == PathType.CLIENT_SERVICE_INT) {
//             log.debug("[lastleaf] CLIENT to SERVICE_INT ");
//             /*  IP Client to INT Service */
//             TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
//                     .matchInPort(inputPort)
//                     .matchVlanId(vlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipDst)
//                     .matchIPv6Src(ipSrc);

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(dstMac)
//                     .setVlanId(extraVlan)
//                     .setOutput(outputPort);


//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP INT Service to Client */
//             selector = DefaultTrafficSelector.builder()
//                     .matchInPort(outputPort)
//                     // .matchVlanId(vlanIdMac.vlan)
//                     .matchVlanId(extraVlan)
//                     .matchEthType((short)0x86dd)
//                     .matchIPv6Dst(ipSrc)
//                     .matchIPv6Src(ipDst);

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(srcMac)
//                     .setVlanId(innerVlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else {
//             log.warn("No use case match in installLeafRules method in Last");

//         }

//         return returnedError;
//     }

//     @Override
//     public ObjectiveError installMonoLeafRules(DeviceId deviceId, PortNumber inputPort, PortNumber outputPort, VlanId innerVlan,  VlanId vlan, IpPrefix ipSrc, IpPrefix ipDst, IpPrefix clientPrefix, int typeOfPath, MacAddress srcMac, MacAddress dstMac, VlanId extraVlan, boolean keepExternalVlan, boolean create, String pathId, UUID hostId, MacAddress hostSrcMac,MacAddress hostDstMac){

//         ClosDeviceService service = get(ClosDeviceService.class);
//         FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();

//         //ObjectiveError error = null;
//         ObjectiveError returnedError = null;
//         //int nextId;
//         VlanId emptyVlanId = VlanId.vlanId((short)service.getEmptyVlanIdP());
//         //List<Integer> nextIds = new ArrayList<Integer>();

//         /*Pedro*/
//         String ctpdFakeInternalMacAddress = service.getCtpdFakeInternalMacAddress();
//         String ctpdFakeExternalMacAddress = service.getCtpdFakeExternalMacAddress();
//         int internetServicesFlowPriority = service.getInternetServicesFlowPriority();
//         int defaultVlan = service.getDefaultVlan();
//         int vpdcClientPrefixlength = service.getVpdcClientPrefixlength();


//         if(typeOfPath==PathType.VPDC_SERVICE_INT) {
//             log.debug("[monoleaf] VPDC to SERVICE INT ");
//             /*  IP VPDC to INT Service */
//             TrafficSelector.Builder selector;
//             if (ipDst.isIp6()) {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(inputPort)
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                         .matchEthSrc(srcMac)
//                         .matchVlanId(VlanId.NONE)
//                         .matchEthType((short)0x86dd)
//                         .matchIPv6Dst(ipDst);
//             } else {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(inputPort)
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                         .matchEthSrc(srcMac)
//                         .matchVlanId(VlanId.NONE)
//                         .matchEthType((short)0x0800)
//                         .matchIPDst(ipDst);
//             }

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(dstMac)
//                     .pushVlan()
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);


//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP INT Service to VPDC */
//         //     selector = DefaultTrafficSelector.builder()
//         //             //.matchInPort(outputPort)
//         //             .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//         //             .matchVlanId(vlan)
//         //             .matchEthType((short)0x86dd)
//         //             .matchIPv6Dst(ipSrc);

//         //     treatment = DefaultTrafficTreatment.builder()
//         //             .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//         //             .setEthDst(dstMac)
//         //             .popVlan()
//         //             .setOutput(inputPort);

//         //     flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /* IP INT Service to VPDC */
//             ConsistentMap<UUID, Endpoint> registry = service.getRegistry();

//             VpdcHost vpdcHost = (VpdcHost) registry.get(hostId).value();

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(srcMac)
//                     .popVlan()
//                     .setOutput(inputPort);

//         //     if(vpdcHost != null) {
//         //         for (IpPrefix ipPrefix : vpdcHost.ipList) {


//         //                 /* IP INT Service to VPDC  Host Prefix*/
//         //             if (ipPrefix.isIp6()) {
//         //                 selector = DefaultTrafficSelector.builder()
//         //                         //.matchInPort(outputPort)
//         //                         .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//         //                         .matchVlanId(vlan)
//         //                         .matchEthType((short) 0x86dd)
//         //                         .matchIPv6Dst(ipPrefix);
//         //             } else {
//         //                 selector = DefaultTrafficSelector.builder()
//         //                         //.matchInPort(outputPort)
//         //                         .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//         //                         .matchVlanId(vlan)
//         //                         .matchEthType((short) 0x86dd)
//         //                         .matchIPDst(ipPrefix);
//         //             }

//         //             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
//         //         }
//         //     }

//         Endpoint vpdcClientSide = service.getVpdcClientSideFromVPDCServiceSide(srcMac);
//         clientPrefix = IpPrefix.valueOf(vpdcClientSide.getIpAddress(), vpdcClientPrefixlength);

//         /* IP INT Service to VPDC  Using client prefix */
//         selector = DefaultTrafficSelector.builder()
//                 //.matchInPort(outputPort)
//                 .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                 .matchVlanId(vlan)
//                 .matchEthType((short) 0x86dd)
//                 .matchIPv6Dst(clientPrefix);

//         flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//         /* IP INT Service to VPDC Using vpdc service side prefix */
//         selector = DefaultTrafficSelector.builder()
//                 .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                 .matchVlanId(vlan)
//                 .matchEthType((short)0x86dd)
//                 .matchIPv6Dst(ipSrc);

//         flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));



//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else if(typeOfPath == PathType.SERVICE_INT_SERVICE_INT) {
//             log.debug("[monoleaf] SERVICE INT to SERVICE INT ");
//             /*	VMNBX to Service */
//             TrafficSelector.Builder selector;

//             if (ipDst.isIp6()) {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(inputPort)
//                         .matchEthSrc(srcMac)
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                         .matchEthType((short) 0x86dd)
//                         .matchIPv6Dst(ipDst)
//                         .matchVlanId(vlan);
//             } else {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(inputPort)
//                         .matchEthSrc(srcMac)
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                         .matchEthType((short) 0x0800)
//                         .matchIPDst(ipDst)
//                         .matchVlanId(vlan);
//             }

//             TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(dstMac)
//                     //.pushVlan()
//                     .setVlanId(vlan)
//                     .setOutput(outputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));

//             /*	Service to VMNBX  */
//             if (ipSrc.isIp6()) {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(outputPort)
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                         .matchVlanId(vlan)
//                         .matchEthType((short) 0x86dd)
//                         .matchIPv6Dst(ipSrc);
//             } else {
//                 selector = DefaultTrafficSelector.builder()
//                         //.matchInPort(outputPort)
//                         .matchEthDst(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                         .matchVlanId(vlan)
//                         .matchEthType((short)0x0800)
//                         .matchIPDst(ipSrc);
//             }

//             treatment = DefaultTrafficTreatment.builder()
//                     .setEthSrc(MacAddress.valueOf(ctpdFakeInternalMacAddress))
//                     .setEthDst(srcMac)
//                     .setVlanId(vlan)
//                     .setOutput(inputPort);

//             flowObjectiveService.forward(deviceId, buildForwardingObjective(selector, treatment, create));
//             String installed = String.format(
//                     "[installLeafRules] Installed rules in device %s- match:SRC%s, action:output %s + match:DST%s, action:output %s", deviceId,
//                     srcMac, outputPort, dstMac, inputPort);

//             log.debug(installed);

//         } else {
//             log.warn("No use case match in installMonoLeafRules method");

//         }

//         return returnedError;
//     }


//     public static final byte LINK_LOCAL_0 = (byte) 0xfe;
//     public static final byte LINK_LOCAL_1 = (byte) 0x80;

//     public static byte[] getLinkLocalAddress(byte[] macAddress) {
//         return new byte[] {
//                 LINK_LOCAL_0,
//                 LINK_LOCAL_1,
//                 0, 0, 0, 0, 0, 0,
//                 (byte) (macAddress[0] ^ (1 << 1)),
//                 macAddress[1],
//                 macAddress[2],
//                 (byte) 0xff,
//                 (byte) 0xfe,
//                 macAddress[3],
//                 macAddress[4],
//                 macAddress[5],
//         };
//     }

//     @Override
//     public ObjectiveError installVlanNoneFilter(DeviceId deviceId) {
//         // To Do ...
//         return null;
//     }
}
