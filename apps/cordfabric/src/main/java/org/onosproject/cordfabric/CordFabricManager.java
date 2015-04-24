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
import com.google.common.collect.Multimaps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
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

    private static final int PRIORITY = 1000;

    private final Multimap<VlanId, ConnectPoint> vlans = HashMultimap.create();

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.cordfabric");

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void addVlan(VlanId vlanId, List<ConnectPoint> ports) {
        checkNotNull(vlanId);
        checkNotNull(ports);
        checkArgument(ports.size() > 1);
        verifyPorts(ports);

        removeVlan(vlanId);

        ports.forEach(cp -> {
            if (vlans.put(vlanId, cp)) {
                addForwarding(vlanId, cp.deviceId(), cp.port(),
                              ports.stream()
                                      .filter(p -> p != cp)
                                      .map(ConnectPoint::port)
                                      .collect(Collectors.toList()));
            }
        });
    }

    @Override
    public void removeVlan(VlanId vlanId) {
        vlans.removeAll(vlanId)
                .forEach(cp -> removeForwarding(vlanId, cp.deviceId(), cp.port()));
    }

    @Override
    public Multimap<VlanId, ConnectPoint> getVlans() {
        return Multimaps.unmodifiableMultimap(vlans);
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

    private void removeForwarding(VlanId vlanId, DeviceId deviceId, PortNumber inPort) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(vlanId)
                .matchInPort(inPort)
                .build();

        ForwardingObjective objective = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(PRIORITY)
                .withSelector(selector)
                .withTreatment(DefaultTrafficTreatment.builder().build())
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
}
