/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.TestApplicationId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.Annotations;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalId;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.OduSignalUtils;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.behaviour.TributarySlotQuery;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.driver.TestBehaviourImpl;
import org.onosproject.net.driver.TestBehaviourTwoImpl;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.intent.IntentSetMultimap;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.net.optical.impl.DefaultOchPort;
import org.onosproject.net.optical.impl.DefaultOduCltPort;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.MockResourceService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.net.AnnotationKeys.STATIC_PORT;
import static org.onosproject.net.Device.Type.ROADM;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.NetTestTools.APP_ID;

public class OpticalCircuitIntentCompilerTest extends AbstractIntentTest {

    private static final String DEV1 = "of:1";
    private static final String DEV2 = "of:2";

    static final Key KEY1 = Key.of(5L, APP_ID);

    private static final String STATIC_TRUE = "true";
    private static final String PNAME = "p2";

    private CoreService coreService;
    private IntentExtensionService intentExtensionService;
    private OpticalCircuitIntentCompiler sut;

    private final ApplicationId appId = new TestApplicationId("test");
    private static Device device1 = new DefaultDevice(ProviderId.NONE, deviceId(DEV1), ROADM,
            "m", "h", "s", "n", new ChassisId(0L));
    private static Device device2 = new DefaultDevice(ProviderId.NONE, deviceId(DEV2), ROADM,
            "m", "h", "s", "n", new ChassisId(1L));

    private static Annotations annotations1 = DefaultAnnotations.builder().set(STATIC_PORT, STATIC_TRUE).build();
    private static Annotations annotations2 = DefaultAnnotations.builder().set(PORT_NAME, PNAME).build();

    // OduClt ports with signalType=1GBE
    private static final OduCltPort D1P1 =
            new DefaultOduCltPort(new DefaultPort(device1, PortNumber.portNumber(1), true, annotations1),
                                  CltSignalType.CLT_1GBE);
    private static final OduCltPort D2P1 =
            new DefaultOduCltPort(new DefaultPort(device2, PortNumber.portNumber(1), true, annotations1),
                                  CltSignalType.CLT_1GBE);

