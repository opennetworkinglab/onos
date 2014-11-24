package org.onlab.onos.net.intent.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.TestApplicationId;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.impl.TestCoreManager;
import org.onlab.onos.event.impl.TestEventDispatcher;
import org.onlab.onos.net.NetworkResource;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleBatchEntry;
import org.onlab.onos.net.flow.FlowRuleBatchEntry.FlowRuleOperation;
import org.onlab.onos.net.flow.FlowRuleBatchOperation;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentCompiler;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentEvent.Type;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentInstaller;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.IntentState;
import org.onlab.onos.net.intent.IntentTestsMocks;
import org.onlab.onos.net.resource.LinkResourceAllocations;
import org.onlab.onos.store.trivial.impl.SimpleIntentBatchQueue;
import org.onlab.onos.store.trivial.impl.SimpleIntentStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.net.intent.IntentState.*;
import static org.onlab.util.Tools.delay;

/**
 * Test intent manager and transitions.
 *
 * TODO implement the following tests:
 *  - {submit, withdraw, update, replace} intent
 *  - {submit, update, recomiling} intent with failed compilation
 *  - failed reservation
 *  - push timeout recovery
 *  - failed items recovery
 *
 *  in general, verify intents store, flow store, and work queue
 */
public class IntentManagerTest {

    private static final ApplicationId APPID = new TestApplicationId("manager-test");

    private IntentManager manager;
    private MockFlowRuleService flowRuleService;

    protected IntentService service;
    protected IntentExtensionService extensionService;
    protected TestListener listener = new TestListener();
    protected TestIntentCompiler compiler = new TestIntentCompiler();
    protected TestIntentInstaller installer = new TestIntentInstaller();

    @Before
    public void setUp() {
        manager = new IntentManager();
        flowRuleService = new MockFlowRuleService();
        manager.store = new SimpleIntentStore();
        manager.batchService = new SimpleIntentBatchQueue();
        manager.eventDispatcher = new TestEventDispatcher();
        manager.trackerService = new TestIntentTracker();
        manager.flowRuleService = flowRuleService;
        manager.coreService = new TestCoreManager();
        service = manager;
        extensionService = manager;

        manager.activate();
        service.addListener(listener);
        extensionService.registerCompiler(MockIntent.class, compiler);
        extensionService.registerInstaller(MockInstallableIntent.class, installer);

        assertTrue("store should be empty",
                   Sets.newHashSet(service.getIntents()).isEmpty());
        assertEquals(0L, flowRuleService.getFlowRuleCount());
    }

    @After
    public void tearDown() {
        // verify that all intents are parked and the batch operation is unblocked
        Set<IntentState> parked = Sets.newHashSet(INSTALLED, WITHDRAWN, FAILED);
        for (Intent i : service.getIntents()) {
            IntentState state = service.getIntentState(i.id());
            assertTrue("Intent " + i.id() + " is in invalid state " + state,
                       parked.contains(state));
        }
        //the batch has not yet been removed when we receive the last event
        // FIXME: this doesn't guarantee to avoid the race
        for (int tries = 0; tries < 10; tries++) {
            if (manager.batchService.getPendingOperations().isEmpty() &&
                    manager.batchService.getCurrentOperations().isEmpty()) {
                break;
            }
            delay(10);
        }
        assertTrue("There are still pending batch operations.",
                   manager.batchService.getPendingOperations().isEmpty());
        assertTrue("There are still outstanding batch operations.",
                   manager.batchService.getCurrentOperations().isEmpty());

        extensionService.unregisterCompiler(MockIntent.class);
        extensionService.unregisterInstaller(MockInstallableIntent.class);
        service.removeListener(listener);
        manager.deactivate();
        // TODO null the other refs?
    }

    @Test
    public void submitIntent() {
        flowRuleService.setFuture(true);

        listener.setLatch(1, Type.SUBMITTED);
        listener.setLatch(1, Type.INSTALLED);
        Intent intent = new MockIntent(MockIntent.nextId());
        service.submit(intent);
        listener.await(Type.SUBMITTED);
        listener.await(Type.INSTALLED);
        assertEquals(1L, service.getIntentCount());
        assertEquals(1L, flowRuleService.getFlowRuleCount());
    }

    @Test
    public void withdrawIntent() {
        flowRuleService.setFuture(true);

        listener.setLatch(1, Type.INSTALLED);
        Intent intent = new MockIntent(MockIntent.nextId());
        service.submit(intent);
        listener.await(Type.INSTALLED);
        assertEquals(1L, service.getIntentCount());
        assertEquals(1L, flowRuleService.getFlowRuleCount());

        listener.setLatch(1, Type.WITHDRAWN);
        service.withdraw(intent);
        listener.await(Type.WITHDRAWN);
        assertEquals(1L, service.getIntentCount());
        assertEquals(0L, flowRuleService.getFlowRuleCount());
    }

    @Test
    public void stressSubmitWithdraw() {
        flowRuleService.setFuture(true);

        int count = 500;

        listener.setLatch(count, Type.INSTALLED);
        listener.setLatch(count, Type.WITHDRAWN);

        Intent intent = new MockIntent(MockIntent.nextId());
        for (int i = 0; i < count; i++) {
            service.submit(intent);
            service.withdraw(intent);
        }

        listener.await(Type.INSTALLED);
        listener.await(Type.WITHDRAWN);
        assertEquals(1L, service.getIntentCount());
        assertEquals(0L, flowRuleService.getFlowRuleCount());
    }

