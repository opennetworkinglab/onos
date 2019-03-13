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
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.TwoWayP2PIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.Device;
import java.util.List;
import org.ctpd.closfwd.Driver;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.Constraint;
import java.util.ArrayList;
import com.google.common.collect.Lists;
import org.onosproject.core.ApplicationId;

import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.Device;




import java.util.Base64;
import java.util.Set;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.sql.ConnectionPoolDataSource;

public abstract class FlowDriver implements Driver{

    private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");

    private static ServiceDirectory services = new DefaultServiceDirectory();

    public static <T> T get(Class<T> serviceClass) {
        return services.get(serviceClass);
    }

    public FlowDriver(){}

//     private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");


    public abstract void installL1Flows(Endpoint endpoint, boolean create);

    public abstract void installL2L3Flows(Endpoint endpoint, DeviceId deviceId, boolean create);

    public abstract void installL4Flows(Endpoint endpoint, DeviceId deviceId, boolean create);

    public abstract void installSpineFlows(Endpoint endpoint,  DeviceId deviceId, boolean create);

    public abstract void createIntent(Endpoint ingressEndpoint, Endpoint egressEndpoint, boolean create);


    public Key createIntentFlows(Key key ,TrafficSelector selector,TrafficTreatment treatment, int priority, ConnectPoint one, ConnectPoint two, Boolean create){

        ClosDeviceService service = get(ClosDeviceService.class);
        ApplicationId appId = service.getAppId();
        IntentService intentService = service.getIntentService();
        FlowObjectiveService flowService = service.getFlowObjectiveService();

        // List<Constraint> constraint = Lists.newArrayList();

        if (create){

            FilteredConnectPoint ingressConnectPoint = new FilteredConnectPoint(one);
            FilteredConnectPoint egressConnectPoint = new FilteredConnectPoint(two);

            ConnectivityIntent intent = PointToPointIntent.builder()
                    .appId(appId)
                    .key(key)
                    .filteredIngressPoint(ingressConnectPoint)
                    .filteredEgressPoint(egressConnectPoint)
                    .selector(selector)
                    .treatment(treatment)
                    .priority(priority)
                    // .constraints(constraint)
                    .build();

            log.debug("[createIntentFlows] Installing intent: "+intent.toString());
            intentService.submit(intent);
            log.debug("[createIntentFlows] Installed intent succesfully: "+intent.toString());

            return key;

        }else{
            Intent intentToDelete = service.getIntentService().getIntent(key);
            log.debug("[createIntentFlows] Withdrawing Intent: "+intentToDelete.toString());
            service.getIntentService().withdraw(intentToDelete);
            log.debug("[createIntentFlows] Withdrawn Intent succesfully: "+intentToDelete.toString());

            // IntentState state = service.getIntentService().getIntentState(key);
            // log.info("Key Deleted "+key.toString());
            // service.getIntentService().purge(intentToDelete);
            return key;
        }
    }


    public FilteringObjective.Builder makeFilteringObjective(TrafficSelector selector, TrafficTreatment meta, ApplicationId appId) {
        return makeFilteringObjective(selector, meta, -1, appId);
    }

    public FilteringObjective.Builder makeFilteringObjective(TrafficSelector selector, TrafficTreatment meta, int priority, ApplicationId appId) {

        FilteringObjective.Builder filtObj = DefaultFilteringObjective.builder()
                .withKey(selector.getCriterion(Criterion.Type.IN_PORT))
                .addCondition(selector.getCriterion(Criterion.Type.VLAN_VID))
                .withMeta(meta)
                .makePermanent()
                .fromApp(appId);

        Criterion innerVlan = selector.getCriterion(Criterion.Type.INNER_VLAN_VID);

        if(innerVlan != null)
            filtObj.addCondition(innerVlan);

        if(priority >= 0)
            filtObj.withPriority(priority);

        filtObj.permit();

        return filtObj;
    }

		/*	Wait for Response from Switch */
        // try{
        //     return filtObjFuture.get(flowInstallationTimeout, TimeUnit.SECONDS);
        // } catch (Exception e) {
        //     log.warn("Timeout Exception verifying result from FilteringObjective:{}", deviceId);
        // }
        // return ObjectiveError.UNKNOWN;

    public void processInstalledRuleObjectiveError(DeviceId deviceId, ObjectiveError error) {
        ClosDeviceService service = get(ClosDeviceService.class);

        if (error != null) {
            log.warn("[processInstalledRuleObjectiveError] error:{} on device:{}", processObjectiveError(error), deviceId.toString());
        }

        log.debug("[processInstalledRuleObjectiveError] success on flow for device:{}", deviceId.toString());
    }

