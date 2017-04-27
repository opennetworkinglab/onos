/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.flow.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestTools;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.Device.Type;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.DriverRegistry;
import org.onosproject.net.driver.impl.DriverManager;
import org.onosproject.net.driver.impl.DriverRegistryManager;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.MetadataInstruction;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.trivial.SimpleFlowRuleStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;
import static org.onosproject.net.flow.FlowRuleEvent.Type.*;

/**
 * Test codifying the flow rule service & flow rule provider service contracts.
 */
public class FlowRuleManagerTest {


    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId FOO_PID = new ProviderId("foo", "foo");

    private static final DeviceId DID = DeviceId.deviceId("of:001");
    private static final DeviceId FOO_DID = DeviceId.deviceId("foo:002");
    private static final int TIMEOUT = 10;

    private static final DefaultAnnotations ANNOTATIONS =
            DefaultAnnotations.builder().set(AnnotationKeys.DRIVER, "foo").build();

    private static final Device DEV =
            new DefaultDevice(PID, DID, Type.SWITCH, "", "", "", "", null);
    private static final Device FOO_DEV =
            new DefaultDevice(FOO_PID, FOO_DID, Type.SWITCH, "", "", "", "", null, ANNOTATIONS);

    private FlowRuleManager mgr;

    protected FlowRuleService service;
    protected FlowRuleProviderRegistry registry;
    protected FlowRuleProviderService providerService;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();
    private ApplicationId appId;

    private TestDriverManager driverService;

    @Before
    public void setUp() {
        mgr = new FlowRuleManager();
        mgr.store = new SimpleFlowRuleStore();
        injectEventDispatcher(mgr, new TestEventDispatcher());
        mgr.deviceService = new TestDeviceService();
        mgr.mastershipService = new TestMastershipService();
        mgr.coreService = new TestCoreService();
        mgr.operationsService = MoreExecutors.newDirectExecutorService();
        mgr.deviceInstallers = MoreExecutors.newDirectExecutorService();
        mgr.cfgService = new ComponentConfigAdapter();
        service = mgr;
        registry = mgr;

        DriverRegistryManager driverRegistry = new DriverRegistryManager();
        driverService = new TestDriverManager(driverRegistry);
        driverRegistry.addDriver(new DefaultDriver("foo", ImmutableList.of(), "", "", "",
                                                   ImmutableMap.of(FlowRuleProgrammable.class,
                                                                   TestFlowRuleProgrammable.class),
                                                   ImmutableMap.of()));

        mgr.activate(null);
        mgr.addListener(listener);
        provider = new TestProvider(PID);
        providerService = this.registry.register(provider);
        appId = new TestApplicationId(0, "FlowRuleManagerTest");
        assertTrue("provider should be registered",
                   this.registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                    registry.getProviders().contains(provider.id()));
        service.removeListener(listener);
        mgr.deactivate();
        injectEventDispatcher(mgr, null);
        mgr.deviceService = null;
    }

    private FlowRule flowRule(int tsval, int trval) {
        return flowRule(DID, tsval, trval);
    }

    private FlowRule flowRule(DeviceId did, int tsval, int trval) {
        TestSelector ts = new TestSelector(tsval);
        TestTreatment tr = new TestTreatment(trval);
        return DefaultFlowRule.builder()
                .forDevice(did)
                .withSelector(ts)
                .withTreatment(tr)
                .withPriority(10)
                .fromApp(appId)
                .makeTemporary(TIMEOUT)
                .build();
    }

    private FlowRule addFlowRule(int hval) {
        FlowRule rule = flowRule(hval, hval);
        service.applyFlowRules(rule);

        assertNotNull("rule should be found", service.getFlowEntries(DID));
        return rule;
    }

    private void validateEvents(FlowRuleEvent.Type... events) {
        if (events == null) {
            assertTrue("events generated", listener.events.isEmpty());
        }

        int i = 0;
        System.err.println("events :" + listener.events);
        for (FlowRuleEvent e : listener.events) {
            assertEquals("unexpected event", events[i], e.type());
            i++;
        }

        assertEquals("mispredicted number of events",
                     events.length, listener.events.size());

        listener.events.clear();
    }

    private int flowCount() {
        return Sets.newHashSet(service.getFlowEntries(DID)).size();
    }

    @Test
    public void getFlowEntries() {
        assertTrue("store should be empty",
                   Sets.newHashSet(service.getFlowEntries(DID)).isEmpty());
        FlowRule f1 = addFlowRule(1);
        FlowRule f2 = addFlowRule(2);

        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);
        assertEquals("2 rules should exist", 2, flowCount());

        providerService.pushFlowMetrics(DID, ImmutableList.of(fe1, fe2));
        validateEvents(RULE_ADD_REQUESTED, RULE_ADD_REQUESTED,
                       RULE_ADDED, RULE_ADDED);