    @Test
    public void replaceIntent() {
        flowRuleService.setFuture(true);

        MockIntent intent = new MockIntent(MockIntent.nextId());
        listener.setLatch(1, Type.INSTALLED);
        service.submit(intent);
        listener.await(Type.INSTALLED);
        assertEquals(1L, service.getIntentCount());
        assertEquals(1L, manager.flowRuleService.getFlowRuleCount());

        MockIntent intent2 = new MockIntent(MockIntent.nextId());
        listener.setLatch(1, Type.WITHDRAWN);
        listener.setLatch(1, Type.SUBMITTED);
        listener.setLatch(1, Type.INSTALLED);
        service.replace(intent.id(), intent2);
        listener.await(Type.WITHDRAWN);
        listener.await(Type.INSTALLED);
        assertEquals(2L, service.getIntentCount());
        assertEquals(1L, manager.flowRuleService.getFlowRuleCount());
        assertEquals(intent2.number().intValue(),
                     flowRuleService.flows.iterator().next().priority());
    }

    private static class TestListener implements IntentListener {
        final Multimap<IntentEvent.Type, IntentEvent> events = HashMultimap.create();
        Map<IntentEvent.Type, CountDownLatch> latchMap = Maps.newHashMap();

        @Override
        public void event(IntentEvent event) {
            events.put(event.type(), event);
            if (latchMap.containsKey(event.type())) {
                latchMap.get(event.type()).countDown();
            }
        }

        public int getCounts(IntentEvent.Type type) {
            return events.get(type).size();
        }

        public void setLatch(int count, IntentEvent.Type type) {
            latchMap.put(type, new CountDownLatch(count));
        }

        public void await(IntentEvent.Type type) {
            try {
                latchMap.get(type).await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class TestIntentTracker implements ObjectiveTrackerService {
        private TopologyChangeDelegate delegate;
        @Override
        public void setDelegate(TopologyChangeDelegate delegate) {
            this.delegate = delegate;
        }

        @Override
        public void unsetDelegate(TopologyChangeDelegate delegate) {
            if (delegate.equals(this.delegate)) {
                this.delegate = null;
            }
        }

        @Override
        public void addTrackedResources(IntentId intentId, Collection<NetworkResource> resources) {
            //TODO
        }

        @Override
        public void removeTrackedResources(IntentId intentId, Collection<NetworkResource> resources) {
            //TODO
        }
    }

    private static class MockIntent extends Intent {
        private static AtomicLong counter = new AtomicLong(0);

        private final Long number;
        // Nothing new here
        public MockIntent(Long number) {
            super(APPID, null);
            this.number = number;
        }

        public Long number() {
            return number;
        }

        public static Long nextId() {
            return counter.getAndIncrement();
        }
    }

    private static class MockInstallableIntent extends MockIntent {
        public MockInstallableIntent(Long number) {
            super(number);
        }

        @Override
        public boolean isInstallable() {
            return true;
        }
    }

    private static class TestIntentCompiler implements IntentCompiler<MockIntent> {
        @Override
        public List<Intent> compile(MockIntent intent, List<Intent> installable,
                                    Set<LinkResourceAllocations> resources) {
            return Lists.newArrayList(new MockInstallableIntent(intent.number()));
        }
    }

    private static class TestIntentInstaller implements IntentInstaller<MockInstallableIntent> {
        @Override
        public List<FlowRuleBatchOperation> install(MockInstallableIntent intent) {
            FlowRule fr = new IntentTestsMocks.MockFlowRule(intent.number().intValue());
            List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.ADD, fr));
            return Lists.newArrayList(new FlowRuleBatchOperation(rules));
        }

        @Override
        public List<FlowRuleBatchOperation> uninstall(MockInstallableIntent intent) {
            FlowRule fr = new IntentTestsMocks.MockFlowRule(intent.number().intValue());
            List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, fr));
            return Lists.newArrayList(new FlowRuleBatchOperation(rules));
        }

        @Override
        public List<FlowRuleBatchOperation> replace(MockInstallableIntent oldIntent, MockInstallableIntent newIntent) {
            FlowRule fr = new IntentTestsMocks.MockFlowRule(oldIntent.number().intValue());
            FlowRule fr2 = new IntentTestsMocks.MockFlowRule(newIntent.number().intValue());
            List<FlowRuleBatchEntry> rules = Lists.newLinkedList();
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.REMOVE, fr));
            rules.add(new FlowRuleBatchEntry(FlowRuleOperation.ADD, fr2));
            return Lists.newArrayList(new FlowRuleBatchOperation(rules));
        }
    }

    /**
     * Hamcrest matcher to check that a conllection of Intents contains an
     * Intent with the specified Intent Id.
     */
    public static class EntryForIntentMatcher extends TypeSafeMatcher<Collection<Intent>> {
        private final String id;

        public EntryForIntentMatcher(String idValue) {
            id = idValue;
        }

        @Override
        public boolean matchesSafely(Collection<Intent> intents) {
            return hasItem(Matchers.<Intent>hasProperty("id", equalTo(id))).matches(intents);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("an intent with id \" ").
                    appendText(id).
                    appendText("\"");
        }
    }
}