    public String processObjectiveError(ObjectiveError error) {
		switch (error) {
			case BADPARAMS:
				return "[Error] BADPARAMS: Incorrect Objective parameters passed in by the caller.";
			case DEVICEMISSING:
				return "[Error] DEVICEMISSING: The device was not available to install objectives to.";
			case FLOWINSTALLATIONFAILED:
				return "[Error] FLOWINSTALLATIONFAILED: The flow installation for this objective failed.";
			case GROUPINSTALLATIONFAILED:
				return "[Error] GROUPINSTALLATIONFAILED: The group installation for this objective failed.";
			case GROUPMISSING:
				return "[Error] GROUPMISSING: The group was reported as installed but is missing.";
			case GROUPREMOVALFAILED:
				return "[Error] GROUPREMOVALFAILED: The group removal for this objective failed.";
			case NOPIPELINER:
				return "[Error] NOPIPELINER: The device has no pipeline driver to install objectives.";
			case UNKNOWN:
				return "[Error] UNKNOWN: An unknown error occurred.";
			case UNSUPPORTED:
				return "[Error] UNSUPPORTED: The driver processing this objective does not know how to process it.";
		}
		return "[Error] LAST: UNKNOWN: An unknown error occurred.";
	}


    public NextObjective.Builder makeNextObjective(List<TrafficTreatment> treatmentList, int nextId, TrafficSelector meta, int priority, ApplicationId appId) {

        NextObjective.Builder nxtObj = DefaultNextObjective.builder()
                .withMeta(meta)
                // .addTreatment(treatment)
                .withId(nextId)
                .withType(NextObjective.Type.SIMPLE)
                .withPriority(priority)
                .fromApp(appId)
                .makePermanent();

        for(TrafficTreatment treatment  : treatmentList){
            nxtObj.addTreatment(treatment);
        }
        if(treatmentList.size()>1){
            nxtObj.withType(NextObjective.Type.HASHED);
        }
        return nxtObj;
    }

    public NextObjective.Builder makeTmpNextObjective(List<TrafficTreatment> treatmentList, int nextId, TrafficSelector meta, int priority, ApplicationId appId) {
        ClosDeviceService service = get(ClosDeviceService.class);
        int client2ServiceIdleTimeout = service.getClient2ServiceIdleTimeout();

        NextObjective.Builder nxtObj = DefaultNextObjective.builder()
                .withMeta(meta)
                // .addTreatment(treatment)
                .withId(nextId)
                .withType(NextObjective.Type.SIMPLE)
                .withPriority(priority)
                .fromApp(appId)
                .makeTemporary(client2ServiceIdleTimeout);


        for(TrafficTreatment treatment  : treatmentList){
            nxtObj.addTreatment(treatment);
        }
        if(treatmentList.size()>1){
            nxtObj.withType(NextObjective.Type.HASHED);
        }
        return nxtObj;
    }

    public ObjectiveError filter(DeviceId deviceId, FilteringObjective.Builder filtObj, boolean create) {
        ClosDeviceService service = get(ClosDeviceService.class);
        FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();
        long flowInstallationTimeout = service.getFlowInstallationTimeout();
        CompletableFuture<ObjectiveError> filtObjFuture = new CompletableFuture();
        if (create){
            log.debug("[filter] Adding filter objective", deviceId);
            flowObjectiveService.filter(deviceId, filtObj.add(new ObjectiveContext() {
                @Override
                public void onSuccess(Objective objective) {
                    log.debug("[filter] Filter add successfully in Device ID: {}", deviceId);
                    filtObjFuture.complete(null);
                }

                @Override
                public void onError(Objective objective, ObjectiveError error) {
                    log.error("[filter] Error in CompletableFuture create");
                    filtObjFuture.complete(error);
                }
            }));
        }else{
            log.debug("[filter] Deleting filter objective", deviceId);
            flowObjectiveService.filter(deviceId, filtObj.remove(new ObjectiveContext() {
                @Override
                public void onSuccess(Objective objective) {
                    log.debug("[filter] Filter remove successfully in Device ID: {}", deviceId);
                    filtObjFuture.complete(null);
                }

                @Override
                public void onError(Objective objective, ObjectiveError error) {
                    log.error("[filter] Error in CompletableFuture delete");
                    filtObjFuture.complete(error);
                }
            }));
        }
        return null;
    }