    // Och ports with signalType=ODU2
    private static final OchPort D1P2 =
            new DefaultOchPort(new DefaultPort(device1, PortNumber.portNumber(2), true, annotations2),
                               OduSignalType.ODU2,
                    true, OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1));
    private static final OchPort D2P2 =
            new DefaultOchPort(new DefaultPort(device2, PortNumber.portNumber(2), true, annotations2),
                               OduSignalType.ODU2,
                    true, OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1));

    // OduClt ports with signalType=10GBE
    private static final OduCltPort D1P3 =
            new DefaultOduCltPort(new DefaultPort(device1, PortNumber.portNumber(3), true, annotations1),
                                  CltSignalType.CLT_10GBE);
    private static final OduCltPort D2P3 =
            new DefaultOduCltPort(new DefaultPort(device2, PortNumber.portNumber(3), true, annotations1),
                                  CltSignalType.CLT_10GBE);


    private OpticalCircuitIntent intent;

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
                return ImmutableList.of((Port) D2P1, (Port) D2P2, (Port) D2P3);
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
                    case "3":
                        return D2P3;
                    default:
                        return null;
                }
            }
            return null;
        }
    }

    /**
     * Mocks the driver service so it will appear supporting TributarySlotQuery Behaviour in the test.
     */
    private static class MockDriverServiceWithTs extends DriverServiceAdapter {
        @Override
        public Driver getDriver(DeviceId deviceId) {
            DefaultDriver ddp = new DefaultDriver("foo.base", new ArrayList<>(), "Circus", "lux", "1.2a",
                    ImmutableMap.of(Behaviour.class,
                                    TestBehaviourImpl.class,
                                    TributarySlotQuery.class,
                                    TestBehaviourTwoImpl.class),
                    ImmutableMap.of("foo", "bar"));
            return ddp;
        }
    }

    /**
     * Mocks the driver service so it will appear not-supporting TributarySlotQuery Behaviour in the test.
     */
    private static class MockDriverServiceNoTs extends DriverServiceAdapter {
        @Override
        public Driver getDriver(DeviceId deviceId) {
            DefaultDriver ddp = new DefaultDriver("foo.base", new ArrayList<>(), "Circus", "lux", "1.2a",
                    ImmutableMap.of(Behaviour.class,
                                    TestBehaviourImpl.class),
                    ImmutableMap.of("foo", "bar"));
            return ddp;
        }
    }

    private static class MockIntentSetMultimap implements IntentSetMultimap {
        @Override
        public boolean allocateMapping(IntentId keyIntentId,
                IntentId valIntentId) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public Set<IntentId> getMapping(IntentId intentId) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void releaseMapping(IntentId intentId) {
            // TODO Auto-generated method stub
        }

    }

    /**
     * Represents a fake IntentService class that easily allows to store and
     * retrieve intents without implementing the IntentService logic.
     */
    private class TestIntentService extends IntentServiceAdapter {

        private Set<Intent> intents;

        public TestIntentService() {
            intents = Sets.newHashSet();
        }

        @Override
        public void submit(Intent intent) {
            intents.add(intent);
        }

        @Override
        public long getIntentCount() {
            return intents.size();
        }

        @Override
        public Iterable<Intent> getIntents() {
            return intents;
        }

        @Override
        public Intent getIntent(Key intentKey) {
            for (Intent intent : intents) {
                if (intent.key().equals(intentKey)) {
                    return intent;
                }
            }
            return null;
        }
    }

    @BeforeClass
    public static void setUpClass() {
        AbstractProjectableModel.setDriverService("key", new DriverServiceAdapter());
    }

    @Before
    public void setUp() {
        sut = new OpticalCircuitIntentCompiler();
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication("org.onosproject.net.intent"))
                .andReturn(appId);
        sut.coreService = coreService;
        sut.deviceService = new MockDeviceService();
        sut.resourceService = new MockResourceService();
        sut.intentService = new TestIntentService();
        sut.intentSetMultimap = new MockIntentSetMultimap();

        super.setUp();

        intentExtensionService = createMock(IntentExtensionService.class);
        intentExtensionService.registerCompiler(OpticalCircuitIntent.class, sut);
        intentExtensionService.unregisterCompiler(OpticalCircuitIntent.class);
        sut.intentManager = intentExtensionService;
        replay(coreService, intentExtensionService);

        // mocking ComponentConfigService
        ComponentConfigService mockConfigService =
                EasyMock.createMock(ComponentConfigService.class);
        expect(mockConfigService.getProperties(anyObject())).andReturn(ImmutableSet.of());
        mockConfigService.registerProperties(sut.getClass());
        expectLastCall();
        mockConfigService.unregisterProperties(sut.getClass(), false);
        expectLastCall();
        expect(mockConfigService.getProperties(anyObject())).andReturn(ImmutableSet.of());
        sut.cfgService = mockConfigService;
        replay(mockConfigService);

    }

    /**
     * Tests compile of OpticalCircuitIntent with allocation of TributarySlots.
     * Compile two ODUCLT ports (with CLT_1GBE), over OCH ports (with ODU2):
     *   - only one TributarySlot is used
     */
    @Test
    public void test1GbeMultiplexOverOdu2() {

        // Use driver with TributarySlotQuery Behaviour
        sut.driverService = new MockDriverServiceWithTs();

        ConnectPoint oduCltSrcCP = new ConnectPoint(device1.id(), D1P1.number());
        ConnectPoint oduCltDstCP = new ConnectPoint(device2.id(), D2P1.number());
        ConnectPoint ochSrcCP = new ConnectPoint(device1.id(), D1P2.number());
        ConnectPoint ochDstCP = new ConnectPoint(device2.id(), D2P2.number());

        intent = OpticalCircuitIntent.builder()
                .appId(APP_ID)
                .key(KEY1)
                .src(oduCltSrcCP)
                .dst(oduCltDstCP)
                .signalType(D1P1.signalType())
                .bidirectional(false)
                .build();

        sut.activate(null);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        assertThat("key is inherited",
                   compiled.stream().map(Intent::key).collect(Collectors.toList()),
                   everyItem(is(intent.key())));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();

        FlowRule rule1 = rules.stream()
                .filter(x -> x.deviceId().equals(device1.id()))
                .findFirst()
                .get();
        // validate SRC selector
        TrafficSelector.Builder selectorBuilder1 = DefaultTrafficSelector.builder();
        selectorBuilder1.matchInPort(oduCltSrcCP.port());
        selectorBuilder1.add(Criteria.matchOduSignalType(OduSignalType.ODU0));
        assertThat(rule1.selector(), is(selectorBuilder1.build()));

        // validate SRC treatment  (with OduSignalId, where 1 TributarySlot is used)
        TrafficTreatment.Builder treatmentBuilder1 = DefaultTrafficTreatment.builder();
        Set<TributarySlot> slots = new HashSet<>();
        slots.add(TributarySlot.of(1));
        OduSignalId oduSignalId = OduSignalUtils.buildOduSignalId(D1P2.signalType(), slots);
        treatmentBuilder1.add(Instructions.modL1OduSignalId(oduSignalId));
        treatmentBuilder1.setOutput(ochSrcCP.port());
        assertThat(rule1.treatment(), is(treatmentBuilder1.build()));

        FlowRule rule2 = rules.stream()
                .filter(x -> x.deviceId().equals(device2.id()))
                .findFirst()
                .get();
        // validate DST selector (with OduSignalId, where the same TributarySlot is used)
        TrafficSelector.Builder selectorBuilder2 = DefaultTrafficSelector.builder();
        selectorBuilder2.matchInPort(ochDstCP.port());
        selectorBuilder2.add(Criteria.matchOduSignalType(OduSignalType.ODU0));
        selectorBuilder2.add(Criteria.matchOduSignalId(oduSignalId));
        assertThat(rule2.selector(), is(selectorBuilder2.build()));

        // validate DST treatment
        assertThat(rule2.treatment(), is(
                DefaultTrafficTreatment.builder().setOutput(oduCltDstCP.port()).build()
                ));

        rules.forEach(rule -> assertEquals("FlowRule priority is incorrect",
                intent.priority(), rule.priority()));

        sut.deactivate();
    }

    /**
     * Tests compile of OpticalCircuitIntent with allocation of TributarySlots.
     * Compile two ODUCLT ports (with CLT_10GBE), over OCH ports (with ODU2):
     *   - All TributarySlots are used
     */
    @Test
    public void test10GbeMultiplexOverOdu2() {

        // Use driver with TributarySlotQuery Behaviour
        sut.driverService = new MockDriverServiceWithTs();

        ConnectPoint oduCltSrcCP = new ConnectPoint(device1.id(), D1P3.number());
        ConnectPoint oduCltDstCP = new ConnectPoint(device2.id(), D2P3.number());
        ConnectPoint ochSrcCP = new ConnectPoint(device1.id(), D1P2.number());
        ConnectPoint ochDstCP = new ConnectPoint(device2.id(), D2P2.number());

        intent = OpticalCircuitIntent.builder()
                .appId(APP_ID)
                .key(KEY1)
                .src(oduCltSrcCP)
                .dst(oduCltDstCP)
                .signalType(D1P3.signalType())
                .bidirectional(false)
                .build();

        sut.activate(null);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        assertThat("key is inherited",
                   compiled.stream().map(Intent::key).collect(Collectors.toList()),
                   everyItem(is(intent.key())));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();

        FlowRule rule1 = rules.stream()
                .filter(x -> x.deviceId().equals(device1.id()))
                .findFirst()
                .get();
        // validate SRC selector
        TrafficSelector.Builder selectorBuilder1 = DefaultTrafficSelector.builder();
        selectorBuilder1.matchInPort(oduCltSrcCP.port());
        selectorBuilder1.add(Criteria.matchOduSignalType(OduSignalType.ODU2));
        assertThat(rule1.selector(), is(selectorBuilder1.build()));

        // validate SRC treatment (without OduSignalId, i.e. All TributarySlots are used)
        TrafficTreatment.Builder treatmentBuilder1 = DefaultTrafficTreatment.builder();
        treatmentBuilder1.setOutput(ochSrcCP.port());
        assertThat(rule1.treatment(), is(treatmentBuilder1.build()));

        FlowRule rule2 = rules.stream()
                .filter(x -> x.deviceId().equals(device2.id()))
                .findFirst()
                .get();
        // validate DST selector (without OduSignalId, i.e. All TributarySlots are used)
        TrafficSelector.Builder selectorBuilder2 = DefaultTrafficSelector.builder();
        selectorBuilder2.matchInPort(ochDstCP.port());
        selectorBuilder2.add(Criteria.matchOduSignalType(OduSignalType.ODU2));
        assertThat(rule2.selector(), is(selectorBuilder2.build()));

        // validate DST treatment
        assertThat(rule2.treatment(), is(
                DefaultTrafficTreatment.builder().setOutput(oduCltDstCP.port()).build()
                ));

        rules.forEach(rule -> assertEquals("FlowRule priority is incorrect",
                intent.priority(), rule.priority()));

        sut.deactivate();
    }

    /**
     * Tests compile of OpticalCircuitIntent without allocation of TributarySlots.
     * Compile two ODUCLT ports (with CLT_10GBE), over OCH ports (with ODU2):
     *   - No TributarySlots are used
     */
    @Test
    public void test10GbeNoMuxOverOdu2() {

        // Use driver without support for TributarySlotQuery Behaviour
        sut.driverService = new MockDriverServiceNoTs();

        ConnectPoint oduCltSrcCP = new ConnectPoint(device1.id(), D1P3.number());
        ConnectPoint oduCltDstCP = new ConnectPoint(device2.id(), D2P3.number());
        ConnectPoint ochSrcCP = new ConnectPoint(device1.id(), D1P2.number());
        ConnectPoint ochDstCP = new ConnectPoint(device2.id(), D2P2.number());

        intent = OpticalCircuitIntent.builder()
                .appId(APP_ID)
                .key(KEY1)
                .src(oduCltSrcCP)
                .dst(oduCltDstCP)
                .signalType(D1P3.signalType())
                .bidirectional(false)
                .build();

        sut.activate(null);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        assertThat("key is inherited",
                   compiled.stream().map(Intent::key).collect(Collectors.toList()),
                   everyItem(is(intent.key())));


        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();

        FlowRule rule1 = rules.stream()
                .filter(x -> x.deviceId().equals(device1.id()))
                .findFirst()
                .get();
        // validate SRC selector
        TrafficSelector.Builder selectorBuilder1 = DefaultTrafficSelector.builder();
        selectorBuilder1.matchInPort(oduCltSrcCP.port());
        assertThat(rule1.selector(), is(selectorBuilder1.build()));

        // validate SRC treatment (without OduSignalType and OduSignalId: i.e. No TributarySlots are used)
        TrafficTreatment.Builder treatmentBuilder1 = DefaultTrafficTreatment.builder();
        treatmentBuilder1.setOutput(ochSrcCP.port());
        assertThat(rule1.treatment(), is(treatmentBuilder1.build()));

        FlowRule rule2 = rules.stream()
                .filter(x -> x.deviceId().equals(device2.id()))
                .findFirst()
                .get();
        // validate DST selector (without OduSignalType and OduSignalId: i.e. No TributarySlots are used)
        TrafficSelector.Builder selectorBuilder2 = DefaultTrafficSelector.builder();
        selectorBuilder2.matchInPort(ochDstCP.port());
        assertThat(rule2.selector(), is(selectorBuilder2.build()));
        // validate DST treatment
        assertThat(rule2.treatment(), is(
                DefaultTrafficTreatment.builder().setOutput(oduCltDstCP.port()).build()
                ));

        rules.forEach(rule -> assertEquals("FlowRule priority is incorrect",
                intent.priority(), rule.priority()));

        sut.deactivate();
    }

}
