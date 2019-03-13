package org.ctpd.closfwd;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.ctpd.closfwd.Endpoint;
import org.ctpd.closfwd.ClosDeviceService;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Set;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class FlowFactory {

    private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");

    private static ServiceDirectory services = new DefaultServiceDirectory();

    public static <T> T get(Class<T> serviceClass) {
        return services.get(serviceClass);
    }

    public FlowFactory(){}


    public abstract ObjectiveError installL1Rules(DeviceId deviceId, PortNumber inputPort, PortNumber outputPort, VlanId innerVlan,  VlanId vlan, IpPrefix ipSrc, IpPrefix ipDst, IpPrefix clientPrefix, int typeOfEndpoint, MacAddress srcMac, MacAddress dstMac, VlanId extraVlan, boolean keepExternalVlan, boolean create, UUID hostid, MacAddress hostMacAddress);

    public abstract ObjectiveError installL2L3Rules(DeviceId deviceId, PortNumber inputPort, PortNumber outputPort, VlanId innerVlan,  VlanId vlan, IpPrefix ipSrc, IpPrefix ipDst, IpPrefix clientPrefix, int typeOfEndpoint, MacAddress srcMac, MacAddress dstMac, VlanId extraVlan, boolean keepExternalVlan, boolean create, UUID hostid, MacAddress hostMacAddress);

    public abstract ObjectiveError installL4Rules(DeviceId deviceId, PortNumber inputPort, PortNumber outputPort, VlanId innerVlan,  VlanId vlan, IpPrefix ipSrc, IpPrefix ipDst, IpPrefix clientPrefix, int typeOfEndpoint, MacAddress srcMac, MacAddress dstMac, VlanId extraVlan, boolean keepExternalVlan, boolean create, UUID hostid, MacAddress hostMacAddress);

    public abstract ObjectiveError installSpineRules(DeviceId deviceId, PortNumber inputPort, PortNumber outputPort, VlanId innerVlan,  VlanId vlan, IpPrefix ipSrc, IpPrefix ipDst, IpPrefix clientPrefix, int typeOfEndpoint, MacAddress srcMac, MacAddress dstMac, VlanId extraVlan, boolean keepExternalVlan, boolean create, UUID hostid, MacAddress hostMacAddress);

    // public abstract ObjectiveError installVlanNoneFilter(DeviceId deviceId);



//     public ForwardingObjective buildForwardingObjective(TrafficSelector.Builder selector, TrafficTreatment.Builder treatment, boolean create) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();
//         int flowPriority = service.getFlowPriority();

//         ForwardingObjective forwardingObjective;
//         if(create)
//             forwardingObjective = DefaultForwardingObjective.builder()
//                     .withSelector(selector.build())
//                     .withTreatment(treatment.build())
//                     .withPriority(flowPriority)
//                     .withFlag(ForwardingObjective.Flag.VERSATILE)
//                     .fromApp(appId)
//                     .makePermanent()
//                     .add();
//         else
//             forwardingObjective = DefaultForwardingObjective.builder()
//                     .withSelector(selector.build())
//                     .withTreatment(treatment.build())
//                     .withPriority(flowPriority)
//                     .withFlag(ForwardingObjective.Flag.VERSATILE)
//                     .fromApp(appId)
//                     .remove();

//         return forwardingObjective;
//     }
//     /*  Override arp flow installed by openflow app */
//     public ForwardingObjective buildForwardingObjective(TrafficSelector.Builder selector, TrafficTreatment.Builder treatment, int priority, boolean create) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();

//         ForwardingObjective forwardingObjective;
//         if(create)
//             forwardingObjective = DefaultForwardingObjective.builder()
//                     .withSelector(selector.build())
//                     .withTreatment(treatment.build())
//                     .withPriority(priority)
//                     .withFlag(ForwardingObjective.Flag.VERSATILE)
//                     .fromApp(appId)
//                     .makePermanent()
//                     .add();
//         else
//             forwardingObjective = DefaultForwardingObjective.builder()
//                     .withSelector(selector.build())
//                     .withTreatment(treatment.build())
//                     .withPriority(priority)
//                     .withFlag(ForwardingObjective.Flag.VERSATILE)
//                     .fromApp(appId)
//                     .remove();
//         return forwardingObjective;
//     }


// //    private ForwardingObjective makeTemporalForwardingObjective(TrafficSelector.Builder selector, TrafficTreatment.Builder treatment, int priority, boolean create) {
// //        ForwardingObjective forwardingObjective;
// //        if(create)
// //            forwardingObjective = DefaultForwardingObjective.builder()
// //                    .withSelector(selector.build())
// //                    .withTreatment(treatment.build())
// //                    .withPriority(priority)
// //                    .withFlag(ForwardingObjective.Flag.VERSATILE)
// //                    .fromApp(appId)
// //                    .makeTemporary(100)// Conectividad de 1-2 min
// //                    .add();
// //        else
// //            forwardingObjective = DefaultForwardingObjective.builder()
// //                    .withSelector(selector.build())
// //                    .withTreatment(treatment.build())
// //                    .withPriority(priority)
// //                    .withFlag(ForwardingObjective.Flag.VERSATILE)
// //                    .fromApp(appId)
// //                    .remove();
// //
// //        return forwardingObjective;
// //    }


//     public ForwardingObjective makeSpecificForwardingObjective(TrafficSelector.Builder selector, TrafficTreatment.Builder treatment, int priority, boolean create) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();

//         ForwardingObjective forwardingObjective;
//         if(create)
//             forwardingObjective = DefaultForwardingObjective.builder()
//                     .withSelector(selector.build())
//                     .withTreatment(treatment.build())
//                     .withPriority(priority)
//                     .withFlag(ForwardingObjective.Flag.SPECIFIC)
//                     .fromApp(appId)
//                     .makePermanent()
//                     .add();
//         else
//             forwardingObjective = DefaultForwardingObjective.builder()
//                     .withSelector(selector.build())
//                     .withTreatment(treatment.build())
//                     .withPriority(priority)
//                     .withFlag(ForwardingObjective.Flag.SPECIFIC)
//                     .fromApp(appId)
//                     .remove();

//         return forwardingObjective;
//     }
//     /*	Ofdpa Objective Methods*/

//     public FilteringObjective.Builder makeFilteringObjective(TrafficSelector selector) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();

//         FilteringObjective.Builder filtObj = DefaultFilteringObjective.builder()
//                 .withKey(selector.getCriterion(Criterion.Type.IN_PORT))
//                 .addCondition(selector.getCriterion(Criterion.Type.VLAN_VID))
//                 .makePermanent()
//                 .fromApp(appId)
//                 .permit();
//         return filtObj;
//     }

//     public FilteringObjective.Builder makeFilteringObjective(TrafficSelector selector, TrafficTreatment meta) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();

//         FilteringObjective.Builder filtObj = DefaultFilteringObjective.builder()
//                 .withKey(selector.getCriterion(Criterion.Type.IN_PORT))
//                 .addCondition(selector.getCriterion(Criterion.Type.VLAN_VID))
//                 .withMeta(meta)
//                 .makePermanent()
//                 .fromApp(appId)
//                 .permit();
//         return filtObj;
//     }

//     public NextObjective.Builder makeNextObjective(TrafficTreatment treatment, int nextId, TrafficSelector meta, int priority) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();

//         NextObjective.Builder nxtObj = DefaultNextObjective.builder()
//                 .withMeta(meta)
//                 .addTreatment(treatment)
//                 .withId(nextId)
//                 .withType(NextObjective.Type.SIMPLE)
//                 .withPriority(priority)
//                 .fromApp(appId)
//                 .makePermanent();
//         return nxtObj;
//     }


//     public NextObjective.Builder makeNextObjective(TrafficTreatment treatment, int nextId, TrafficSelector meta) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();
//         int flowPriority = service.getFlowPriority();

//         NextObjective.Builder nxtObj = DefaultNextObjective.builder()
//                 .withMeta(meta)
//                 .addTreatment(treatment)
//                 .withId(nextId)
//                 .withType(NextObjective.Type.SIMPLE)
//                 .withPriority(flowPriority)
//                 .fromApp(appId)
//                 .makePermanent();
//         return nxtObj;
//     }

//     public ForwardingObjective.Builder makeForwardingObjective(TrafficSelector selector, int nextId, int priority) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();

//         ForwardingObjective.Builder fwdObj = DefaultForwardingObjective.builder()
//                 .withSelector(selector)
//                 .nextStep(nextId)
//                 .withPriority(priority)
//                 .withFlag(ForwardingObjective.Flag.VERSATILE)
//                 .fromApp(appId)
//                 .makePermanent();
//         return fwdObj;
//     }

//     public ForwardingObjective.Builder makeForwardingObjective(TrafficSelector selector, int nextId) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();
//         int flowPriority = service.getFlowPriority();

//         ForwardingObjective.Builder fwdObj = DefaultForwardingObjective.builder()
//                 .withSelector(selector)
//                 .nextStep(nextId)
//                 .withPriority(flowPriority)
//                 .withFlag(ForwardingObjective.Flag.VERSATILE)
//                 .fromApp(appId)
//                 .makePermanent();
//         return fwdObj;
//     }

// //    private ForwardingObjective.Builder makeForwardingObjectiveSpecific(TrafficSelector selector, int nextId, int priority) {
// //        ForwardingObjective.Builder fwdObj = DefaultForwardingObjective.builder()
// //                .withSelector(selector)
// //                .nextStep(nextId)
// //                .withPriority(priority)
// //                .withFlag(ForwardingObjective.Flag.SPECIFIC)
// //                .fromApp(appId)
// //                .makePermanent();
// //        return fwdObj;
// //    }
// //
// //    private ForwardingObjective.Builder makeForwardingObjectiveSpecific(TrafficSelector selector, int nextId) {
// //        ForwardingObjective.Builder fwdObj = DefaultForwardingObjective.builder()
// //                .withSelector(selector)
// //                .nextStep(nextId)
// //                .withPriority(flowPriority)
// //                .withFlag(ForwardingObjective.Flag.SPECIFIC)
// //                .fromApp(appId)
// //                .makePermanent();
// //        return fwdObj;
// //    }

//     /** Temporary **/


//     public FilteringObjective.Builder makeTmpFilteringObjective(TrafficSelector selector) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();
//         int client2ServiceIdleTimeout = service.getClient2ServiceIdleTimeout();

//         FilteringObjective.Builder filtObj = DefaultFilteringObjective.builder()
//                 .withKey(selector.getCriterion(Criterion.Type.IN_PORT))
//                 .addCondition(selector.getCriterion(Criterion.Type.VLAN_VID))
//                 .makeTemporary(client2ServiceIdleTimeout)
//                 .fromApp(appId)
//                 .permit();
//         return filtObj;
//     }

//     public FilteringObjective.Builder makeTmpFilteringObjective(TrafficSelector selector, TrafficTreatment meta) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();
//         int client2ServiceIdleTimeout = service.getClient2ServiceIdleTimeout();

//         FilteringObjective.Builder filtObj = DefaultFilteringObjective.builder()
//                 .withKey(selector.getCriterion(Criterion.Type.IN_PORT))
//                 .addCondition(selector.getCriterion(Criterion.Type.VLAN_VID))
//                 .withMeta(meta)
//                 .makeTemporary(client2ServiceIdleTimeout)
//                 .fromApp(appId)
//                 .permit();
//         return filtObj;
//     }

//     public NextObjective.Builder makeTmpNextObjective(TrafficTreatment treatment, int nextId, TrafficSelector meta, int priority) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();
//         int client2ServiceIdleTimeout = service.getClient2ServiceIdleTimeout();

//         NextObjective.Builder nxtObj = DefaultNextObjective.builder()
//                 .withMeta(meta)
//                 .addTreatment(treatment)
//                 .withId(nextId)
//                 .withType(NextObjective.Type.SIMPLE)
//                 .withPriority(priority)
//                 .fromApp(appId)
//                 .makeTemporary(client2ServiceIdleTimeout);
//         return nxtObj;
//     }

//     public NextObjective.Builder makeTmpNextObjective(TrafficTreatment treatment, int nextId, TrafficSelector meta) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();
//         int flowPriority = service.getFlowPriority();
//         int client2ServiceIdleTimeout = service.getClient2ServiceIdleTimeout();

//         NextObjective.Builder nxtObj = DefaultNextObjective.builder()
//                 .withMeta(meta)
//                 .addTreatment(treatment)
//                 .withId(nextId)
//                 .withType(NextObjective.Type.SIMPLE)
//                 .withPriority(flowPriority)
//                 .fromApp(appId)
//                 .makeTemporary(client2ServiceIdleTimeout);
//         return nxtObj;
//     }

//     public ForwardingObjective.Builder makeTmpForwardingObjective(TrafficSelector selector, int nextId, int priority) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();
//         int client2ServiceIdleTimeout = service.getClient2ServiceIdleTimeout();

//         ForwardingObjective.Builder fwdObj = DefaultForwardingObjective.builder()
//                 .withSelector(selector)
//                 .nextStep(nextId)
//                 .withPriority(priority)
//                 .withFlag(ForwardingObjective.Flag.VERSATILE)
//                 .fromApp(appId)
//                 .makeTemporary(client2ServiceIdleTimeout);
//         return fwdObj;
//     }

//     public ForwardingObjective.Builder makeTmpForwardingObjective(TrafficSelector selector, TrafficTreatment treatment, int nextId, int priority) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();
//         int client2ServiceIdleTimeout = service.getClient2ServiceIdleTimeout();

//         ForwardingObjective.Builder fwdObj = DefaultForwardingObjective.builder()
//                 .withSelector(selector)
//                 .nextStep(nextId)
//                 .withPriority(priority)
//                 .withFlag(ForwardingObjective.Flag.VERSATILE)
//                 .withTreatment(treatment)
//                 .fromApp(appId)
//                 .makeTemporary(client2ServiceIdleTimeout);
//         return fwdObj;
//     }

//     public ForwardingObjective.Builder makeTmpForwardingObjective(TrafficSelector selector, int nextId) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         ApplicationId appId = service.getAppId();
//         int flowPriority = service.getFlowPriority();
//         int client2ServiceIdleTimeout = service.getClient2ServiceIdleTimeout();

//         ForwardingObjective.Builder fwdObj = DefaultForwardingObjective.builder()
//                 .withSelector(selector)
//                 .nextStep(nextId)
//                 .withPriority(flowPriority)
//                 .withFlag(ForwardingObjective.Flag.VERSATILE)
//                 .fromApp(appId)
//                 .makeTemporary(client2ServiceIdleTimeout);
//         return fwdObj;
//     }


//     /*Pedro*/

//     public ObjectiveError filter(DeviceId deviceId, FilteringObjective.Builder filtObj, boolean create) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();
//         long flowInstallationTimeout = service.getFlowInstallationTimeout();

//         CompletableFuture<ObjectiveError> filtObjFuture = new CompletableFuture();
//         if (create)
//             flowObjectiveService.filter(deviceId, filtObj.add(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Filter add successfully in Device ID: {}", deviceId);
//                     filtObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     log.error("Error in CompletableFuture create");
//                     filtObjFuture.complete(error);
//                 }
//             }));
//         else
//             flowObjectiveService.filter(deviceId, filtObj.remove(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Filter remove successfully in Device ID: {}", deviceId);
//                     filtObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     log.error("Error in CompletableFuture delete");
//                     filtObjFuture.complete(error);
//                 }
//             }));
//         return null;

// 		/*	Wait for Response from Switch */
//         // try{
//         //     return filtObjFuture.get(flowInstallationTimeout, TimeUnit.SECONDS);
//         // } catch (Exception e) {
//         //     log.warn("Timeout Exception verifying result from FilteringObjective:{}", deviceId);
//         // }
//         // return ObjectiveError.UNKNOWN;
//     }

//     public ObjectiveError filter(DeviceId deviceId, FilteringObjective.Builder filtObj, boolean create, String pathId) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();
//         long flowInstallationTimeout = service.getFlowInstallationTimeout();
//         String pathid = new String (Base64.getUrlDecoder().decode(pathId));

//         CompletableFuture<ObjectiveError> filtObjFuture = new CompletableFuture();
//         if (create)
//             flowObjectiveService.filter(deviceId, filtObj.add(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
                    // log.debug("Filter to Path: {} add successfully in Device ID: {}", pathid , deviceId);
//                     filtObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     // log.info("Error in CompletableFuture create");
//                     log.error("Error adding Filter to Path: {} in Device ID: {}", pathid , deviceId);
//                     filtObjFuture.complete(error);
//                 }
//             }));
//         else
//             flowObjectiveService.filter(deviceId, filtObj.remove(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Filter from Path: {} remove successfully in Device ID: {}", pathid , deviceId);
//                     filtObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     // log.info("Error in CompletableFuture delete");
//                     log.error("Error removing Filter from Path: {} in Device ID: {}", pathid , deviceId);
//                     filtObjFuture.complete(error);
//                 }
//             }));
//         return null;

// 		/*	Wait for Response from Switch */
//         // try{
//         //     return filtObjFuture.get(flowInstallationTimeout, TimeUnit.SECONDS);
//         // } catch (Exception e) {
//         //     log.warn("Timeout Exception verifying result from FilteringObjective:{}", deviceId);
//         // }
//         // return ObjectiveError.UNKNOWN;
//     }


//     public ObjectiveError next(DeviceId deviceId, NextObjective.Builder nxtObj, boolean create) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();

//         long flowInstallationTimeout = service.getFlowInstallationTimeout();

//         CompletableFuture<ObjectiveError> nextObjFuture = new CompletableFuture();
//         if (create)
//             flowObjectiveService.next(deviceId, nxtObj.add(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Next objective add successfully in Device ID: {}", deviceId);
//                     nextObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     log.error("Error adding next objective in Device ID: {}", deviceId);
//                     nextObjFuture.complete(error);
//                 }
//             }));
//         else
//             flowObjectiveService.next(deviceId, nxtObj.remove(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Next objective remove successfully in Device ID: {}", deviceId);
//                     nextObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     log.error("Error removing next objective in Device ID: {}", deviceId);
//                     nextObjFuture.complete(error);
//                 }
//             }));
//         return null;
// 		/*	Wait for Response from Switch */
//         // try{
//         //     return nextObjFuture.get(flowInstallationTimeout, TimeUnit.SECONDS);
//         // } catch (Exception e) {
//         //     log.warn("Timeout Exception verifying result from NextObjective:{}", deviceId);
//         // }
//         // return ObjectiveError.UNKNOWN;
//     }

//     public ObjectiveError next(DeviceId deviceId, NextObjective.Builder nxtObj, boolean create, String pathId) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();

//         long flowInstallationTimeout = service.getFlowInstallationTimeout();
//         String pathid = new String (Base64.getUrlDecoder().decode(pathId));

//         CompletableFuture<ObjectiveError> nextObjFuture = new CompletableFuture();
//         if (create)
//             flowObjectiveService.next(deviceId, nxtObj.add(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Next objective to Path: {} add successfully in Device ID: {}", pathid , deviceId);
//                     nextObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     log.error("Error adding to Path: {} next objective in Device ID: {}", pathid, deviceId);
//                     nextObjFuture.complete(error);
//                 }
//             }));
//         else
//             flowObjectiveService.next(deviceId, nxtObj.remove(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Next objective to Path: {} remove successfully in Device ID: {}", pathid , deviceId);
//                     nextObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     log.error("Error removing to Path: {} next objective in Device ID: {}", pathid , deviceId);
//                     nextObjFuture.complete(error);
//                 }
//             }));
//         return null;
// 		/*	Wait for Response from Switch */
//         // try{
//         //     return nextObjFuture.get(flowInstallationTimeout, TimeUnit.SECONDS);
//         // } catch (Exception e) {
//         //     log.warn("Timeout Exception verifying result from NextObjective:{}", deviceId);
//         // }
//         // return ObjectiveError.UNKNOWN;
//     }



//     public ObjectiveError forward(DeviceId deviceId, ForwardingObjective.Builder fwdObj, boolean create) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();
//         long flowInstallationTimeout = service.getFlowInstallationTimeout();

//         CompletableFuture<ObjectiveError> fwdObjFuture = new CompletableFuture();
//         if (create)
//             flowObjectiveService.forward(deviceId, fwdObj.add(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Forward objective add successfully in Device ID: {}", deviceId);
//                     fwdObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     log.error("Error adding forward objective in Device ID: {}", deviceId);
//                     fwdObjFuture.complete(error);
//                 }
//             }));
//         else
//             flowObjectiveService.forward(deviceId, fwdObj.remove(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Forward objective remove successfully in Device ID: {}", deviceId);
//                     fwdObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     log.error("Error removing Forward objective in Device ID: {}", deviceId);
//                     fwdObjFuture.complete(error);
//                 }
//             }));
//         return null;
// 		/*	Wait for Response from Switch */
//         // try{
//         //     return fwdObjFuture.get(flowInstallationTimeout, TimeUnit.SECONDS);
//         // } catch (Exception e) {
//         //     log.warn("Timeout Exception verifying result from ForwardingObjective:{}",deviceId);
//         // }
//         // return ObjectiveError.UNKNOWN;
//     }


//     public ObjectiveError forward(DeviceId deviceId, ForwardingObjective.Builder fwdObj, boolean create, String pathId) {
//         ClosDeviceService service = get(ClosDeviceService.class);
//         FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();
//         long flowInstallationTimeout = service.getFlowInstallationTimeout();
//         String pathid = new String (Base64.getUrlDecoder().decode(pathId));

//         CompletableFuture<ObjectiveError> fwdObjFuture = new CompletableFuture();
//         if (create)
//             flowObjectiveService.forward(deviceId, fwdObj.add(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Forward objective to Path: {} add successfully in Device ID: {}", pathid, deviceId);
//                     fwdObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     log.error("Error adding to Path: {} forward objective in Device ID: {}", pathid, deviceId);
//                     fwdObjFuture.complete(error);
//                 }
//             }));
//         else
//             flowObjectiveService.forward(deviceId, fwdObj.remove(new ObjectiveContext() {
//                 @Override
//                 public void onSuccess(Objective objective) {
//                     log.debug("Forward objective from Path: {} remove successfully in Device ID: {}", pathid, deviceId);
//                     fwdObjFuture.complete(null);
//                 }

//                 @Override
//                 public void onError(Objective objective, ObjectiveError error) {
//                     log.error("Error removing from Path: {} Forward objective in Device ID: {}", pathid, deviceId);
//                     fwdObjFuture.complete(error);
//                 }
//             }));
//         return null;
// 		/*	Wait for Response from Switch */
//         // try{
//         //     return fwdObjFuture.get(flowInstallationTimeout, TimeUnit.SECONDS);
//         // } catch (Exception e) {
//         //     log.warn("Timeout Exception verifying result from ForwardingObjective:{}",deviceId);
//         // }
//         // return ObjectiveError.UNKNOWN;
//     }





//     public ObjectiveError processInstalledRuleObjectiveError(DeviceId deviceId, ObjectiveError error, ObjectiveError returnedError) {
//         ClosDeviceService service = get(ClosDeviceService.class);

//         if (error != null) {
//             log.warn("[processInstalledRuleObjectiveError] error:{} on device:{}", service.processObjectiveError(error), deviceId.toString());
//             return error;
//         }

//         log.debug("[processInstalledRuleObjectiveError] success on flow for device:{}", deviceId.toString());

//         return returnedError;
//     }

}

