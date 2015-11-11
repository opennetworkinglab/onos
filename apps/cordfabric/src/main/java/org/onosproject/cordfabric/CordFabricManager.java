/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.cordfabric;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * CORD fabric application.
 */
@Service
@Component(immediate = true)
public class CordFabricManager implements FabricService {

    private final Logger log = getLogger(getClass());

    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private InternalDeviceListener deviceListener = new InternalDeviceListener();

    private static final int PRIORITY = 50000;
    private static final int TESTPRIO = 49999;

    private short radiusPort = 1812;

    private short ofPort = 6653;

    private DeviceId fabricDeviceId = DeviceId.deviceId("of:5e3e486e73000187");

    private final Multimap<VlanId, ConnectPoint> vlans = HashMultimap.create();

    //TODO make this configurable
    private boolean testMode = true;


    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.cordfabric");

        deviceService.addListener(deviceListener);

        if (deviceService.isAvailable(fabricDeviceId)) {
            setupDefaultFlows();
        }

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);

        log.info("Stopped");
    }

    private void setupDefaultFlows() {
        TrafficSelector ofInBandMatchUp = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(ofPort))
                .matchInPort(PortNumber.portNumber(6))
                .build();

        TrafficSelector ofInBandMatchDown = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpSrc(TpPort.tpPort(ofPort))
                .matchInPort(PortNumber.portNumber(1))
                .build();

        TrafficSelector oltMgmtUp = DefaultTrafficSelector.builder()
                .matchEthSrc(MacAddress.valueOf("00:0c:d5:00:01:01"))
                .matchInPort(PortNumber.portNumber(2))
                .build();

        TrafficSelector oltMgmtDown = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.valueOf("00:0c:d5:00:01:01"))
                .matchInPort(PortNumber.portNumber(9))
                .build();

        TrafficTreatment up = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(1))
                .build();

        TrafficTreatment down = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(6))
                .build();

        TrafficSelector toRadius = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(2))
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(radiusPort))
                .build();

        TrafficSelector fromRadius = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(5))
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(radiusPort))
                .build();

        TrafficTreatment toOlt = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2))
                .build();

        TrafficTreatment toVolt = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(9))
                .build();

        TrafficTreatment sentToRadius = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(5))
                .build();

        TrafficTreatment testPort = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(8))
                .build();

        ForwardingObjective ofTestPath = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(TESTPRIO)
                .withSelector(
                        DefaultTrafficSelector.builder()
                                .matchInPort(PortNumber.portNumber(2))
                                .build())
                .withTreatment(testPort)
                .add();

        ForwardingObjective radiusToServer = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(toRadius)
                .withTreatment(sentToRadius)
                .add();

        ForwardingObjective serverToRadius = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(fromRadius)
                .withTreatment(toOlt)
                .add();



        ForwardingObjective upCtrl = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(ofInBandMatchUp)
                .withTreatment(up)
                .add();

        ForwardingObjective downCtrl = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(ofInBandMatchDown)
                .withTreatment(down)
                .add();

        ForwardingObjective upOltMgmt = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(oltMgmtUp)
                .withTreatment(toVolt)
                .add();

        ForwardingObjective downOltMgmt = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(oltMgmtDown)
                .withTreatment(toOlt)
                .add();

        if (testMode) {
            flowObjectiveService.forward(fabricDeviceId, ofTestPath);
        }

        flowObjectiveService.forward(fabricDeviceId, upCtrl);
        flowObjectiveService.forward(fabricDeviceId, downCtrl);
        flowObjectiveService.forward(fabricDeviceId, radiusToServer);
        flowObjectiveService.forward(fabricDeviceId, serverToRadius);
        flowObjectiveService.forward(fabricDeviceId, upOltMgmt);
        flowObjectiveService.forward(fabricDeviceId, downOltMgmt);
    }

    @Override
    public void addVlan(FabricVlan vlan) {
        checkNotNull(vlan);
        checkArgument(vlan.ports().size() > 1);
        verifyPorts(vlan.ports());

        removeVlan(vlan.vlan());

        if (vlan.iptv()) {
            provisionIpTv();
        }

        vlan.ports().forEach(cp -> {
            if (vlans.put(vlan.vlan(), cp)) {
                addForwarding(vlan.vlan(), cp.deviceId(), cp.port(),
                              vlan.ports().stream()
                                      .filter(p -> p != cp)
                                      .map(ConnectPoint::port)
                                      .collect(Collectors.toList()));
            }
        });
    }

    //FIXME: pass iptv vlan in here.
    private void provisionIpTv() {
        TrafficSelector ipTvUp = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.vlanId((short) 7))
                .matchInPort(PortNumber.portNumber(2))
                .build();

        TrafficTreatment ipTvActUp = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(7)).build();

        TrafficSelector ipTvDown = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.vlanId((short) 7))
                .matchInPort(PortNumber.portNumber(7))
                .build();

        TrafficTreatment ipTvActDown = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(2)).build();

        ForwardingObjective ipTvUpstream = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(ipTvUp)
                .withTreatment(ipTvActUp)
                .add();

        ForwardingObjective ipTvDownstream = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(ipTvDown)
                .withTreatment(ipTvActDown)
                .add();

        flowObjectiveService.forward(fabricDeviceId, ipTvUpstream);
        flowObjectiveService.forward(fabricDeviceId, ipTvDownstream);
    }

    @Override
    public void removeVlan(VlanId vlanId) {
        Collection<ConnectPoint> ports = vlans.removeAll(vlanId);

        ports.forEach(cp -> removeForwarding(vlanId, cp.deviceId(), cp.port(),
                                             ports.stream()
                                                     .filter(p -> p != cp)
                                                     .map(ConnectPoint::port)
                                                     .collect(Collectors.toList())));
    }

    @Override
    public List<FabricVlan> getVlans() {
        List<FabricVlan> fVlans = new ArrayList<>();
        vlans.keySet().forEach(vlan -> fVlans.add(
                //FIXME: Very aweful but will fo for now
                new FabricVlan(vlan, vlans.get(vlan), vlan.toShort() == 201)));
        return fVlans;
    }

    private static void verifyPorts(List<ConnectPoint> ports) {
        DeviceId deviceId = ports.get(0).deviceId();
        for (ConnectPoint connectPoint : ports) {
            if (!connectPoint.deviceId().equals(deviceId)) {
                throw new IllegalArgumentException("Ports must all be on the same device");
            }
        }
    }

    private void addForwarding(VlanId vlanId, DeviceId deviceId, PortNumber inPort,
                               List<PortNumber> outPorts) {

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(vlanId)
                .matchInPort(inPort)
                .build();

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        outPorts.forEach(p -> treatmentBuilder.setOutput(p));

        ForwardingObjective objective = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(selector)
                .withTreatment(treatmentBuilder.build())
                .add(new ObjectiveHandler());

        flowObjectiveService.forward(deviceId, objective);
    }

    private void removeForwarding(VlanId vlanId, DeviceId deviceId, PortNumber inPort,
                                  List<PortNumber> outPorts) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(vlanId)
                .matchInPort(inPort)
                .build();

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();

        outPorts.forEach(p -> treatmentBuilder.setOutput(p));

        ForwardingObjective objective = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(selector)
                .withTreatment(treatmentBuilder.build())
                .remove(new ObjectiveHandler());

        flowObjectiveService.forward(deviceId, objective);
    }

    private static class ObjectiveHandler implements ObjectiveContext {
        private static Logger log = LoggerFactory.getLogger(ObjectiveHandler.class);

        @Override
        public void onSuccess(Objective objective) {
            log.info("Flow objective operation successful: {}", objective);
        }

        @Override
        public void onError(Objective objective, ObjectiveError error) {
            log.info("Flow objective operation failed: {}", objective);
        }
    }

    /**
     * Internal listener for device service events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
            case DEVICE_ADDED:
            case DEVICE_AVAILABILITY_CHANGED:
                if (event.subject().id().equals(fabricDeviceId) &&
                        deviceService.isAvailable(event.subject().id())) {
                    setupDefaultFlows();
                }
            default:
                break;
            }
        }
    }
}
