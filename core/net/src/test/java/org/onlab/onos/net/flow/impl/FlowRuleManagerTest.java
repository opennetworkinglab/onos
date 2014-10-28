package org.onlab.onos.net.flow.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.DefaultApplicationId;
import org.onlab.onos.event.impl.TestEventDispatcher;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Device.Type;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.BatchOperation;
import org.onlab.onos.net.flow.CompletedBatchOperation;
import org.onlab.onos.net.flow.DefaultFlowEntry;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowEntry.FlowEntryState;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleListener;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.StoredFlowEntry;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.trivial.impl.SimpleFlowRuleStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.onlab.onos.net.flow.FlowRuleEvent.Type.*;

/**
 * Test codifying the flow rule service & flow rule provider service contracts.
 */
public class FlowRuleManagerTest {


    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID = DeviceId.deviceId("of:001");
    private static final int TIMEOUT = 10;
    private static final Device DEV = new DefaultDevice(
            PID, DID, Type.SWITCH, "", "", "", "", null);

    private FlowRuleManager mgr;

    protected FlowRuleService service;
    protected FlowRuleProviderRegistry registry;
    protected FlowRuleProviderService providerService;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();
    private ApplicationId appId;

    @Before
    public void setUp() {
        mgr = new FlowRuleManager();
        mgr.store = new SimpleFlowRuleStore();
        mgr.eventDispatcher = new TestEventDispatcher();
        mgr.deviceService = new TestDeviceService();
        service = mgr;
        registry = mgr;

        mgr.activate();
        mgr.addListener(listener);
        provider = new TestProvider(PID);
        providerService = registry.register(provider);
        appId = new TestApplicationId((short) 0, "FlowRuleManagerTest");
        assertTrue("provider should be registered",
                   registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                    registry.getProviders().contains(provider.id()));
        service.removeListener(listener);
        mgr.deactivate();
        mgr.eventDispatcher = null;
        mgr.deviceService = null;
    }

    private FlowRule flowRule(int tsval, int trval) {
        TestSelector ts = new TestSelector(tsval);
        TestTreatment tr = new TestTreatment(trval);
        return new DefaultFlowRule(DID, ts, tr, 10, appId, TIMEOUT, false);
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
            assertTrue("unexpected event", e.type().equals(events[i]));
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
        assertEquals("should still be 2 rules", 2, flowCount());

        providerService.pushFlowMetrics(DID, ImmutableList.of(fe1));
        validateEvents(RULE_UPDATED);
    }


    // TODO: If preserving iteration order is a requirement, redo FlowRuleStore.
    //backing store is sensitive to the order of additions/removals
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
        validateEvents(RULE_ADD_REQUESTED, RULE_ADDED);

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
    public void applyBatch() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);


        mgr.applyFlowRules(f1);

        FlowEntry fe1 = new DefaultFlowEntry(f1);
        providerService.pushFlowMetrics(DID, Collections.<FlowEntry>singletonList(fe1));

        FlowRuleBatchEntry fbe1 = new FlowRuleBatchEntry(
                FlowRuleBatchEntry.FlowRuleOperation.REMOVE, f1);

        FlowRuleBatchEntry fbe2 = new FlowRuleBatchEntry(
                FlowRuleBatchEntry.FlowRuleOperation.ADD, f2);

        FlowRuleBatchOperation fbo = new FlowRuleBatchOperation(
                Lists.newArrayList(fbe1, fbe2));
        Future<CompletedBatchOperation> future = mgr.applyBatch(fbo);
        assertTrue("Entries in wrong state",
                   validateState(ImmutableMap.of(
                           f1, FlowEntryState.PENDING_REMOVE,
                           f2, FlowEntryState.PENDING_ADD)));
        CompletedBatchOperation completed = null;
        try {
            completed = future.get();
        } catch (InterruptedException | ExecutionException e) {
            fail("Unexpected exception: " + e);
        }
        if (!completed.isSuccess()) {
            fail("Installation should be a success");
        }

    }

    @Test
    public void cancelBatch() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);


        mgr.applyFlowRules(f1);

        assertTrue("Entries in wrong state",
                   validateState(ImmutableMap.of(
                           f1, FlowEntryState.PENDING_ADD)));

        FlowEntry fe1 = new DefaultFlowEntry(f1);
        providerService.pushFlowMetrics(DID, Collections.<FlowEntry>singletonList(fe1));

        assertTrue("Entries in wrong state",
                   validateState(ImmutableMap.of(
                           f1, FlowEntryState.ADDED)));


        FlowRuleBatchEntry fbe1 = new FlowRuleBatchEntry(
                FlowRuleBatchEntry.FlowRuleOperation.REMOVE, f1);

        FlowRuleBatchEntry fbe2 = new FlowRuleBatchEntry(
                FlowRuleBatchEntry.FlowRuleOperation.ADD, f2);

        FlowRuleBatchOperation fbo = new FlowRuleBatchOperation(
                Lists.newArrayList(fbe1, fbe2));
        Future<CompletedBatchOperation> future = mgr.applyBatch(fbo);

        future.cancel(true);

        assertTrue(flowCount() == 2);

        /*
         * Rule f1 should be re-added to the list and therefore be in a pending add
         * state.
         */
        assertTrue("Entries in wrong state",
                   validateState(ImmutableMap.of(
                           f2, FlowEntryState.PENDING_REMOVE,
                           f1, FlowEntryState.PENDING_ADD)));


    }


    private static class TestListener implements FlowRuleListener {
        final List<FlowRuleEvent> events = new ArrayList<>();

        @Override
        public void event(FlowRuleEvent event) {
            events.add(event);
        }
    }

    private static class TestDeviceService implements DeviceService {

        @Override
        public int getDeviceCount() {
            return 1;
        }

        @Override
        public Iterable<Device> getDevices() {
            return Arrays.asList(DEV);
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return DEV;
        }

        @Override
        public MastershipRole getRole(DeviceId deviceId) {
            return null;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            return null;
        }

        @Override
        public Port getPort(DeviceId deviceId, PortNumber portNumber) {
            return null;
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return false;
        }

        @Override
        public void addListener(DeviceListener listener) {
        }

        @Override
        public void removeListener(DeviceListener listener) {
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
        public ListenableFuture<CompletedBatchOperation> executeBatch(
                BatchOperation<FlowRuleBatchEntry> batch) {
            return new TestInstallationFuture();
        }

        private class TestInstallationFuture
                implements ListenableFuture<CompletedBatchOperation> {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return true;
            }

            @Override
            public boolean isCancelled() {
                return true;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public CompletedBatchOperation get()
                    throws InterruptedException, ExecutionException {
                return new CompletedBatchOperation(true, Collections.<FlowEntry>emptySet());
            }

            @Override
            public CompletedBatchOperation get(long timeout, TimeUnit unit)
                    throws InterruptedException,
                    ExecutionException, TimeoutException {
                return null;
            }

            @Override
            public void addListener(Runnable task, Executor executor) {
                // TODO: add stuff.
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
            return null;
        }

        @Override
        public Criterion getCriterion(
                org.onlab.onos.net.flow.criteria.Criterion.Type type) {
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
        public List<Instruction> instructions() {
            return null;
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

    }

    public class TestApplicationId extends DefaultApplicationId {

        public TestApplicationId(short id, String name) {
            super(id, name);
        }
    }

}
