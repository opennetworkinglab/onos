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
package org.onosproject.net.optical.intent.impl.compiler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.OduSignalUtils;
import org.onosproject.net.OtuSignalType;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.OpticalOduIntent;
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.net.optical.OtuPort;
import org.onosproject.net.optical.impl.DefaultOduCltPort;
import org.onosproject.net.optical.impl.DefaultOtuPort;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.MockResourceService;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyServiceAdapter;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.driver.DriverServiceAdapter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.AnnotationKeys.STATIC_PORT;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.net.Device.Type.OTN;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.OPTICAL;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.PID;

public class OpticalOduIntentCompilerTest {

    private static final String DEV1 = "of:1";
    private static final String DEV2 = "of:2";
    private static final String DEV3 = "of:3";

    static final Key KEY1 = Key.of(5L, APP_ID);

    private static final String STATIC_TRUE = "true";
    private static final String PNAME = "p2";

    private CoreService coreService;
    private IntentExtensionService intentExtensionService;
    private final IdGenerator idGenerator = new MockIdGenerator();
    private OpticalOduIntentCompiler sut;

    private final ApplicationId appId = new TestApplicationId("test");
    private static Device device1 = new DefaultDevice(ProviderId.NONE, deviceId(DEV1), OTN,
            "m", "h", "s", "n", new ChassisId(0L));
    private static Device device2 = new DefaultDevice(ProviderId.NONE, deviceId(DEV2), OTN,
            "m", "h", "s", "n", new ChassisId(1L));
    private static Device device3 = new DefaultDevice(ProviderId.NONE, deviceId(DEV3), OTN,
            "m", "h", "s", "n", new ChassisId(2L));

    private static Annotations annotations1 = DefaultAnnotations.builder().set(STATIC_PORT, STATIC_TRUE).build();
    private static Annotations annotations2 = DefaultAnnotations.builder().set(PORT_NAME, PNAME).build();

    // OduClt ports with signalType=1GBE
    private static final OduCltPort D1P1 =
            new DefaultOduCltPort(new DefaultPort(device1, PortNumber.portNumber(1), true, annotations1),
                                  CltSignalType.CLT_1GBE);
    private static final OduCltPort D3P2 =
            new DefaultOduCltPort(new DefaultPort(device3, PortNumber.portNumber(2), true, annotations1),
                                  CltSignalType.CLT_1GBE);

    // Otu ports with signalType=ODU2
    private static final OtuPort D1P2 =
            new DefaultOtuPort(new DefaultPort(device1, PortNumber.portNumber(2), true, annotations2),
                               OtuSignalType.OTU2);
    private static final OtuPort D2P1 =
            new DefaultOtuPort(new DefaultPort(device2, PortNumber.portNumber(1), true, annotations2),
                               OtuSignalType.OTU2);
    private static final OtuPort D2P2 =
            new DefaultOtuPort(new DefaultPort(device2, PortNumber.portNumber(2), true, annotations2),
                               OtuSignalType.OTU2);
    private static final OtuPort D3P1 =
            new DefaultOtuPort(new DefaultPort(device3, PortNumber.portNumber(1), true, annotations2),
                               OtuSignalType.OTU2);

    // OduClt ports with signalType=10GBE
    private static final OduCltPort D1P3 =
            new DefaultOduCltPort(new DefaultPort(device1, PortNumber.portNumber(3), true, annotations1),
                                  CltSignalType.CLT_10GBE);
    private static final OduCltPort D3P3 =
            new DefaultOduCltPort(new DefaultPort(device3, PortNumber.portNumber(3), true, annotations1),
                                  CltSignalType.CLT_10GBE);

    // OduCltPort ConnectPoints
    private final ConnectPoint d1p1 = new ConnectPoint(device1.id(), D1P1.number());
    private final ConnectPoint d1p3 = new ConnectPoint(device1.id(), D1P3.number());
    private final ConnectPoint d3p2 = new ConnectPoint(device3.id(), D3P2.number());
    private final ConnectPoint d3p3 = new ConnectPoint(device3.id(), D3P3.number());

    // OtuPort ConnectPoints
    private final ConnectPoint d1p2 = new ConnectPoint(device1.id(), D1P2.number());
    private final ConnectPoint d2p1 = new ConnectPoint(device2.id(), D2P1.number());
    private final ConnectPoint d2p2 = new ConnectPoint(device2.id(), D2P2.number());
    private final ConnectPoint d3p1 = new ConnectPoint(device3.id(), D3P1.number());

    private final List<Link> links = Arrays.asList(
            DefaultLink.builder().providerId(PID).src(d1p2).dst(d2p1).type(OPTICAL).build(),
            DefaultLink.builder().providerId(PID).src(d2p2).dst(d3p1).type(OPTICAL).build()
    );
    private final Path path = new DefaultPath(PID, links, 3);