    public ObjectiveError next(DeviceId deviceId, NextObjective.Builder nxtObj, boolean create) {
        ClosDeviceService service = get(ClosDeviceService.class);
        FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();
        long flowInstallationTimeout = service.getFlowInstallationTimeout();
        String str = String.valueOf(create);
        CompletableFuture<ObjectiveError> nextObjFuture = new CompletableFuture();

        if (create){
            log.debug("[next] Adding next objective", deviceId);
            flowObjectiveService.next(deviceId, nxtObj.add(new ObjectiveContext() {
                @Override
                public void onSuccess(Objective objective) {
                    log.debug("[next] Next objective add successfully in Device ID: {}", deviceId);
                    nextObjFuture.complete(null);
                }

                @Override
                public void onError(Objective objective, ObjectiveError error) {
                    log.error("[next] Error adding next objective in Device ID: {}", deviceId);
                    nextObjFuture.complete(error);
                }
            }));
        }else{
            log.debug("[next] Deleting next objective", deviceId);
            flowObjectiveService.next(deviceId, nxtObj.remove(new ObjectiveContext() {
                @Override
                public void onSuccess(Objective objective) {
                    log.debug("[next] Next objective remove successfully in Device ID: {}", deviceId);
                    nextObjFuture.complete(null);
                }

                @Override
                public void onError(Objective objective, ObjectiveError error) {
                    log.error("[next] Error removing next objective in Device ID: {}", deviceId);
                    nextObjFuture.complete(error);
                }
            }));
        }
        return null;
		/*	Wait for Response from Switch */
        // try{
        //     return nextObjFuture.get(flowInstallationTimeout, TimeUnit.SECONDS);
        // } catch (Exception e) {
        //     log.warn("Timeout Exception verifying result from NextObjective:{}", deviceId);
        // }
        // return ObjectiveError.UNKNOWN;
    }

    public ForwardingObjective.Builder makeForwardingObjective(TrafficSelector selector, int nextId, int priority, ApplicationId appId) {

        ForwardingObjective.Builder fwdObj = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .nextStep(nextId)
                .withPriority(priority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent();
        return fwdObj;
    }

    public ForwardingObjective.Builder makeTmpForwardingObjective(TrafficSelector selector, int nextId, int priority, ApplicationId appId) {
        ClosDeviceService service = get(ClosDeviceService.class);
        int client2ServiceIdleTimeout = service.getClient2ServiceIdleTimeout();

        ForwardingObjective.Builder fwdObj = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .nextStep(nextId)
                .withPriority(priority)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(client2ServiceIdleTimeout);
        return fwdObj;
    }


    public ForwardingObjective.Builder makeEgressForwardingObjective(TrafficSelector selector, TrafficTreatment treatment, ApplicationId appId) {
        ClosDeviceService service = get(ClosDeviceService.class);
        int client2ServiceIdleTimeout = service.getClient2ServiceIdleTimeout();

        ForwardingObjective.Builder fwdObj = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .withTreatment(treatment)
                .withFlag(ForwardingObjective.Flag.EGRESS)
                .fromApp(appId)
                .makeTemporary(client2ServiceIdleTimeout);
        return fwdObj;
    }

    public ObjectiveError forward(DeviceId deviceId, ForwardingObjective.Builder fwdObj, boolean create) {
        ClosDeviceService service = get(ClosDeviceService.class);
        FlowObjectiveService flowObjectiveService = service.getFlowObjectiveService();
        long flowInstallationTimeout = service.getFlowInstallationTimeout();
        CompletableFuture<ObjectiveError> fwdObjFuture = new CompletableFuture();
        if (create){
            log.debug("[forward] Adding forward objective", deviceId);
            flowObjectiveService.forward(deviceId, fwdObj.add(new ObjectiveContext() {
                @Override
                public void onSuccess(Objective objective) {
                    log.debug("[forward] Forward objective add successfully in Device ID: {}", deviceId);
                    fwdObjFuture.complete(null);
                }

                @Override
                public void onError(Objective objective, ObjectiveError error) {
                    log.error("[forward] Error adding forward objective in Device ID: {}", deviceId);
                    fwdObjFuture.complete(error);
                }
            }));

        }else{
            log.debug("[forward] Deleting forward objective", deviceId);
            flowObjectiveService.forward(deviceId, fwdObj.remove(new ObjectiveContext() {
                @Override
                public void onSuccess(Objective objective) {
                    log.debug("[forward] Forward objective remove successfully in Device ID: {}", deviceId);
                    fwdObjFuture.complete(null);
                }

                @Override
                public void onError(Objective objective, ObjectiveError error) {
                    log.error("[forward] Error removing Forward objective in Device ID: {}", deviceId);
                    fwdObjFuture.complete(error);
                }
            }));
        }
        return null;
		/*	Wait for Response from Switch */
        // try{
        //     return fwdObjFuture.get(flowInstallationTimeout, TimeUnit.SECONDS);
        // } catch (Exception e) {
        //     log.warn("Timeout Exception verifying result from ForwardingObjective:{}",deviceId);
        // }
        // return ObjectiveError.UNKNOWN;
    }
}