        addFlowRule(1);
        System.err.println("events :" + listener.events);
        assertEquals("should still be 2 rules", 2, flowCount());

        providerService.pushFlowMetrics(DID, ImmutableList.of(fe1));
        validateEvents(RULE_UPDATED, RULE_UPDATED);
    }

    private boolean validateState(Map<FlowRule, FlowEntryState> expected) {
        Map<FlowRule, FlowEntryState> expectedToCheck = new HashMap<>(expected);
        Iterable<FlowEntry> rules = service.getFlowEntries(DID);
        for (FlowEntry f : rules) {
            assertTrue("Unexpected FlowRule " + f, expectedToCheck.containsKey(f));
            assertEquals("FlowEntry" + f, expectedToCheck.get(f), f.state());
            expectedToCheck.remove(f);
        }
        assertEquals(Collections.emptySet(), expectedToCheck.entrySet());
        return true;
    }

    @Test
    public void applyFlowRules() {

        FlowRule r1 = flowRule(1, 1);
        FlowRule r2 = flowRule(2, 2);
        FlowRule r3 = flowRule(3, 3);

        assertTrue("store should be empty",
                   Sets.newHashSet(service.getFlowEntries(DID)).isEmpty());
        mgr.applyFlowRules(r1, r2, r3);
        assertEquals("3 rules should exist", 3, flowCount());
        assertTrue("Entries should be pending add.",
                   validateState(ImmutableMap.of(
                           r1, FlowEntryState.PENDING_ADD,
                           r2, FlowEntryState.PENDING_ADD,
                           r3, FlowEntryState.PENDING_ADD)));
    }

    @Test
    public void purgeFlowRules() {
        FlowRule f1 = addFlowRule(1);
        FlowRule f2 = addFlowRule(2);
        FlowRule f3 = addFlowRule(3);
        assertEquals("3 rules should exist", 3, flowCount());
        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);
        FlowEntry fe3 = new DefaultFlowEntry(f3);
        providerService.pushFlowMetrics(DID, ImmutableList.of(fe1, fe2, fe3));
        validateEvents(RULE_ADD_REQUESTED, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED,
                       RULE_ADDED, RULE_ADDED, RULE_ADDED);
        mgr.purgeFlowRules(DID);
        assertEquals("0 rule should exist", 0, flowCount());
    }

    @Test
    public void removeFlowRules() {
        FlowRule f1 = addFlowRule(1);
        FlowRule f2 = addFlowRule(2);
        FlowRule f3 = addFlowRule(3);
        assertEquals("3 rules should exist", 3, flowCount());

        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);
        FlowEntry fe3 = new DefaultFlowEntry(f3);
        providerService.pushFlowMetrics(DID, ImmutableList.of(fe1, fe2, fe3));
        validateEvents(RULE_ADD_REQUESTED, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED,
                       RULE_ADDED, RULE_ADDED, RULE_ADDED);

        mgr.removeFlowRules(f1, f2);
        //removing from north, so no events generated
        validateEvents(RULE_REMOVE_REQUESTED, RULE_REMOVE_REQUESTED);
        assertEquals("3 rule should exist", 3, flowCount());
        assertTrue("Entries should be pending remove.",
                   validateState(ImmutableMap.of(
                           f1, FlowEntryState.PENDING_REMOVE,
                           f2, FlowEntryState.PENDING_REMOVE,
                           f3, FlowEntryState.ADDED)));

        mgr.removeFlowRules(f1);
        assertEquals("3 rule should still exist", 3, flowCount());
    }

    @Test
    public void flowRemoved() {
        FlowRule f1 = addFlowRule(1);
        FlowRule f2 = addFlowRule(2);
        StoredFlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);

        providerService.pushFlowMetrics(DID, ImmutableList.of(fe1, fe2));
        service.removeFlowRules(f1);

        //FIXME modification of "stored" flow entry outside of store
        fe1.setState(FlowEntryState.REMOVED);

        providerService.flowRemoved(fe1);

        validateEvents(RULE_ADD_REQUESTED, RULE_ADD_REQUESTED, RULE_ADDED,
                       RULE_ADDED, RULE_REMOVE_REQUESTED, RULE_REMOVED);

        providerService.flowRemoved(fe1);
        validateEvents();

        FlowRule f3 = flowRule(3, 3);
        FlowEntry fe3 = new DefaultFlowEntry(f3);
        service.applyFlowRules(f3);

        providerService.pushFlowMetrics(DID, Collections.singletonList(fe3));
        validateEvents(RULE_ADD_REQUESTED, RULE_ADDED, RULE_UPDATED);

        providerService.flowRemoved(fe3);
        validateEvents();
    }

    @Test
    public void flowMetrics() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);
        FlowRule f3 = flowRule(3, 3);

        mgr.applyFlowRules(f1, f2, f3);

        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);

        //FlowRule updatedF1 = flowRule(f1, FlowRuleState.ADDED);
        //FlowRule updatedF2 = flowRule(f2, FlowRuleState.ADDED);

        providerService.pushFlowMetrics(DID, Lists.newArrayList(fe1, fe2));

        assertTrue("Entries should be added.",
                   validateState(ImmutableMap.of(
                           f1, FlowEntryState.ADDED,
                           f2, FlowEntryState.ADDED,
                           f3, FlowEntryState.PENDING_ADD)));

        validateEvents(RULE_ADD_REQUESTED, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED,
                       RULE_ADDED, RULE_ADDED);
    }

    @Test
    public void extraneousFlow() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);
        FlowRule f3 = flowRule(3, 3);
        mgr.applyFlowRules(f1, f2);

