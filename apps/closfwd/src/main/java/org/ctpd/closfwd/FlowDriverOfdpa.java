package org.ctpd.closfwd;

import org.ctpd.closfwd.Endpoint;
import org.ctpd.closfwd.ClosDeviceService;
import org.ctpd.closfwd.VpdcEndpoint;
import org.ctpd.closfwd.VoltEndpoint;
import org.ctpd.closfwd.ClientServiceBypassEndpoint;
import org.ctpd.closfwd.VpdcHostEndpoint;
import org.dom4j.Branch;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.*;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.Key;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.flow.criteria.Criterion;

import org.onosproject.cli.net.IntentKeyCompleter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.store.service.ConsistentMap;
import java.util.*;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.flowobjective.*;




import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class FlowDriverOfdpa extends FlowDriver {

    private final Logger log = LoggerFactory.getLogger("log4j.logger.org.ctpdLogger");

    private static ServiceDirectory services = new DefaultServiceDirectory();

    public static <T> T get(Class<T> serviceClass) {
        return services.get(serviceClass);
    }

    TrafficSelector selector;
    TrafficTreatment treatment;
    ObjectiveError error;
    List<TrafficTreatment> treatmentList;
    List<TrafficSelector> selectorList;

    public FlowDriverOfdpa(){
    }


    @Override
    public  void createIntent(Endpoint ingressEndpoint, Endpoint egressEndpoint, boolean create){

        ClosDeviceService service = get(ClosDeviceService.class);
        Key key = null;
        long range = 0;
        Random r = null;
        long number = 0;

        log.debug("install-intents-flows-start");

        VlanId emptyVlanId = VlanId.vlanId((short)service.getEmptyVlanId());

        DeviceId ingressDeviceId = ingressEndpoint.getNode();
        DeviceId egressDeviceId = egressEndpoint.getNode();

        if(ingressEndpoint instanceof VoltEndpoint && egressEndpoint instanceof OltControlEndpoint){

            // intent creation to conect Voltha with OLT

            selector = DefaultTrafficSelector.builder()
                    .matchVlanId(ingressEndpoint.getVlan())
                    .matchEthDst(egressEndpoint.getMac())
                    .matchInPort(ingressEndpoint.getPort())
                    .build();

            if(((OltControlEndpoint)egressEndpoint).getExplicitVlan()) {
                treatment = DefaultTrafficTreatment.builder()
                    .build();
            } else {
                treatment = DefaultTrafficTreatment.builder()
                    .popVlan()
                    .build();
            }

            ConnectPoint one = new ConnectPoint(ingressDeviceId, ingressEndpoint.getPort());
            ConnectPoint two = new ConnectPoint(egressDeviceId, egressEndpoint.getPort());

            if(create){
                range = 1234567L;
                r = new Random();
                number = (long)(r.nextDouble()*range);
                key = Key.of(number, service.getAppId());
                log.debug("Creating intent to join {} -> {} with key "+key.toString(),one.toString(),two.toString());
                service.getRegisterIntents().put(egressEndpoint.getUUID()+"/"+key.toString(), key);
                // log.debug("EndPointUUIDEgress "+egressEndpoint.getUUID());
                // log.debug("KeyGenerated "+key.toString());
                // log.debug("keySetIntents "+service.getRegisterIntents().keySet());
            }
            else{
                Set<String> keySetIntents = service.getRegisterIntents().keySet();
                // log.debug("keySetIntents "+service.getRegisterIntents().keySet());
                for(String idIter : keySetIntents){
                    // log.debug("idIter "+idIter.toString());
                    // log.debug("id "+ egressEndpoint.id.toString());
                    if(idIter.toString().split("/")[0].equals(egressEndpoint.getUUID().toString())){
                        key = service.getRegisterIntents().get(idIter).value();
                        log.debug("Deleting intent to join {} -> {} with key "+key.toString(),one.toString(),two.toString());
                        // log.debug("intentKey "+key.toString());
                        service.getRegisterIntents().remove(idIter);

                        break;
                    }
                }
            }

            createIntentFlows(key, selector, treatment, service.getFlowPriority(), one, two,create);

            // intent creation to conect OLT with Voltha

            selector = DefaultTrafficSelector.builder()
                    .matchEthDst(ingressEndpoint.getMac())
                    .matchInPort(egressEndpoint.getPort())
                    .matchVlanId(ingressEndpoint.getVlan())     // Filter rules make voltha vlan present in both directions
                    .build();

            treatment = DefaultTrafficTreatment.builder()
                    .setVlanId(ingressEndpoint.getVlan())
                    // .setOutput(PortNumber.portNumber(41))
                    // .popVlan()
                    .build();


            if(create){
                range = 1234567L;
                r = new Random();
                number = (long)(r.nextDouble()*range);
                key = Key.of(number, service.getAppId());
                log.debug("Creating intent to join {} -> {} with key "+key.toString(),two.toString(),one.toString());
                service.getRegisterIntents().put(egressEndpoint.getUUID()+"/"+key.toString(), key);
                // log.debug("EndPointUUIDEgress "+egressEndpoint.getUUID());
                // log.debug("KeyGenerated "+key.toString());
                // log.debug("keySetIntents "+service.getRegisterIntents().keySet().toString());
            }
            else{
                Set<String> keySetIntents = service.getRegisterIntents().keySet();
                // log.debug("keySetIntents "+service.getRegisterIntents().keySet());
                for(String idIter : keySetIntents){
                    // log.debug("idIter "+idIter.toString());
                    // log.debug("id "+egressEndpoint.id.toString());
                    if(idIter.toString().split("/")[0].equals(egressEndpoint.getUUID().toString())){
                        key = service.getRegisterIntents().get(idIter).value();
                        log.debug("Deleting intent to join {} -> {} with key "+key.toString(),two.toString(),one.toString());
                        // log.debug("intentKey "+key.toString());
                        service.getRegisterIntents().remove(idIter);
                        break;
                    }
                }
            }

            createIntentFlows(key, selector, treatment, service.getFlowPriority(), two, one,create);

        }


        if(ingressEndpoint instanceof OltEndpoint && egressEndpoint instanceof VpdcHostEndpoint){


            // intent creation to conect OLT tagged with vpdchost

            selector = DefaultTrafficSelector.builder()
                    .matchVlanId(egressEndpoint.getVlan())
                    .build();

            treatment = DefaultTrafficTreatment.builder()
                    .setVlanId(egressEndpoint.getVlan())
                    .build();

            ConnectPoint one = new ConnectPoint(ingressDeviceId, ingressEndpoint.getPort());
            ConnectPoint two = new ConnectPoint(egressDeviceId, egressEndpoint.getPort());

            if(create){
                range = 1234567L;
                r = new Random();
                number = (long)(r.nextDouble()*range);
                key = Key.of(number, service.getAppId());
                log.debug("Creating intent to join {} -> {} with key "+key.toString(),one.toString(),two.toString());
                service.getRegisterIntents().put(egressEndpoint.getUUID()+"/"+key.toString(), key);
                // log.debug("EndPointUUIDEgress "+egressEndpoint.getUUID());
                // log.debug("KeyGenerated "+key.toString());
                // log.debug("keySetIntents "+service.getRegisterIntents().keySet());
            }
            else{
                Set<String> keySetIntents = service.getRegisterIntents().keySet();
                // log.debug("keySetIntents "+service.getRegisterIntents().keySet());
                for(String idIter : keySetIntents){
                    // log.debug("idIter "+idIter.toString());
                    // log.debug("id "+ egressEndpoint.id.toString());
                    if(idIter.toString().split("/")[0].equals(egressEndpoint.getUUID().toString())){
                        key = service.getRegisterIntents().get(idIter).value();
                        log.debug("Deleting intent to join {} -> {} with key "+key.toString(),one.toString(),two.toString());
                        // log.debug("intentKey "+key.toString());
                        service.getRegisterIntents().remove(idIter);
                        break;
                    }
                }
            }

            createIntentFlows(key, selector, treatment, service.getFlowPriority(), one, two,create);

            // intent creation to conect vpdchost with OLT tagged

            selector = DefaultTrafficSelector.builder()
                    .matchVlanId(egressEndpoint.getVlan())
                    .build();

            treatment = DefaultTrafficTreatment.builder()
                    .setVlanId(egressEndpoint.getVlan())
                    .build();

            if(create){
                range = 1234567L;
                r = new Random();
                number = (long)(r.nextDouble()*range);
                key = Key.of(number, service.getAppId());
                log.debug("Creating intent to join {} -> {} with key "+key.toString(),two.toString(),one.toString());
                service.getRegisterIntents().put(egressEndpoint.getUUID()+"/"+key.toString(), key);
                // log.debug("EndPointUUIDEgress "+egressEndpoint.getUUID());
                // log.debug("KeyGenerated "+key.toString());
                // log.debug("keySetIntents "+service.getRegisterIntents().keySet().toString());
            }
            else{
                Set<String> keySetIntents = service.getRegisterIntents().keySet();
                // log.debug("keySetIntents "+service.getRegisterIntents().keySet());
                for(String idIter : keySetIntents){
                    // log.debug("idIter "+idIter.toString());
                    // log.debug("id "+egressEndpoint.id.toString());
                    if(idIter.toString().split("/")[0].equals(egressEndpoint.getUUID().toString())){
                        log.debug("Deleting intent to join {} -> {} with key "+key.toString(),two.toString(),one.toString());
                        key = service.getRegisterIntents().get(idIter).value();
                        // log.debug("intentKey "+key.toString());
                        service.getRegisterIntents().remove(idIter);
                        break;
                    }
                }
            }

            createIntentFlows(key, selector, treatment, service.getFlowPriority(), two, one,create);

        }

        log.debug("install-intents-flows-end");

    }
    @Override
    public  void installL1Flows(Endpoint endpoint, boolean create){

        log.debug("install-intents-flows-emptyVlan-start");
        ClosDeviceService service = get(ClosDeviceService.class);
        ApplicationId closFwdAppId = service.getAppId();

        if(endpoint instanceof OltControlEndpoint){
            // Only needed if olt does not use explicit vlan
            if(!((OltControlEndpoint)endpoint).getExplicitVlan()){
                selector = DefaultTrafficSelector.builder()
                    .matchInPort(endpoint.getPort())
                    .matchVlanId(VlanId.NONE)
                    .build();
                treatment = DefaultTrafficTreatment.builder()
                    .setVlanId(endpoint.getVlan()).build();

                error = filter(endpoint.getNode(), makeFilteringObjective(selector, treatment, closFwdAppId), true);
                processInstalledRuleObjectiveError(endpoint.getNode(), error);
            }
        }
        else if(endpoint instanceof ClientServiceBypassEndpoint){
            ClientServiceBypassEndpoint clientEndpointendpoint = (ClientServiceBypassEndpoint) endpoint;
            for(UUID serviceUUID: clientEndpointendpoint.getServiceUUIDs())
            {
                Endpoint serviceEndpoint = getEndpointFromUUID(serviceUUID);
                if (serviceEndpoint != null)
                {
                    if(create)
                        service.requestPackets(endpoint, serviceEndpoint);
                    else
                        service.withdrawPackets(endpoint, serviceEndpoint);
                }
            }
        }
        log.debug("install-intents-flows-emptyVlan-end");
    }

    private List<PortNumber> getPortsToSpineFromDevice(DeviceId deviceId, Endpoint destinationEndpoint) {
        ClosDeviceService service = get(ClosDeviceService.class);
        Iterator<Link> iteratorLinks = service.getLinkService().getDeviceEgressLinks(deviceId).iterator();
        List<PortNumber> ports = new ArrayList<>();
        while(iteratorLinks.hasNext()){
            Link link = iteratorLinks.next();
            // We do only conside active links...
            if(link.state() == Link.State.ACTIVE)
            {
                // Check if that spine let us reach destination leaf
                Iterator<Link> iteratorSpineLinks = service.getLinkService().getDeviceIngressLinks(link.dst().deviceId()).iterator();
                while(iteratorSpineLinks.hasNext())
                {
                    Link spineLink = iteratorSpineLinks.next();
                    if(link.state() == Link.State.ACTIVE)
                    {
                        if(spineLink.src().deviceId().equals(destinationEndpoint.getNode()))
                        {
                            PortNumber port = link.src().port();
                            ports.add(port);
                            log.debug("Port: "+port.toString()+" of DeviceId: "+deviceId.toString());
                        }
                    }
                }
            }
        }

        return ports;
    }

    private List<PortNumber> getPortsFromDeviceToDevice(DeviceId deviceId, DeviceId destinationDeviceId) {
        ClosDeviceService service = get(ClosDeviceService.class);
        Iterator<Link> iteratorLinks = service.getLinkService().getDeviceEgressLinks(deviceId).iterator();
        List<PortNumber> ports = new ArrayList<>();

        while(iteratorLinks.hasNext()){
            Link link = iteratorLinks.next();
            // We do only conside active links...
            if(link.state() == Link.State.ACTIVE)
            {
                // Check if that spine let us reach destination leaf
                Iterator<Link> iteratorSpineLinks = service.getLinkService().getDeviceEgressLinks(link.dst().deviceId()).iterator();
                while(iteratorSpineLinks.hasNext())
                {
                    Link spineLink = iteratorSpineLinks.next();
                    if(link.state() == Link.State.ACTIVE)
                    {
                        if(spineLink.dst().deviceId().equals(destinationDeviceId))
                        {
                            PortNumber port = link.src().port();
                            ports.add(port);
                            log.debug("Port: "+port.toString()+" of DeviceId: "+deviceId.toString());
                        }
                    }
                }
            }
        }
        return ports;
    }

    private List<PortNumber> getPortsFromSpineToDevice(DeviceId deviceId, DeviceId destinationDeviceId) {
        ClosDeviceService service = get(ClosDeviceService.class);
        Iterator<Link> iteratorLinks = service.getLinkService().getDeviceEgressLinks(deviceId).iterator();
        List<PortNumber> ports = new ArrayList<>();

        while(iteratorLinks.hasNext()){
            Link link = iteratorLinks.next();
            // We do only conside active links...
            if(link.state() == Link.State.ACTIVE)
            {
                if(link.dst().deviceId().equals(destinationDeviceId))
                {
                    PortNumber port = link.src().port();
                    ports.add(port);
                    log.debug("Port: "+port.toString()+" of DeviceId: "+deviceId.toString());
                }
            }
        }
        return ports;
    }

    @Override
    public  void installL2L3Flows(Endpoint endpoint, DeviceId deviceId, boolean create){

        log.debug("install-l2l3-flows-start");
        ClosDeviceService service = get(ClosDeviceService.class);
        VlanId emptyVlanId = VlanId.vlanId((short)service.getEmptyVlanId());
        VlanId serviceVlanId = VlanId.vlanId((short)service.getServiceVlanId());
        // VlanId extServiceVlanId = VlanId.vlanId((short)service.getExtServiceVlanId());
        int nextId = 0;
        ApplicationId appId;
        ApplicationId closFwdAppId = service.getAppId();
        MacAddress ctpdFakeInternalMacAddress = MacAddress.valueOf(service.getCtpdFakeInternalMacAddress());
        List<PortNumber> ports = getPortsToSpineFromDevice(deviceId, endpoint);

        if(ports.size() == 0){
            log.debug("no-flows-installed");
            return;
        }

        if(endpoint instanceof ServiceEndpoint || endpoint instanceof StorageEndpoint){

            log.debug("install-service-internal-start");

            Endpoint serviceEndpoint = null;

            if(endpoint instanceof ServiceEndpoint){
                serviceEndpoint = (ServiceEndpoint) endpoint;
            }else{
                serviceEndpoint = (StorageEndpoint) endpoint;

            }

            // First we install filter to convert traffic with no Vlan to emptyVlan

            selector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.ALL)
                .matchVlanId(VlanId.NONE)
                .build();
            treatment = DefaultTrafficTreatment.builder()
                .setVlanId(emptyVlanId).build();
                appId = service.getApplicationFlowId(endpoint);
                error = filter(deviceId, makeFilteringObjective(selector, treatment, closFwdAppId), true);

            processInstalledRuleObjectiveError(deviceId, error);

            // Then install service flow for access from VPDC to service...

            MacAddress broadcastMac = MacAddress.BROADCAST;


            if ((serviceEndpoint.getIpPrefix().isIp6()) && (serviceEndpoint.getSrcMacMask() == broadcastMac)){
                selector = DefaultTrafficSelector.builder()
                .matchEthSrc(serviceEndpoint.getSrcMac())
                // .matchEthSrcMasked(serviceEndpoint.getSrcMac(),serviceEndpoint.getSrcMacMask())
                .matchIPv6Dst(serviceEndpoint.getIpPrefix())
                .matchVlanId(emptyVlanId)
                .matchEthType((short)0x86dd)
                .build();

            } else if ((serviceEndpoint.getIpPrefix().isIp6()) && !(serviceEndpoint.getSrcMacMask() == broadcastMac)){
                selector = DefaultTrafficSelector.builder()
                .matchEthSrcMasked(serviceEndpoint.getSrcMac(),serviceEndpoint.getSrcMacMask())
                .matchIPv6Dst(serviceEndpoint.getIpPrefix())
                .matchVlanId(emptyVlanId)
                .matchEthType((short)0x86dd)
                .build();

            } else if (!(serviceEndpoint.getIpPrefix().isIp6()) && (serviceEndpoint.getSrcMacMask() == broadcastMac)){
                selector = DefaultTrafficSelector.builder()
                .matchEthSrc(serviceEndpoint.getSrcMac())
                // .matchEthSrcMasked(serviceEndpoint.getSrcMac(),serviceEndpoint.getSrcMacMask())
                .matchIPDst(serviceEndpoint.getIpPrefix())
                .matchVlanId(emptyVlanId)
                .matchEthType((short)0x0800)
                .build();

            } else if (!(serviceEndpoint.getIpPrefix().isIp6()) && !(serviceEndpoint.getSrcMacMask() == broadcastMac)) {
                selector = DefaultTrafficSelector.builder()
                .matchEthSrcMasked(serviceEndpoint.getSrcMac(),serviceEndpoint.getSrcMacMask())
                .matchIPDst(serviceEndpoint.getIpPrefix())
                .matchVlanId(emptyVlanId)
                .matchEthType((short)0x0800)
                .build();
            }

            if(deviceId.equals(endpoint.getNode())){

                // Mono leaf case

                treatmentList = new ArrayList<>();

                treatment = DefaultTrafficTreatment.builder()
                    .setEthDst(serviceEndpoint.getMac())
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setVlanId(serviceEndpoint.getVlan())
                    .setOutput(serviceEndpoint.getPort())
                    .build();

                treatmentList.add(treatment);

                nextId = service.getNextId(endpoint.getUUID(), create);
                appId = service.getApplicationFlowId(endpoint);
                if (create){
                    // nextIds.add(nextId);
                    error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(),appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }
                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(),appId), create);
                processInstalledRuleObjectiveError(deviceId, error);

            }
            else {
                // Different leafs case

                treatmentList = new ArrayList<>();

                if (service.getUseEcmp()){

                    for(PortNumber port  : ports){
                        treatment = DefaultTrafficTreatment.builder()
                            .setEthDst(service.getLeafDstMac(serviceEndpoint.getNode()))
                            .setEthSrc(ctpdFakeInternalMacAddress)
                            .setVlanId(serviceVlanId)
                            .setOutput(port)
                            .build();

                        treatmentList.add(treatment);
                    }

                    appId = service.getEcmpApplicationId();

                    if(service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                        nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                        log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                    }else if (!service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                        nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                        NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId);
                        error =  next(deviceId, nextObjective, create);
                        processInstalledRuleObjectiveError(deviceId, error);
                        log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                    }else{
                        nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), false);
                    }

                    error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);

                }else{

                    PortNumber port = ports.get(0);
                    treatment = DefaultTrafficTreatment.builder()
                            .setEthDst(service.getHostMac(serviceEndpoint.getNode(), serviceEndpoint.getPort()))
                            .setEthSrc(ctpdFakeInternalMacAddress)
                            .setVlanId(serviceVlanId)
                            .setOutput(port)
                            .build();

                    treatmentList.add(treatment);

                    nextId = service.getNextId(endpoint.getUUID(), create);
                    appId = service.getApplicationFlowId(endpoint);
                    if (create){
                        // nextIds.add(nextId);
                        error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId), create);
                        processInstalledRuleObjectiveError(deviceId, error);
                    }

                    error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);

                }

            }

            // Then install service flow for access from other service instance...

            // Mono leaf case

            if(deviceId.equals(endpoint.getNode()))
            {
                if (serviceEndpoint.getIpPrefix().isIp6()) {
                    selector = DefaultTrafficSelector.builder()
                    // .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPv6Dst(serviceEndpoint.getIpPrefix())
                    .matchVlanId(serviceEndpoint.getVlan())
                    .matchEthType((short)0x86dd)
                    .build();
                } else {
                    selector = DefaultTrafficSelector.builder()
                    // .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPDst(serviceEndpoint.getIpPrefix())
                    .matchVlanId(serviceEndpoint.getVlan())
                    .matchEthType((short)0x0800)
                    .build();
                }


                treatmentList = new ArrayList<>();

                // Single treatment in monoleaf case...

                treatment = DefaultTrafficTreatment.builder()
                    .setEthDst(serviceEndpoint.getMac())
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setVlanId(serviceEndpoint.getVlan())
                    .setOutput(serviceEndpoint.getPort())
                    .build();

                treatmentList.add(treatment);

                nextId = service.getNextId(endpoint.getUUID(), create);
                appId = service.getApplicationFlowId(endpoint);
                if (create){
                    // nextIds.add(nextId);
                    error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }
                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                processInstalledRuleObjectiveError(deviceId, error);
            }
            else {
                // Different leaf case

                if (serviceEndpoint.getIpPrefix().isIp6()) {
                    selector = DefaultTrafficSelector.builder()
                    // .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPv6Dst(serviceEndpoint.getIpPrefix())
                    .matchVlanId(serviceEndpoint.getVlan())
                    .matchEthType((short)0x86dd)
                    .build();
                } else {
                    selector = DefaultTrafficSelector.builder()
                    // .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPDst(serviceEndpoint.getIpPrefix())
                    .matchVlanId(serviceEndpoint.getVlan())
                    .matchEthType((short)0x0800)
                    .build();
                }

                treatmentList = new ArrayList<>();

                if (service.getUseEcmp()){

                    for(PortNumber port  : ports){
                        treatment = DefaultTrafficTreatment.builder()
                            .setEthDst(service.getLeafDstMac(serviceEndpoint.getNode()))
                            .setEthSrc(ctpdFakeInternalMacAddress)
                            .setVlanId(serviceVlanId)
                            .setOutput(port)
                            .build();

                        treatmentList.add(treatment);
                    }

                    appId = service.getEcmpApplicationId();

                    if(service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                        nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                        log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                    }else if (!service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                        nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                        NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId);
                        error =  next(deviceId, nextObjective, create);
                        processInstalledRuleObjectiveError(deviceId, error);
                        log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                    }else{
                        nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), false);
                    }

                    error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);

                }else{

                    PortNumber port = ports.get(0);
                    treatment = DefaultTrafficTreatment.builder()
                        .setEthDst(service.getHostMac(serviceEndpoint.getNode(), serviceEndpoint.getPort()))
                        .setEthSrc(ctpdFakeInternalMacAddress)
                        .setVlanId(serviceVlanId)
                        .setOutput(port)
                        .build();

                    treatmentList.add(treatment);

                    nextId = service.getNextId(endpoint.getUUID(), create);
                    appId = service.getApplicationFlowId(endpoint);
                    if (create){
                        // nextIds.add(nextId);
                        error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId), create);
                        processInstalledRuleObjectiveError(deviceId, error);
                    }

                    error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }
            }

            //Incoming packages form different leafs from int service (Service Vlan). Only install in endpoint leaf

            if(deviceId.equals(endpoint.getNode()))
            {
                if (serviceEndpoint.getIpPrefix().isIp6()) {
                    selector = DefaultTrafficSelector.builder()
                    // .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPv6Dst(serviceEndpoint.getIpPrefix())
                    .matchVlanId(serviceVlanId)
                    .matchEthType((short)0x86dd)
                    .build();
                } else {
                    selector = DefaultTrafficSelector.builder()
                    // .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPDst(serviceEndpoint.getIpPrefix())
                    .matchVlanId(serviceVlanId)
                    .matchEthType((short)0x0800)
                    .build();
                }

                treatmentList = new ArrayList<>();

                treatment = DefaultTrafficTreatment.builder()
                    .setEthDst(serviceEndpoint.getMac())
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setVlanId(serviceEndpoint.getVlan())
                    .setOutput(serviceEndpoint.getPort())
                    .build();

                treatmentList.add(treatment);

                nextId = service.getNextId(endpoint.getUUID(), create);
                appId = service.getApplicationFlowId(endpoint);
                if (create){
                    // nextIds.add(nextId);
                    error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }
                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                processInstalledRuleObjectiveError(deviceId, error);

            //Incoming packages form different leafs from ext service (Ext Service Vlan). Only install in endpoint leaf

                if (serviceEndpoint.getIpPrefix().isIp6()) {
                    selector = DefaultTrafficSelector.builder()
                    // .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPv6Dst(serviceEndpoint.getIpPrefix())
                    .matchVlanId(serviceVlanId)
                    .matchEthType((short)0x86dd)
                    .build();
                } else {
                    selector = DefaultTrafficSelector.builder()
                    // .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPDst(serviceEndpoint.getIpPrefix())
                    .matchVlanId(serviceVlanId)
                    .matchEthType((short)0x0800)
                    .build();
                }

                treatmentList = new ArrayList<>();

                treatment = DefaultTrafficTreatment.builder()
                    .setEthDst(serviceEndpoint.getMac())
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setVlanId(serviceEndpoint.getVlan())
                    .setOutput(serviceEndpoint.getPort())
                    .build();

                treatmentList.add(treatment);

                nextId = service.getNextId(endpoint.getUUID(), create);
                appId = service.getApplicationFlowId(endpoint);
                if (create){
                    // nextIds.add(nextId);
                    error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }
                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                processInstalledRuleObjectiveError(deviceId, error);
            }

            log.debug("install-service-internal-end");

        }

        if(endpoint instanceof ExternalServiceEndpoint){

            log.debug("install-service-external-start");

            ExternalServiceEndpoint serviceEndpoint = (ExternalServiceEndpoint) endpoint;

            // First we install filter to convert traffic with no Vlan to emptyVlan

                selector = DefaultTrafficSelector.builder()
                    .matchInPort(PortNumber.ALL)
                    .matchVlanId(VlanId.NONE)
                    .build();

                treatment = DefaultTrafficTreatment.builder()
                    .setVlanId(emptyVlanId).build();

            error = filter(deviceId, makeFilteringObjective(selector, treatment, closFwdAppId), true);
            processInstalledRuleObjectiveError(deviceId, error);

            // Then install service flow for access from VPDC to internet...

            MacAddress broadcastMac = MacAddress.BROADCAST;

            if (serviceEndpoint.getSrcMacMask() == broadcastMac){
                selector = DefaultTrafficSelector.builder()
                //.matchInPort(inputPort)
                .matchEthDst(ctpdFakeInternalMacAddress)
                .matchEthSrc(serviceEndpoint.getSrcMac())
                // .matchEthSrcMasked(serviceEndpoint.getSrcMac(),serviceEndpoint.getSrcMacMask())
                // .matchVlanId(emptyVlanId)
                .build();
            } else {
                selector = DefaultTrafficSelector.builder()
                //.matchInPort(inputPort)
                .matchEthDst(ctpdFakeInternalMacAddress)
                .matchEthSrcMasked(serviceEndpoint.getSrcMac(),serviceEndpoint.getSrcMacMask())
                // .matchVlanId(emptyVlanId)
                .build();
            }

            treatmentList = new ArrayList<>();

            if (service.getUseEcmp()){

                for(PortNumber port  : ports){
                    treatment = DefaultTrafficTreatment.builder()
                        .setEthDst(service.getLeafDstMac(serviceEndpoint.getNode()))
                        .setEthSrc(ctpdFakeInternalMacAddress)
                        .setVlanId(serviceVlanId)
                        .setOutput(port)
                        .build();

                    treatmentList.add(treatment);
                }

                appId = service.getEcmpApplicationId();

                appId = service.getEcmpApplicationId();

                if(service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                    log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                }else if (!service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                    NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority()-1, appId);
                    error =  next(deviceId, nextObjective, create);
                    processInstalledRuleObjectiveError(deviceId, error);
                    log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                }else{
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), false);
                }

                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority()-1, appId), create);
                processInstalledRuleObjectiveError(deviceId, error);

            }else{

                PortNumber port = ports.get(0);
                treatment = DefaultTrafficTreatment.builder()
                    .setEthDst(service.getHostMac(serviceEndpoint.getNode(), serviceEndpoint.getPort()))
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setVlanId(serviceVlanId)
                    .setOutput(port)
                    .build();

                treatmentList.add(treatment);

                nextId = service.getNextId(endpoint.getUUID(), create);
                appId = service.getApplicationFlowId(endpoint);
                if (create){
                    // nextIds.add(nextId);
                    error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority()-1, appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }

                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority()-1, appId), create);
                processInstalledRuleObjectiveError(deviceId, error);
            }

            // Mono leaf scenario is not posible for this enpoint as external links are in L4

            // Then install service flow for access from internal service to internet ...

            // We filter by ServicePrefix to avoid Vpdcs access internet using this flow

            Ip6Prefix serviceIp6Prefix = Ip6Prefix.valueOf(service.getIp6ServicePrefix());
            Ip4Prefix serviceIp4Prefix = Ip4Prefix.valueOf(service.getIp4ServicePrefix());

            if(serviceEndpoint.getIpPrefix().isIp6()) {
                selector = DefaultTrafficSelector.builder()
                    .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPv6Src(serviceIp6Prefix)
                    // .matchEthSrc(serviceEndpoint.getMac())
                    // .matchVlanId(serviceEndpoint.getVlan())
                    .matchEthType((short)0x86dd)
                    .build();
            }else{
                selector = DefaultTrafficSelector.builder()
                    .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPSrc(serviceIp4Prefix)
                    // .matchEthSrc(serviceEndpoint.getMac())
                    // .matchVlanId(serviceEndpoint.getVlan())
                    .matchEthType((short)0x0800)
                    .build();
            }

            treatmentList = new ArrayList<>();

            if (service.getUseEcmp()){

                for(PortNumber port  : ports){
                    treatment = DefaultTrafficTreatment.builder()
                        .setEthDst(service.getLeafDstMac(serviceEndpoint.getNode()))
                        .setEthSrc(ctpdFakeInternalMacAddress)
                        .setVlanId(serviceVlanId)
                        .setOutput(port)
                        .build();

                    treatmentList.add(treatment);
                }

                appId = service.getEcmpApplicationId();

                if(service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                    log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                }else if (!service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                    NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority()-1, appId);
                    error =  next(deviceId, nextObjective, create);
                    processInstalledRuleObjectiveError(deviceId, error);
                    log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                }else{
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), false);
                }

                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority()-1, appId), create);
                processInstalledRuleObjectiveError(deviceId, error);

            }else{

                PortNumber port = ports.get(0);
                treatment = DefaultTrafficTreatment.builder()
                    .setEthDst(service.getHostMac(serviceEndpoint.getNode(), serviceEndpoint.getPort()))
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setVlanId(serviceVlanId)
                    .setOutput(port)
                    .build();

                treatmentList.add(treatment);

                nextId = service.getNextId(endpoint.getUUID(), create);
                appId = service.getApplicationFlowId(endpoint);
                if (create){
                    // nextIds.add(nextId);
                    error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority()-1, appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }

                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority()-1, appId), create);
                processInstalledRuleObjectiveError(deviceId, error);
            }

            // Mono leaf scenario is not posible for this enpoint as external links are in L4

            log.debug("install-service-external-end");

        }

        if(endpoint instanceof VpdcHostEndpoint){

            log.debug("install-vpdchost-start");

            boolean hl4 = endpoint instanceof HL4Endpoint;
            VpdcHostEndpoint vpdcHostEndpoint = null;
            HL4Endpoint hl4Endpoint = null;

            if(hl4)
                hl4Endpoint = (HL4Endpoint) endpoint;
            else
                vpdcHostEndpoint = (VpdcHostEndpoint) endpoint;

            if (hl4)
            {
                // First we install filter to remove hl4 vlan

                selector = DefaultTrafficSelector.builder()
                    .matchInPort(hl4Endpoint.getPort())
                    .matchVlanId(hl4Endpoint.getVlan())
                    .build();
                treatment = DefaultTrafficTreatment.builder()
                    .setVlanId(emptyVlanId).build();
                error = filter(deviceId, makeFilteringObjective(selector, treatment, closFwdAppId), true);

                processInstalledRuleObjectiveError(deviceId, error);
            }

            // We create a list to add client IPs and Service IP of vpdcHosts. This is used to simplify code

            List<IpPrefix> vpdcHostIps = new ArrayList<>();

            for (IpPrefix ip : vpdcHostEndpoint.getIpClientList()){
                vpdcHostIps.add(ip);
            }
            for (IpPrefix ip : vpdcHostEndpoint.getIpServiceList()){
                vpdcHostIps.add(ip);
            }

            // Install flow to access to VPDC_HOST Clients IPs from Int Service and for access to VPDC_HOST Service IPs from Int Service

            for (IpPrefix ip : vpdcHostIps) {

                if (ip.isIp6()) {
                    selector = DefaultTrafficSelector.builder()
                    //.matchInPort(inputPort)
                    .matchEthDst(ctpdFakeInternalMacAddress)
                    // .matchEthSrcMasked(serviceEndpoint.getMacMask(),serviceEndpoint.getMacMask())
                    .matchIPv6Dst(ip)
                    // .matchVlanId(serviceEndpoint.getVlan())
                    // .matchInPort(serviceEndpoint.getPort())
                    .matchEthType((short)0x86dd)
                    .build();
                } else {
                    selector = DefaultTrafficSelector.builder()
                    //.matchInPort(inputPort)
                    .matchEthDst(ctpdFakeInternalMacAddress)
                    // .matchEthSrcMasked(serviceEndpoint.getMacMask(),serviceEndpoint.getMacMask())
                    .matchIPDst(ip)
                    // .matchVlanId(serviceEndpoint.getVlan())
                    // .matchInPort(serviceEndpoint.getPort())
                    .matchEthType((short)0x0800)
                    .build();
                }

                if(!deviceId.equals(endpoint.getNode()))
                {
                    // VPDC host and service in different leafs
                    treatmentList = new ArrayList<>();

                    if (service.getUseEcmp()){

                        for(PortNumber port  : ports){
                            treatment = DefaultTrafficTreatment.builder()
                                .setEthDst(service.getLeafDstMac(vpdcHostEndpoint.getNode()))
                                .setEthSrc(ctpdFakeInternalMacAddress)
                                .setVlanId(serviceVlanId)
                                .setOutput(port)
                                .build();

                            treatmentList.add(treatment);
                        }

                        appId = service.getEcmpApplicationId();

                        if(service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                            nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                            log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                        }else if (!service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                            nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                            NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId);
                            error =  next(deviceId, nextObjective, create);
                            processInstalledRuleObjectiveError(deviceId, error);
                            log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                        }else{
                            nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), false);
                        }

                        error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                        processInstalledRuleObjectiveError(deviceId, error);

                    }else{

                        PortNumber port = ports.get(0);
                        treatment = DefaultTrafficTreatment.builder()
                            .setEthDst(service.getHostMac(vpdcHostEndpoint.getNode(), vpdcHostEndpoint.getPort()))
                            .setEthSrc(ctpdFakeInternalMacAddress)
                            .setVlanId(serviceVlanId)
                            .setOutput(port)
                            .build();

                        treatmentList.add(treatment);
                        nextId = service.getNextId(endpoint.getUUID(), create);
                        appId = service.getApplicationFlowId(endpoint);
                        if (create){
                            // nextIds.add(nextId);
                            error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId), create);
                            processInstalledRuleObjectiveError(deviceId, error);
                        }

                        error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                        processInstalledRuleObjectiveError(deviceId, error);

                    }
                }
            }

            log.debug("install-vpdchost-end");

        }

        if(endpoint instanceof VpdcEndpoint){

            log.debug("install-vpdc-start");

            VpdcEndpoint vpdcEndpoint = (VpdcEndpoint) endpoint;

            //Only install VpdcEndpoint flows in Leaf in which VPDC is allocated

            if(deviceId.equals(vpdcEndpoint.getNode()))
            {

                // We create a list to add client IPs and Service IP of VPDC. This is used to simplify code

                List<IpPrefix> vpdcIps = new ArrayList<>();
                vpdcIps.add(vpdcEndpoint.getIpClient());
                vpdcIps.add(vpdcEndpoint.getIpService());

                for (IpPrefix ip : vpdcIps) {

                    // Install flows to allow access from services monoleaf to VPDC

                    treatmentList = new ArrayList<>();
                        //.matchInPort(inputPort)

                    if (ip.isIp6()) {
                        selector = DefaultTrafficSelector.builder()
                            .matchEthDst(ctpdFakeInternalMacAddress)
                            .matchIPv6Dst(ip)
                            .matchEthType((short)0x86dd)
                            .build();
                    }else{
                        selector = DefaultTrafficSelector.builder()
                            .matchEthDst(ctpdFakeInternalMacAddress)
                            .matchIPDst(ip)
                            .matchEthType((short)0x0800)
                            .build();
                    }

                    treatment = DefaultTrafficTreatment.builder()
                        .setEthDst(vpdcEndpoint.getMac())
                        .setEthSrc(ctpdFakeInternalMacAddress)
                        // .setVlanId(serviceEndpoint.getVlan())
                        .setVlanId(emptyVlanId) // We set to empty to avoid driver complain. No real effect as pop removes it in L2 group
                        .popVlan()
                        .setOutput(vpdcEndpoint.getPort())
                        .build();

                    treatmentList.add(treatment);

                    // We does not need higher priority because selector is different to VpdcHost

                    nextId = service.getNextId(endpoint.getUUID(), create);
                    appId = service.getApplicationFlowId(endpoint);
                    if (create){
                        // nextIds.add(nextId);
                        error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId), create);
                        processInstalledRuleObjectiveError(deviceId, error);
                    }
                    error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);

                    // Install flows for access VPDC from different leafs
                    treatmentList = new ArrayList<>();

                    MacAddress leafOrHostDst = null;

                    if (service.getUseEcmp()){
                        leafOrHostDst = service.getLeafDstMac(vpdcEndpoint.getNode());
                    }else{
                        leafOrHostDst = service.getHostMac(vpdcEndpoint.getNode(), vpdcEndpoint.getPort());
                    }

                    if (ip.isIp6()) {
                        selector = DefaultTrafficSelector.builder()
                            //.matchInPort(inputPort)
                            //.matchEthDst(ctpdFakeInternalMacAddress)
                            .matchEthDst(leafOrHostDst)
                            .matchIPv6Dst(ip)
                            // .matchVlanId(emptyVlanId)
                            .matchEthType((short)0x86dd)
                            .build();
                    }else{
                        selector = DefaultTrafficSelector.builder()
                            //.matchInPort(inputPort)
                            //.matchEthDst(ctpdFakeInternalMacAddress)
                            .matchEthDst(leafOrHostDst)
                            .matchIPDst(ip)
                            // .matchVlanId(emptyVlanId)
                            .matchEthType((short)0x0800)
                            .build();
                    }

                    treatment = DefaultTrafficTreatment.builder()
                        .setEthDst(vpdcEndpoint.getMac())
                        // .setEthSrc(ctpdFakeInternalMacAddress)
                        // .setVlanId(serviceEndpoint.getVlan())
                        .setVlanId(emptyVlanId) // We set to empty to avoid driver complain. No real effect as pop removes it in L2 group
                        .popVlan()
                        .setOutput(vpdcEndpoint.getPort())
                        .build();

                    treatmentList.add(treatment);

                    // We does not need higher priority because selector is different to VpdcHost
                    nextId = service.getNextId(endpoint.getUUID(), create);
                    appId = service.getApplicationFlowId(endpoint);
                    if (create){
                        // nextIds.add(nextId);
                        error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId), create);
                        processInstalledRuleObjectiveError(deviceId, error);
                    }
                    error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }
            }

            // Install flows to allow access to VPDC from internal and external services

            // treatmentList = new ArrayList<>();

            // selector = DefaultTrafficSelector.builder()
            //     //.matchInPort(inputPort)
            //     //.matchEthDst(ctpdFakeInternalMacAddress)
            //     .matchEthDst(service.getHostMac(vpdcEndpoint.getNode(), vpdcEndpoint.getPort()))
            //     .matchIPv6Dst(vpdcEndpoint.getIpPrefix())
            //     // .matchVlanId(emptyVlanId)
            //     .matchEthType((short)0x86dd)
            //     .build();

            // treatment = DefaultTrafficTreatment.builder()
            //     .setEthDst(vpdcEndpoint.getMac())
            //     // .setEthSrc(ctpdFakeInternalMacAddress)
            //     // .setVlanId(serviceEndpoint.getVlan())
            //     .setVlanId(emptyVlanId) // We set to empty to avoid driver complain. No real effect as pop removes it in L2 group
            //     .popVlan()
            //     .setOutput(vpdcEndpoint.getPort())
            //     .build();

            // treatmentList.add(treatment);

            // nextId = service.getFlowObjectiveService().allocateNextId();
            // if (create){
            //     nextIds.add(nextId);
            //     error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority()), create);
            //     processInstalledRuleObjectiveError(deviceId, error);
            // }
            // error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority()), create);
            // processInstalledRuleObjectiveError(deviceId, error);

            log.debug("install-vpdc-end");
        }
        log.debug("install-l2l3-flows-end");
    }

    @Override
    public  void installL4Flows(Endpoint endpoint, DeviceId deviceId, boolean create){

        log.debug("install-l4-flows-start");

        int nextId;
        ClosDeviceService service = get(ClosDeviceService.class);
        List<Integer> nextIds = new ArrayList<Integer>();
        ApplicationId appId;
        ApplicationId closFwdAppId = service.getAppId();
        nextId=0;
        VlanId emptyVlanId = VlanId.vlanId((short)service.getEmptyVlanId());
        VlanId serviceVlanId = VlanId.vlanId((short)service.getServiceVlanId());
        MacAddress ctpdFakeInternalMacAddress = MacAddress.valueOf(service.getCtpdFakeInternalMacAddress());
        List<PortNumber> ports = getPortsToSpineFromDevice(deviceId, endpoint);

        if(ports.size() == 0){
            log.debug("no-flows-installed");
            return;
        }

        if(endpoint instanceof ServiceEndpoint || endpoint instanceof StorageEndpoint){

            log.debug("install-service-internal-start");

            Endpoint serviceEndpoint = null;

            if(endpoint instanceof ServiceEndpoint){
                serviceEndpoint = (ServiceEndpoint) endpoint;
            }else{
                serviceEndpoint = (StorageEndpoint) endpoint;

            }

            log.debug("install-service-internal-start");

            if(serviceEndpoint.getExternalAccessFlag())
            {
                // Trafic from internet to internal service

                if (serviceEndpoint.getIpPrefix().isIp6()) {
                    selector = DefaultTrafficSelector.builder()
                    //.matchInPort(inputPort)
                    .matchEthDst(ctpdFakeInternalMacAddress)
                    // .matchEthSrcMasked(serviceEndpoint.getMacMask(),serviceEndpoint.getMacMask())
                    .matchIPv6Dst(serviceEndpoint.getIpPrefix())
                    // .matchVlanId(serviceEndpoint.getVlan())
                    .matchEthType((short)0x86dd)
                    .build();
                } else {
                    selector = DefaultTrafficSelector.builder()
                    //.matchInPort(inputPort)
                    //.matchEthDst(ctpdFakeInternalMacAddress)
                    .matchEthDst(ctpdFakeInternalMacAddress)
                    .matchIPDst(serviceEndpoint.getIpPrefix())
                    // .matchVlanId(serviceEndpoint.getVlan())
                    .matchEthType((short)0x0800)
                    .build();
                }

                treatmentList = new ArrayList<>();

                if (service.getUseEcmp()){

                    for(PortNumber port  : ports){
                        treatment = DefaultTrafficTreatment.builder()
                            // .setEthDst(serviceEndpoint.getMac())
                            .setEthSrc(ctpdFakeInternalMacAddress)
                            .setEthDst(service.getLeafDstMac(serviceEndpoint.getNode()))
                            .setVlanId(serviceVlanId)
                            .setOutput(port)
                            .build();

                        treatmentList.add(treatment);
                    }

                    appId = service.getEcmpApplicationId();

                    if(service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                        nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                        log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                    }else if (!service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                        nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                        NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId);
                        error =  next(deviceId, nextObjective, create);
                        processInstalledRuleObjectiveError(deviceId, error);
                        log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                    }else{
                        nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), false);
                    }

                    error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);

                }else{

                    PortNumber port = ports.get(0);
                    treatment = DefaultTrafficTreatment.builder()
                        // .setEthDst(serviceEndpoint.getMac())
                        .setEthSrc(ctpdFakeInternalMacAddress)
                        .setEthDst(service.getHostMac(serviceEndpoint.getNode(), serviceEndpoint.getPort()))
                        .setVlanId(serviceVlanId)
                        .setOutput(port)
                        .build();

                    treatmentList.add(treatment);

                    nextId = service.getNextId(endpoint.getUUID(), create);
                    appId = service.getApplicationFlowId(endpoint);
                    if (create){
                        // nextIds.add(nextId);
                        error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId), create);
                        processInstalledRuleObjectiveError(deviceId, error);
                    }

                    error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }
            }
            log.debug("install-service-internal-end");
        }

        if(endpoint instanceof ExternalServiceEndpoint){

            ExternalServiceEndpoint serviceEndpoint = (ExternalServiceEndpoint) endpoint;

            log.debug("install-service-external-start");

            // Install flow in L4 to access external service

            MacAddress leafOrHostDst = null;

            if (service.getUseEcmp()){
                leafOrHostDst = service.getLeafDstMac(serviceEndpoint.getNode());
            }else{
                leafOrHostDst = service.getHostMac(serviceEndpoint.getNode(), serviceEndpoint.getPort());
            }

            if(serviceEndpoint.getIpPrefix().isIp6()){

                selector = DefaultTrafficSelector.builder()
                .matchEthDst(leafOrHostDst)
                .matchEthType((short)0x86dd)
                .build();

            }else{
                selector = DefaultTrafficSelector.builder()
                .matchEthDst(leafOrHostDst)
                .matchEthType((short)0x0800)
                .build();
            }


            // if (service.getUseEcmp()){
            //     selector = DefaultTrafficSelector.builder()
            //     .matchEthDst(service.getLeafDstMac(serviceEndpoint.getNode()))
            //     .build();

            // }else{
            //     selector = DefaultTrafficSelector.builder()
            //     .matchEthDst(service.getHostMac(serviceEndpoint.getNode(), serviceEndpoint.getPort()))
            //     .build();
            // }

            treatmentList = new ArrayList<>();

            treatment = DefaultTrafficTreatment.builder()
                .setEthDst(serviceEndpoint.getMac())
                .setEthSrc(ctpdFakeInternalMacAddress)
                .setVlanId(serviceEndpoint.getVlan())
                .setOutput(serviceEndpoint.getPort())
                .build();

            treatmentList.add(treatment);

            nextId = service.getNextId(endpoint.getUUID(), create);
            appId = service.getApplicationFlowId(endpoint);
            if (create){
                // nextIds.add(nextId);
                error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), closFwdAppId), create);
                processInstalledRuleObjectiveError(deviceId, error);
            }
            error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), closFwdAppId), create);
            processInstalledRuleObjectiveError(deviceId, error);

            log.debug("install-service-external-end");
        }

        if(endpoint instanceof VpdcHostEndpoint){

            log.debug("install-vpdchost-start");

            VpdcHostEndpoint vpdcHostEndpoint = (VpdcHostEndpoint) endpoint;

            // Install flow to access to VPDC_HOST Client IPs from Ext Service. Never allow access form Ext Services to VPDCs service IPs

            if(vpdcHostEndpoint.getExternalAccessFlag())
            {
                for (IpPrefix ip : vpdcHostEndpoint.getIpClientList()) {

                    if (ip.isIp6()) {
                        selector = DefaultTrafficSelector.builder()
                        //.matchInPort(inputPort)
                            .matchEthDst(ctpdFakeInternalMacAddress)
                            // .matchEthSrcMasked(serviceEndpoint.getMacMask(),serviceEndpoint.getMacMask())
                            .matchIPv6Dst(ip)
                            // .matchVlanId(serviceEndpoint.getVlan())
                            // .matchInPort(serviceEndpoint.getPort())
                            .matchEthType((short)0x86dd)
                            .build();
                    }else{
                        selector = DefaultTrafficSelector.builder()
                        //.matchInPort(inputPort)
                            .matchEthDst(ctpdFakeInternalMacAddress)
                            // .matchEthSrcMasked(serviceEndpoint.getMacMask(),serviceEndpoint.getMacMask())
                            .matchIPDst(ip)
                            // .matchVlanId(serviceEndpoint.getVlan())
                            // .matchInPort(serviceEndpoint.getPort())
                            .matchEthType((short)0x0800)
                            .build();
                    }

                    treatmentList = new ArrayList<>();

                    if (service.getUseEcmp()){

                        for(PortNumber port  : ports){
                            treatment = DefaultTrafficTreatment.builder()
                                .setEthDst(service.getLeafDstMac(vpdcHostEndpoint.getNode()))
                                .setEthSrc(ctpdFakeInternalMacAddress)
                                .setVlanId(serviceVlanId)
                                .setOutput(port)
                                .build();

                            treatmentList.add(treatment);
                        }

                        appId = service.getEcmpApplicationId();

                        if(service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                            nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                            log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                        }else if (!service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                            nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                            NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId);
                            error =  next(deviceId, nextObjective, create);
                            processInstalledRuleObjectiveError(deviceId, error);
                            log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                        }else{
                            nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), false);
                        }

                        error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                        processInstalledRuleObjectiveError(deviceId, error);

                    }else{

                        PortNumber port = ports.get(0);
                        treatment = DefaultTrafficTreatment.builder()
                            .setEthDst(service.getHostMac(vpdcHostEndpoint.getNode(), vpdcHostEndpoint.getPort()))
                            .setEthSrc(ctpdFakeInternalMacAddress)
                            .setVlanId(serviceVlanId)
                            .setOutput(port)
                            .build();

                        treatmentList.add(treatment);

                        nextId = service.getNextId(endpoint.getUUID(), create);
                        appId = service.getApplicationFlowId(endpoint);
                        if (create){
                            // nextIds.add(nextId);
                            error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId), create);
                            processInstalledRuleObjectiveError(deviceId, error);
                        }

                        error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                        processInstalledRuleObjectiveError(deviceId, error);
                    }
                }
            }

            log.debug("install-vpdchost-end");

        }
        log.debug("install-l4-flows-end");
    }

    @Override
    public  void installSpineFlows(Endpoint endpoint, DeviceId deviceId, boolean create){

        log.debug("install-spine-flows-start");

        int nextId;
        ClosDeviceService service = get(ClosDeviceService.class);
        List<Integer> nextIds = new ArrayList<Integer>();
        ApplicationId appId;
        ApplicationId closFwdAppId = service.getAppId();
        nextId=0;
        VlanId emptyVlanId = VlanId.vlanId((short)service.getEmptyVlanId());
        VlanId serviceVlanId = VlanId.vlanId((short)service.getServiceVlanId());
        VlanId bypassVlanId = VlanId.vlanId((short)service.getBypassVlanId());
        MacAddress ctpdFakeInternalMacAddress = MacAddress.valueOf(service.getCtpdFakeInternalMacAddress());

        // We let traffic comming on service_vlan to pass unaltered (to avoid matchin VlanId.ANY match in bypass case)

        List<PortNumber>ports = getPortsFromDeviceToDevice(deviceId, endpoint.getNode());

        for (PortNumber port: ports)
        {
            selector = DefaultTrafficSelector.builder()
                .matchVlanId(serviceVlanId)
                .matchInPort(port)
                .build();

            // No action
            treatment = DefaultTrafficTreatment.builder().build();
            appId = service.getApplicationFlowId(endpoint);

            error = filter(deviceId, makeFilteringObjective(selector, treatment, PacketPriority.CONTROL.priorityValue(), closFwdAppId), true);
            processInstalledRuleObjectiveError(deviceId, error);
        }

        // We now install flows for service vlan ...

        ports = new ArrayList<>();

        Iterator <Link> iteratorSpinesLinks = service.getLinkService().getDeviceEgressLinks(deviceId).iterator();
        while(iteratorSpinesLinks.hasNext()){
            // We get all ports that connects spine with endpoint leaf
            Link link = iteratorSpinesLinks.next();
            if(link.state() == Link.State.ACTIVE)
            {
                log.debug("Checking "+link.dst().deviceId().toString()+" with "+endpoint.getNode().toString());
                if (link.dst().deviceId().equals(endpoint.getNode())){
                    log.debug("DeviceId match found");
                    PortNumber port = link.src().port();
                    ports.add(port);
                    log.debug("Port: "+port.toString()+" of DeviceId: "+deviceId.toString());
                }
            }
        }

        if(ports.size() == 0){
            log.debug("no-flows-installed");
            return;
        }

        if (endpoint instanceof ServiceEndpoint  ||
            endpoint instanceof ExternalServiceEndpoint ||
            endpoint instanceof VpdcHostEndpoint ||
            endpoint instanceof StorageEndpoint) {

            log.debug("install-serviceVlan-flows-start");

            // Internal service routing...

            if (service.getUseEcmp()){
                selector = DefaultTrafficSelector.builder()
                    .matchVlanId(serviceVlanId)
                    .matchEthDst(service.getLeafDstMac(endpoint.getNode()))
                    .build();
            }else{
                selector = DefaultTrafficSelector.builder()
                    .matchVlanId(serviceVlanId)
                    .matchEthDst(service.getHostMac(endpoint.getNode(),endpoint.getPort()))
                    .build();
            }


            treatmentList = new ArrayList<>();

            if (service.getUseEcmp()){

                for(PortNumber port  : ports){
                    treatment = DefaultTrafficTreatment.builder()
                        .setOutput(port)
                        .setVlanId(serviceVlanId)
                        .setEthSrc(ctpdFakeInternalMacAddress)
                        .setEthDst(service.getLeafDstMac(endpoint.getNode()))
                        .build();

                    treatmentList.add(treatment);
                }

                appId = service.getEcmpApplicationId();

                if(service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                    log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                }else if (!service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                    NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId);
                    error =  next(deviceId, nextObjective, create);
                    processInstalledRuleObjectiveError(deviceId, error);
                    log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                }else{
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), false);
                }

                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                processInstalledRuleObjectiveError(deviceId, error);

            }else{

                PortNumber port = ports.get(0);
                treatment = DefaultTrafficTreatment.builder()
                    .setOutput(port)
                    .setVlanId(serviceVlanId)
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setEthDst(service.getHostMac(endpoint.getNode(), endpoint.getPort()))
                    .build();

                treatmentList.add(treatment);

                nextId = service.getNextId(endpoint.getUUID(), create);
                appId = service.getApplicationFlowId(endpoint);
                if (create){
                    // nextIds.add(nextId);
                    error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), closFwdAppId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }

                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), closFwdAppId), create);
                processInstalledRuleObjectiveError(deviceId, error);
            }

            // // External service routing

            // if (service.getUseEcmp()){

            //     selector = DefaultTrafficSelector.builder()
            //         .matchVlanId(extServiceVlanId)
            //         .matchEthDst(service.getLeafDstMac(endpoint.getNode()))
            //         .build();
            // }else{
            //         selector = DefaultTrafficSelector.builder()
            //         .matchVlanId(extServiceVlanId)
            //         .matchEthDst(service.getHostMac(endpoint.getNode(),endpoint.getPort()))
            //         .build();
            // }

            // treatmentList = new ArrayList<>();

            // if (service.getUseEcmp()){

            //     for(PortNumber port  : ports){
            //         treatment = DefaultTrafficTreatment.builder()
            //             .setOutput(port)
            //             .setVlanId(extServiceVlanId)
            //             .setEthSrc(ctpdFakeInternalMacAddress)
            //             .setEthDst(service.getLeafDstMac(endpoint.getNode()))
            //             .build();

            //         treatmentList.add(treatment);
            //     }

            //     appId = service.getEcmpApplicationId();

            //     if(service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
            //         nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
            //         log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

            //     }else if (!service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
            //         nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
            //         NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId);
            //         error =  next(deviceId, nextObjective, create);
            //         processInstalledRuleObjectiveError(deviceId, error);
            //         log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

            //     }else{
            //         nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), false);
            //     }

            //     error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
            //     processInstalledRuleObjectiveError(deviceId, error);

            // }else{

        //         PortNumber port = ports.get(0);
        //         treatment = DefaultTrafficTreatment.builder()
        //             .setOutput(port)
        //             .setVlanId(extServiceVlanId)
        //             .setEthSrc(ctpdFakeInternalMacAddress)
        //             .setEthDst(service.getHostMac(endpoint.getNode(), endpoint.getPort()))
        //             .build();

        //         treatmentList.add(treatment);

        //         nextId = service.getNextId(endpoint.getUUID(), create);
        //         appId = service.getApplicationFlowId(endpoint);
        //         if (create){
        //             // nextIds.add(nextId);
        //             error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), closFwdAppId), create);
        //             processInstalledRuleObjectiveError(deviceId, error);
        //         }

        //         error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), closFwdAppId), create);
        //         processInstalledRuleObjectiveError(deviceId, error);
        //     }

        //     log.debug("install-serviceVlan-flows-end");

        }

        if (endpoint instanceof OltEndpoint) {

            log.debug("install-bypassVlan-flows-start");


            // and also for bypass vlan, translating into service

            if (service.getUseEcmp()){
                selector = DefaultTrafficSelector.builder()
                    .matchVlanId(bypassVlanId)
                    .matchEthDst(service.getLeafDstMac(endpoint.getNode()))
                    .build();
            }else{
                selector = DefaultTrafficSelector.builder()
                    .matchVlanId(bypassVlanId)
                    .matchEthDst(service.getHostMac(endpoint.getNode(), endpoint.getPort()))
                    .build();
            }

            treatmentList = new ArrayList<>();

            if (service.getUseEcmp()){

                for(PortNumber port  : ports){
                    treatment = DefaultTrafficTreatment.builder()
                        .setOutput(port)
                        .setVlanId(serviceVlanId)
                        .setEthSrc(ctpdFakeInternalMacAddress)
                        .setEthDst(service.getLeafDstMac(endpoint.getNode()))
                        .build();

                    treatmentList.add(treatment);
                }

                appId = service.getEcmpApplicationId();

                if(service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                    log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                }else if (!service.checkIfEcmpExists(deviceId, endpoint.getNode()) && create){
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), true);
                    NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId);
                    error =  next(deviceId, nextObjective, create);
                    processInstalledRuleObjectiveError(deviceId, error);
                    log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, deviceId, endpoint.getNode());

                }else{
                    nextId = service.getNextIdForEcmp(deviceId, endpoint.getNode(), false);
                }

                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                processInstalledRuleObjectiveError(deviceId, error);

            }else{

                PortNumber port = ports.get(0);
                treatment = DefaultTrafficTreatment.builder()
                    .setOutput(port)
                    .setVlanId(serviceVlanId)
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setEthDst(service.getHostMac(endpoint.getNode(), endpoint.getPort()))
                    .build();

                treatmentList.add(treatment);

                nextId = service.getNextId(endpoint.getUUID(), create);
                appId = service.getApplicationFlowId(endpoint);

                if (create){
                    // nextIds.add(nextId);
                    error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), closFwdAppId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }

                error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority(), closFwdAppId), create);
                processInstalledRuleObjectiveError(deviceId, error);
            }

            log.debug("install-bypassVlan-flows-end");

        }

        log.debug("install-spine-flows-end");


        // if(endpoint instanceof VpdcHostEndpoint){

        //     VpdcHostEndpoint vpdcHostEndpoint = (VpdcHostEndpoint) endpoint;

        //     selector = DefaultTrafficSelector.builder()
        //         .matchVlanId(serviceVlanId)
        //         .matchEthDst(service.getHostMac(vpdcHostEndpoint.getNode(), vpdcHostEndpoint.getPort()))
        //         .build();

        //     treatmentList = new ArrayList<>();

        //     for(PortNumber port  : ports){
        //         treatment = DefaultTrafficTreatment.builder()
        //             .setOutput(port)
        //             .setVlanId(serviceVlanId)
        //             .setEthSrc(ctpdFakeInternalMacAddress)
        //             .setEthDst(service.getHostMac(vpdcHostEndpoint.getNode(), vpdcHostEndpoint.getPort()))
        //             .build();
        //         treatmentList.add(treatment);
        //     }

        //     nextId = service.getFlowObjectiveService().allocateNextId();
        //     if (create){
        //         nextIds.add(nextId);
        //         error = next(deviceId, makeNextObjective(treatmentList, nextId, null, service.getFlowPriority()), create);
        //         processInstalledRuleObjectiveError(deviceId, error);
        //     }
        //     error = forward(deviceId, makeForwardingObjective(selector, nextId, service.getFlowPriority()), create);
        //     processInstalledRuleObjectiveError(deviceId, error);

        // }
    }

    public void installBypassTemporaryL1Flows(ClientServiceBypassEndpoint clientEndpoint, ServiceEndpoint serviceEndpoint, MacAddress clientObservedMac, IpPrefix observedClientIp, boolean create) {

        ClosDeviceService service = get(ClosDeviceService.class);
        MacAddress ctpdFakeInternalMacAddress = MacAddress.valueOf(service.getCtpdFakeInternalMacAddress());
        VlanId bypassVlanId = VlanId.vlanId((short)service.getBypassVlanId());
        VlanId emptyVlanId = VlanId.vlanId((short)service.getEmptyVlanId());
        int nextId;
        ApplicationId appId;
        TrafficSelector selector;
        TrafficTreatment treatment;

        log.debug("bypass-temporary-l1-flows-start");

        if(serviceEndpoint != null){

            /*  IP Client to INT Service */

            if(clientEndpoint.getIpPrefix().isIp6())
            {
                selector = DefaultTrafficSelector.builder()
                    .matchInPort(clientEndpoint.getPort())
                    .matchVlanId(clientEndpoint.getVlan())
                    .matchEthType((short)0x86dd)
                    .matchEthSrc(clientObservedMac)
                    .matchIPv6Src(observedClientIp)
                    .matchIPv6Dst(serviceEndpoint.getIpPrefix())
                    .build();
            }
            else
            {
                selector = DefaultTrafficSelector.builder()
                    .matchInPort(clientEndpoint.getPort())
                    .matchVlanId(clientEndpoint.getVlan())
                    .matchEthType((short)0x0800)
                    .matchEthSrc(clientObservedMac)
                    .matchIPSrc(observedClientIp)
                    .matchIPDst(serviceEndpoint.getIpPrefix())
                    .build();
            }

            List<PortNumber> ports = getPortsToSpineFromDevice(clientEndpoint.getNode(), serviceEndpoint);

            treatmentList = new ArrayList<>();

            if (service.getUseEcmp()){

                // for(PortNumber port  : ports){
                    PortNumber port = ports.get(0);
                    treatment = DefaultTrafficTreatment.builder()
                        .setEthSrc(ctpdFakeInternalMacAddress)
                        .setEthDst(service.getLeafDstMac(serviceEndpoint.getNode()))
                        //.popVLAN()
                        //.setVlanId(extraVlan)
                        .setVlanId(bypassVlanId)
                        .setOutput(port)
                        .build();
                    treatmentList.add(treatment);
                // }

                // appId = service.getEcmpApplicationId();

                // if(service.checkIfEcmpExists(clientEndpoint.getNode(), serviceEndpoint.getNode()) && create){
                //     nextId = service.getNextIdForEcmp(clientEndpoint.getNode(), serviceEndpoint.getNode(), true);
                //     log.debug("Ecmp exists. Going to use storage nextId {} to go from {} to {}", nextId, clientEndpoint.getNode(), serviceEndpoint.getNode());

                // }else if (!service.checkIfEcmpExists(clientEndpoint.getNode(), serviceEndpoint.getNode()) && create){
                //     nextId = service.getNextIdForEcmp(clientEndpoint.getNode(), serviceEndpoint.getNode(), true);
                //     NextObjective.Builder nextObjective = makeNextObjective(treatmentList, nextId, null, service.getFlowPriority(), appId);
                //     error =  next(clientEndpoint.getNode(), nextObjective, create);
                //     processInstalledRuleObjectiveError(clientEndpoint.getNode(), error);
                //     log.debug("Ecmp not exists. Going to add nextObjective and storage nextId {} to go from {} to {}", nextId, clientEndpoint.getNode(), serviceEndpoint.getNode());

                // }else{
                //     nextId = service.getNextIdForEcmp(clientEndpoint.getNode(), serviceEndpoint.getNode(), false);
                // }

                // error = forward(clientEndpoint.getNode(), makeForwardingObjective(selector, nextId, service.getFlowPriority(), appId), create);
                // processInstalledRuleObjectiveError(clientEndpoint.getNode(), error);

            }else{

                PortNumber port = ports.get(0);
                treatment = DefaultTrafficTreatment.builder()
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setEthDst(service.getHostMac(serviceEndpoint.getNode(), serviceEndpoint.getPort()))
                    //.popVLAN()
                    //.setVlanId(extraVlan)
                    .setVlanId(bypassVlanId)
                    .setOutput(port)
                    .build();

                treatmentList.add(treatment);
            }

            nextId = service.getNextId(clientEndpoint.getUUID(), create);
            appId = service.getBypassApplicationId();

            if (create){
                error = next(clientEndpoint.getNode(), makeTmpNextObjective(treatmentList, nextId, null, service.getBypassFlowPriority(), appId), create);
                processInstalledRuleObjectiveError(clientEndpoint.getNode(), error);
            }
            error = forward(clientEndpoint.getNode(), makeTmpForwardingObjective(selector, nextId, service.getBypassFlowPriority(), appId), create);
            processInstalledRuleObjectiveError(clientEndpoint.getNode(), error);

        }

        /* IP INT Service to client */

        treatmentList = new ArrayList<>();

        if(clientEndpoint.getIpPrefix().isIp6())
        {
            selector = DefaultTrafficSelector.builder()
                // .matchInPort(outputPort)
                .matchVlanId(bypassVlanId)
                .matchEthType((short)0x86dd)
                .matchIPv6Dst(clientEndpoint.getIpPrefix())
                .matchIPv6Src(serviceEndpoint.getIpPrefix())
                .build();
        }
        else{
            selector = DefaultTrafficSelector.builder()
                // .matchInPort(outputPort)
                .matchVlanId(bypassVlanId)
                .matchEthType((short)0x0800)
                .matchIPDst(clientEndpoint.getIpPrefix())
                .matchIPSrc(serviceEndpoint.getIpPrefix())
                .build();
        }

        /*	Next Objective */

        treatment = DefaultTrafficTreatment.builder()
                .setEthSrc(ctpdFakeInternalMacAddress)
                // Set observed mac
                .setEthDst(clientObservedMac)
                // Set Stag rewiriting bypassvlan
                .setVlanId(clientEndpoint.getVlan())
                .setOutput(clientEndpoint.getPort())
                .build();

        treatmentList.add(treatment);

        nextId = service.getNextId(clientEndpoint.getUUID(), create);
        appId = service.getBypassApplicationId();

        error = next(clientEndpoint.getNode(), makeTmpNextObjective(treatmentList, nextId, null, service.getBypassFlowPriority(), appId), create);
        processInstalledRuleObjectiveError(clientEndpoint.getNode(), error);

        error = forward(clientEndpoint.getNode(), makeTmpForwardingObjective(selector, nextId, service.getBypassFlowPriority(), appId), create);
        processInstalledRuleObjectiveError(clientEndpoint.getNode(), error);

        log.debug("bypass-temporary-l1-flows-end");

    }

    public void installBypassTemporarySpineFlows(ClientServiceBypassEndpoint clientEndpoint, ServiceEndpoint serviceEndpoint, boolean create) {

        ClosDeviceService service = get(ClosDeviceService.class);
        VlanId bypassVlanId = VlanId.vlanId((short)service.getBypassVlanId());
        int flowPriority = service.getFlowPriority();
        List<Integer> nextIds = new ArrayList<Integer>();
        int nextId;
        ApplicationId closFwdAppId = service.getAppId();
        ApplicationId appId;
        TrafficSelector selector;
        TrafficTreatment treatment;

        log.debug("bypass-temporary-spine-flows-start");

        // From service to client
        if(!clientEndpoint.getInnerVlan().equals(VlanId.NONE))
        {
            Collection<Versioned<Device>> collectionSpinesId = service.getSpineDevices().values();
            Iterator<Versioned<Device>> iteratorSpines = collectionSpinesId.iterator();
            // log.debug("Number of spines: "+collectionSpinesId.size());

            while(iteratorSpines.hasNext()){
                DeviceId deviceId = iteratorSpines.next().value().id();
                // log.debug("DeviceId: "+deviceId.toString());

                // Search for ports between spine and L1
                List<PortNumber> ports = getPortsFromSpineToDevice(deviceId, clientEndpoint.getNode());

                // Create filter to clean c-tag from client to service
                for (PortNumber port: ports)
                {
                    selector = DefaultTrafficSelector.builder()
                        .matchVlanId(bypassVlanId)
                        .matchInnerVlanId(clientEndpoint.getInnerVlan())
                        .matchInPort(port)
                        .build();

                    treatment = DefaultTrafficTreatment.builder()
                        .popVlan()
                        .build();

                    nextId = service.getNextId(clientEndpoint.getUUID(), create);
                    appId = service.getBypassApplicationId();
                    error = filter(deviceId, makeFilteringObjective(selector, treatment, flowPriority, appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }

                // Search for ports between spine and L3
                ports = getPortsFromSpineToDevice(deviceId, serviceEndpoint.getNode());

                // Create filter to push service vlan over c-tag coming from service
                for (PortNumber port: ports)
                {

                    selector = DefaultTrafficSelector.builder()
                        // .matchVlanId(VlanId.ANY) // Any VLAN check is not working
                        .matchVlanId(clientEndpoint.getInnerVlan())
                        .matchInPort(port)
                        .build();

                    treatment = DefaultTrafficTreatment.builder()
                        .pushVlan()
                        .setVlanId(bypassVlanId).build();

                    nextId = service.getNextId(clientEndpoint.getUUID(), create);
                    appId = service.getBypassApplicationId();
                    error = filter(deviceId, makeFilteringObjective(selector, treatment, PacketPriority.CONTROL.priorityValue(), appId), create);
                    processInstalledRuleObjectiveError(deviceId, error);
                }
            }
        }
        log.debug("bypass-temporary-spine-flows-end");
    }

    public void installBypassTemporaryL2L3Flows(ClientServiceBypassEndpoint clientEndpoint, ServiceEndpoint serviceEndpoint, IpPrefix observedClientIp, boolean create) {

        // Install flow to change service vlan into c-tag. Nothing to do in other direction

        ClosDeviceService service = get(ClosDeviceService.class);
        MacAddress ctpdFakeInternalMacAddress = MacAddress.valueOf(service.getCtpdFakeInternalMacAddress());
        VlanId bypassVlanId = VlanId.vlanId((short)service.getBypassVlanId());
        int nextId;
        ApplicationId appId;
        TrafficSelector selector;
        TrafficTreatment treatment;

        log.debug("bypass-temporary-l2l3-flows-start");

        /* IP INT Service to client */

        if(clientEndpoint.getIpPrefix().isIp6())
        {
            selector = DefaultTrafficSelector.builder()
                .matchEthType((short)0x86dd)
                .matchIPv6Dst(observedClientIp)
                .matchIPv6Src(serviceEndpoint.getIpPrefix())
                .matchVlanId(serviceEndpoint.getVlan())
                .build();
        }
        else{
            selector = DefaultTrafficSelector.builder()
                .matchEthType((short)0x0800)
                .matchIPDst(observedClientIp)
                .matchIPSrc(serviceEndpoint.getIpPrefix())
                .matchVlanId(serviceEndpoint.getVlan())
                .build();
        }

        /*	Next Objective */

        List<PortNumber> ports = getPortsToSpineFromDevice(serviceEndpoint.getNode(), clientEndpoint);

        treatmentList = new ArrayList<>();

        if (service.getUseEcmp()){

            // for(PortNumber port  : ports){
                PortNumber port = ports.get(0);
                if(clientEndpoint.getInnerVlan().equals(VlanId.NONE)) {
                    treatment = DefaultTrafficTreatment.builder()
                        .setEthSrc(ctpdFakeInternalMacAddress)
                        .setEthDst(service.getLeafDstMac(clientEndpoint.getNode()))
                        .setVlanId(bypassVlanId)
                        .setOutput(port)
                        .build();
                }
                else {
                    treatment = DefaultTrafficTreatment.builder()
                        .setEthSrc(ctpdFakeInternalMacAddress)
                        .setEthDst(service.getLeafDstMac(clientEndpoint.getNode()))
                        .setVlanId(clientEndpoint.getInnerVlan())
                        .setOutput(port)
                        .build();
                }
                treatmentList.add(treatment);
            // }

        }else{

            PortNumber port = ports.get(0);

            if(clientEndpoint.getInnerVlan().equals(VlanId.NONE)) {
                treatment = DefaultTrafficTreatment.builder()
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setEthDst(service.getHostMac(clientEndpoint.getNode(), clientEndpoint.getPort()))
                    .setVlanId(bypassVlanId)
                    .setOutput(port)
                    .build();
            }
            else {
                treatment = DefaultTrafficTreatment.builder()
                    .setEthSrc(ctpdFakeInternalMacAddress)
                    .setEthDst(service.getHostMac(clientEndpoint.getNode(), clientEndpoint.getPort()))
                    .setVlanId(clientEndpoint.getInnerVlan())
                    .setOutput(port)
                    .build();
            }
            treatmentList.add(treatment);

        }

        nextId = service.getNextId(clientEndpoint.getUUID(), create);
        appId = service.getBypassApplicationId();
        if (create){
            error = next(serviceEndpoint.getNode(), makeTmpNextObjective(treatmentList, nextId, null, service.getBypassFlowPriority(), appId), create);
            processInstalledRuleObjectiveError(clientEndpoint.getNode(), error);
        }
        error = forward(serviceEndpoint.getNode(), makeTmpForwardingObjective(selector, nextId, service.getBypassFlowPriority(), appId), create);
        processInstalledRuleObjectiveError(clientEndpoint.getNode(), error);

        log.debug("bypass-temporary-l2l3-flows-end");
    }

    @Override
    public void installBypassTemporaryFlows(ClientServiceBypassEndpoint clientEndpoint, ServiceEndpoint serviceEndpoint, MacAddress macAddress, IpPrefix ipAddress, boolean create){

        log.debug("install-temporary-bypass-flows-start");

        installBypassTemporaryL1Flows(clientEndpoint, serviceEndpoint, macAddress, ipAddress,create);
        installBypassTemporarySpineFlows(clientEndpoint, serviceEndpoint, create);
        installBypassTemporaryL2L3Flows(clientEndpoint, serviceEndpoint, ipAddress, create);

        log.debug("install-temporary-bypass-flows-end");

    }

    private Endpoint getEndpointFromUUID(UUID serviceUUID){

        ClosDeviceService service = get(ClosDeviceService.class);

        if(serviceUUID != null)
        {
            Versioned<Endpoint> versionedEndpoint = service.getRegisterEndpoints().get(serviceUUID);
            if (versionedEndpoint != null)
            {
                return versionedEndpoint.value();
            }
        }
        return null;
    }
}