    private OpticalOduIntent intent;

    /**
     * Mocks the topology service to give paths in the test.
     */
    private class MockTopologyService extends TopologyServiceAdapter {
        Set<Path> paths = Sets.newHashSet(path);

        @Override
        public Topology currentTopology() {
            return null;
        }

        @Override
        public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst, LinkWeight weight) {
            return paths;
        }
    }

    /**
     * Mocks the device service so that devices and ports appear available in the test.
     */
    private static class MockDeviceService extends DeviceServiceAdapter {
        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return true;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            if (deviceId.equals(deviceId(DEV1))) {
                return ImmutableList.of((Port) D1P1, (Port) D1P2, (Port) D1P3);
            }

            if (deviceId.equals(deviceId(DEV2))) {
                return ImmutableList.of((Port) D2P1, (Port) D2P2);
            }

            if (deviceId.equals(deviceId(DEV3))) {
                return ImmutableList.of((Port) D3P1, (Port) D3P2, (Port) D3P3);
            }

            return Collections.emptyList();
        }

        @Override
        public Port getPort(DeviceId deviceId, PortNumber portNumber) {
            if (deviceId.equals(deviceId(DEV1))) {
                switch (portNumber.toString()) {
                    case "1":
                        return D1P1;
                    case "2":
                        return D1P2;
                    case "3":
                        return D1P3;
                    default:
                        return null;
                }
            }
            if (deviceId.equals(deviceId(DEV2))) {
                switch (portNumber.toString()) {
                    case "1":
                        return D2P1;
                    case "2":
                        return D2P2;
                    default:
                        return null;
                }
            }
            if (deviceId.equals(deviceId(DEV3))) {
                switch (portNumber.toString()) {
                    case "1":
                        return D3P1;
                    case "2":
                        return D3P2;
                    case "3":
                        return D3P3;
                    default:
                        return null;
                }
            }
            return null;
        }
    }

    private static class MockDriverService extends DriverServiceAdapter
            implements DriverService {
        // TODO override to return appropriate driver,
        // with DefaultOpticalDevice support, etc.
    }

    @Before
    public void setUp() {
        AbstractProjectableModel.setDriverService(null, new MockDriverService());
        sut =  new OpticalOduIntentCompiler();
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication("org.onosproject.net.intent"))
                .andReturn(appId);
        sut.coreService = coreService;
        sut.deviceService = new MockDeviceService();
        sut.resourceService = new MockResourceService();
        sut.topologyService = new MockTopologyService();

        Intent.bindIdGenerator(idGenerator);

        intentExtensionService = createMock(IntentExtensionService.class);
        intentExtensionService.registerCompiler(OpticalOduIntent.class, sut);
        intentExtensionService.unregisterCompiler(OpticalOduIntent.class);
        sut.intentManager = intentExtensionService;

        replay(coreService, intentExtensionService);
    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Tests compile of OpticalOduIntent with allocation of TributarySlots.
     * Compile two ODUCLT ports (with CLT_1GBE), over OTU ports (with OTU2):
     *   - only one TributarySlot is used
     */
    @Test
    public void test1GbeMultiplexOverOdu2() {

        intent = OpticalOduIntent.builder()
                .appId(APP_ID)
                .key(KEY1)
                .src(d1p1)
                .dst(d3p2)
                .signalType(D1P1.signalType())
                .bidirectional(false)
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();

        // 1st Device
        FlowRule rule1 = rules.stream()
                .filter(x -> x.deviceId().equals(device1.id()))
                .findFirst()
                .get();
        // validate SRC selector
        TrafficSelector.Builder selectorBuilder1 = DefaultTrafficSelector.builder();
        selectorBuilder1.matchInPort(d1p1.port());
        selectorBuilder1.add(Criteria.matchOduSignalType(OduSignalType.ODU0));
        assertThat(rule1.selector(), is(selectorBuilder1.build()));

        // validate SRC treatment  (with OduSignalId, where 1 TributarySlot is used)
        TrafficTreatment.Builder treatmentBuilder1 = DefaultTrafficTreatment.builder();
        Set<TributarySlot> slots = new HashSet<>();
        slots.add(TributarySlot.of(1));
        OduSignalId oduSignalId = OduSignalUtils.buildOduSignalId(OduSignalType.ODU2, slots);
        treatmentBuilder1.add(Instructions.modL1OduSignalId(oduSignalId));
        treatmentBuilder1.setOutput(d1p2.port());
        assertThat(rule1.treatment(), is(treatmentBuilder1.build()));

        // 2nd Device
        FlowRule rule2 = rules.stream()
                .filter(x -> x.deviceId().equals(device2.id()))
                .findFirst()
                .get();
        // validate SRC selector
        TrafficSelector.Builder selectorBuilder2 = DefaultTrafficSelector.builder();
        selectorBuilder2.matchInPort(d2p1.port());
        selectorBuilder2.add(Criteria.matchOduSignalType(OduSignalType.ODU0));
        selectorBuilder2.add(Criteria.matchOduSignalId(oduSignalId));
        assertThat(rule2.selector(), is(selectorBuilder2.build()));

        // validate SRC treatment  (with OduSignalId, where 1 TributarySlot is used)
        TrafficTreatment.Builder treatmentBuilder2 = DefaultTrafficTreatment.builder();
        treatmentBuilder2.add(Instructions.modL1OduSignalId(oduSignalId));
        treatmentBuilder2.setOutput(d2p2.port());
        assertThat(rule2.treatment(), is(treatmentBuilder2.build()));


        // 3rd Device
        FlowRule rule3 = rules.stream()
                .filter(x -> x.deviceId().equals(device3.id()))
                .findFirst()
                .get();
        // validate DST selector (with OduSignalId, where the same TributarySlot is used)
        TrafficSelector.Builder selectorBuilder3 = DefaultTrafficSelector.builder();
        selectorBuilder3.matchInPort(d3p1.port());
        selectorBuilder3.add(Criteria.matchOduSignalType(OduSignalType.ODU0));
        selectorBuilder3.add(Criteria.matchOduSignalId(oduSignalId));
        assertThat(rule3.selector(), is(selectorBuilder3.build()));

        // validate DST treatment
        assertThat(rule3.treatment(), is(
                DefaultTrafficTreatment.builder().setOutput(d3p2.port()).build()
                ));

        rules.forEach(rule -> assertEquals("FlowRule priority is incorrect",
                intent.priority(), rule.priority()));

        sut.deactivate();
    }

    /**
     * Tests compile of OpticalOduIntent with allocation of TributarySlots.
     * Compile two ODUCLT ports (with CLT_10GBE), over OTU ports (with OTU2):
     *   - All TributarySlots are used
     */
    @Test
    public void test10GbeMultiplexOverOdu2() {

        intent = OpticalOduIntent.builder()
                .appId(APP_ID)
                .key(KEY1)
                .src(d1p3)
                .dst(d3p3)
                .signalType(D1P3.signalType())
                .bidirectional(false)
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();

        // 1st Device
        FlowRule rule1 = rules.stream()
                .filter(x -> x.deviceId().equals(device1.id()))
                .findFirst()
                .get();
        // validate SRC selector
        TrafficSelector.Builder selectorBuilder1 = DefaultTrafficSelector.builder();
        selectorBuilder1.matchInPort(d1p3.port());
        selectorBuilder1.add(Criteria.matchOduSignalType(OduSignalType.ODU2));
        assertThat(rule1.selector(), is(selectorBuilder1.build()));

        // validate SRC treatment  (without OduSignalId - all TributarySlots are used)
        TrafficTreatment.Builder treatmentBuilder1 = DefaultTrafficTreatment.builder();
        treatmentBuilder1.setOutput(d1p2.port());
        assertThat(rule1.treatment(), is(treatmentBuilder1.build()));

        // 2nd Device
        FlowRule rule2 = rules.stream()
                .filter(x -> x.deviceId().equals(device2.id()))
                .findFirst()
                .get();
        // validate SRC selector
        TrafficSelector.Builder selectorBuilder2 = DefaultTrafficSelector.builder();
        selectorBuilder2.matchInPort(d2p1.port());
        selectorBuilder2.add(Criteria.matchOduSignalType(OduSignalType.ODU2));
        assertThat(rule2.selector(), is(selectorBuilder2.build()));

        // validate SRC treatment  (without OduSignalId - all TributarySlots are used)
        TrafficTreatment.Builder treatmentBuilder2 = DefaultTrafficTreatment.builder();
        treatmentBuilder2.setOutput(d2p2.port());
        assertThat(rule2.treatment(), is(treatmentBuilder2.build()));


        // 3rd Device
        FlowRule rule3 = rules.stream()
                .filter(x -> x.deviceId().equals(device3.id()))
                .findFirst()
                .get();
        // validate DST selector (without OduSignalId - all TributarySlots are used)
        TrafficSelector.Builder selectorBuilder3 = DefaultTrafficSelector.builder();
        selectorBuilder3.matchInPort(d3p1.port());
        selectorBuilder3.add(Criteria.matchOduSignalType(OduSignalType.ODU2));
        assertThat(rule3.selector(), is(selectorBuilder3.build()));

        // validate DST treatment
        assertThat(rule3.treatment(), is(
                DefaultTrafficTreatment.builder().setOutput(d3p3.port()).build()
                ));

        rules.forEach(rule -> assertEquals("FlowRule priority is incorrect",
                intent.priority(), rule.priority()));

        sut.deactivate();
    }

}