//        FlowRule updatedF1 = flowRule(f1, FlowRuleState.ADDED);
//        FlowRule updatedF2 = flowRule(f2, FlowRuleState.ADDED);
//        FlowRule updatedF3 = flowRule(f3, FlowRuleState.ADDED);
        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);
        FlowEntry fe3 = new DefaultFlowEntry(f3);


        providerService.pushFlowMetrics(DID, Lists.newArrayList(fe1, fe2, fe3));

        validateEvents(RULE_ADD_REQUESTED, RULE_ADD_REQUESTED, RULE_ADDED, RULE_ADDED);

    }

    /*
     * Tests whether a rule that was marked for removal but no flowRemoved was received
     * is indeed removed at the next stats update.
     */
    @Test
    public void flowMissingRemove() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);
        FlowRule f3 = flowRule(3, 3);

//        FlowRule updatedF1 = flowRule(f1, FlowRuleState.ADDED);
//        FlowRule updatedF2 = flowRule(f2, FlowRuleState.ADDED);

        FlowEntry fe1 = new DefaultFlowEntry(f1);
        FlowEntry fe2 = new DefaultFlowEntry(f2);
        mgr.applyFlowRules(f1, f2, f3);

        mgr.removeFlowRules(f3);

        providerService.pushFlowMetrics(DID, Lists.newArrayList(fe1, fe2));

        validateEvents(RULE_ADD_REQUESTED, RULE_ADD_REQUESTED, RULE_ADD_REQUESTED,
                       RULE_REMOVE_REQUESTED, RULE_ADDED, RULE_ADDED, RULE_REMOVED);

    }

    @Test
    public void getByAppId() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);
        mgr.applyFlowRules(f1, f2);

        assertTrue("should have two rules",
                   Lists.newLinkedList(mgr.getFlowRulesById(appId)).size() == 2);
    }

    @Test
    public void removeByAppId() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);
        mgr.applyFlowRules(f1, f2);


        mgr.removeFlowRulesById(appId);

        //only check that we are in pending remove. Events and actual remove state will
        // be set by flowRemoved call.
        validateState(ImmutableMap.of(
                f1, FlowEntryState.PENDING_REMOVE,
                f2, FlowEntryState.PENDING_REMOVE));
    }

    @Test
    public void fallbackBasics() {
        FlowRule f1 = flowRule(FOO_DID, 1, 1);
        flowRules.clear();
        mgr.applyFlowRules(f1);
        assertTrue("flow rule not applied", flowRules.contains(f1));

        flowRules.clear();
        mgr.removeFlowRules(f1);
        assertTrue("flow rule not removed", flowRules.contains(f1));
    }

    @Test
    public void fallbackFlowRemoved() {
        FlowRule f1 = flowRule(FOO_DID, 1, 1);
        mgr.applyFlowRules(f1);
        flowRules.clear();
        providerService.flowRemoved(new DefaultFlowEntry(f1));
        assertTrue("flow rule not reapplied", flowRules.contains(f1));
    }

    @Test
    public void fallbackExtraFlow() {
        FlowRule f1 = flowRule(FOO_DID, 1, 1);
        flowRules.clear();
        providerService.pushFlowMetrics(FOO_DID, ImmutableList.of(new DefaultFlowEntry(f1)));
        assertTrue("flow rule not removed", flowRules.contains(f1));
    }

    @Test
    public void fallbackPoll() {
        FlowRuleDriverProvider fallback = (FlowRuleDriverProvider) mgr.defaultProvider();
        FlowRule f1 = flowRule(FOO_DID, 1, 1);
        mgr.applyFlowRules(f1);
        FlowEntry fe = mgr.getFlowEntries(FOO_DID).iterator().next();
        assertEquals("incorrect state", FlowEntryState.PENDING_ADD, fe.state());

        fallback.init(fallback.providerService, mgr.deviceService, mgr.mastershipService, 1);
        TestTools.assertAfter(2000, () -> {
            FlowEntry e = mgr.getFlowEntries(FOO_DID).iterator().next();
            assertEquals("incorrect state", FlowEntryState.ADDED, e.state());
        });
    }


    private static class TestListener implements FlowRuleListener {
        final List<FlowRuleEvent> events = new ArrayList<>();

        @Override
        public void event(FlowRuleEvent event) {
            events.add(event);
        }
    }

    private static class TestDeviceService extends DeviceServiceAdapter {
        @Override
        public int getDeviceCount() {
            return 2;
        }

        @Override
        public Iterable<Device> getDevices() {
            return ImmutableList.of(DEV, FOO_DEV);
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return getDevices();
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return deviceId.equals(FOO_DID) ? FOO_DEV : DEV;
        }
    }

    private class TestProvider extends AbstractProvider implements FlowRuleProvider {

        protected TestProvider(ProviderId id) {
            super(PID);
        }

        @Override
        public void applyFlowRule(FlowRule... flowRules) {
        }

        @Override
        public void removeFlowRule(FlowRule... flowRules) {
        }

        @Override
        public void removeRulesById(ApplicationId id, FlowRule... flowRules) {
        }

        @Override
        public void executeBatch(FlowRuleBatchOperation batch) {
            // TODO: need to call batchOperationComplete
        }

        private class TestInstallationFuture
                implements ListenableFuture<CompletedBatchOperation> {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public CompletedBatchOperation get()
                    throws InterruptedException, ExecutionException {
                return new CompletedBatchOperation(true, Collections.emptySet(), null);
            }

            @Override
            public CompletedBatchOperation get(long timeout, TimeUnit unit)
                    throws InterruptedException,
                    ExecutionException, TimeoutException {
                return new CompletedBatchOperation(true, Collections.emptySet(), null);
            }

            @Override
            public void addListener(Runnable task, Executor executor) {
                if (isDone()) {
                    executor.execute(task);
                }
            }
        }

    }

    private class TestSelector implements TrafficSelector {

        //for controlling hashcode uniqueness;
        private final int testval;

        public TestSelector(int val) {
            testval = val;
        }

        @Override
        public Set<Criterion> criteria() {
            return Collections.emptySet();
        }

        @Override
        public Criterion getCriterion(
                org.onosproject.net.flow.criteria.Criterion.Type type) {
            return null;
        }

        @Override
        public int hashCode() {
            return testval;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TestSelector) {
                return this.testval == ((TestSelector) o).testval;
            }
            return false;
        }

    }

    private class TestTreatment implements TrafficTreatment {

        //for controlling hashcode uniqueness;
        private final int testval;

        public TestTreatment(int val) {
            testval = val;
        }

        @Override
        public List<Instruction> deferred() {
            return null;
        }

        @Override
        public List<Instruction> immediate() {
            return null;
        }

        @Override
        public List<Instruction> allInstructions() {
            return null;
        }

        @Override
        public Instructions.TableTypeTransition tableTransition() {
            return null;
        }

        @Override
        public boolean clearedDeferred() {
            return false;
        }

        @Override
        public int hashCode() {
            return testval;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TestTreatment) {
                return this.testval == ((TestTreatment) o).testval;
            }
            return false;
        }

        @Override
        public MetadataInstruction writeMetadata() {
            return null;
        }

        @Override
        public Instructions.MeterInstruction metered() {
            return null;
        }

    }

    public class TestApplicationId extends DefaultApplicationId {
        public TestApplicationId(int id, String name) {
            super(id, name);
        }
    }

    private class TestCoreService extends CoreServiceAdapter {

        @Override
        public IdGenerator getIdGenerator(String topic) {
            return new IdGenerator() {
                private AtomicLong counter = new AtomicLong(0);

                @Override
                public long getNewId() {
                    return counter.getAndIncrement();
                }
            };
        }
    }

    private class TestMastershipService extends MastershipServiceAdapter {
        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return MastershipRole.MASTER;
        }
    }

    private class TestDriverManager extends DriverManager {
        TestDriverManager(DriverRegistry registry) {
            this.registry = registry;
            this.deviceService = mgr.deviceService;
            activate();
        }
    }

    static Collection<FlowRule> flowRules = new HashSet<>();

    public static class TestFlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

        @Override
        public Collection<FlowEntry> getFlowEntries() {
            ImmutableList.Builder<FlowEntry> builder = ImmutableList.builder();
            flowRules.stream().map(DefaultFlowEntry::new).forEach(builder::add);
            return builder.build();
        }

        @Override
        public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
            flowRules.addAll(rules);
            return rules;
        }

        @Override
        public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
            flowRules.addAll(rules);
            return rules;
        }
    }
}
