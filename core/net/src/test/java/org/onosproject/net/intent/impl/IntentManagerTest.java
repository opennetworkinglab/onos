/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.intent.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.TestApplicationId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.impl.TestCoreManager;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentCompilationException;
import org.onosproject.net.intent.IntentCompiler;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentEvent.Type;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentInstallCoordinator;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.ObjectiveTrackerService;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.TopologyChangeDelegate;
import org.onosproject.store.trivial.SimpleIntentStore;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.easymock.EasyMock.mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onlab.junit.TestTools.assertAfter;
import static org.onlab.util.Tools.delay;
import static org.onosproject.net.NetTestTools.injectEventDispatcher;
import static org.onosproject.net.intent.IntentState.*;
import static org.onosproject.net.intent.IntentTestsMocks.MockFlowRule;
import static org.onosproject.net.intent.IntentTestsMocks.MockIntent;

/**
 * Test intent manager and transitions.
 *
 * TODO implement the following tests:
 *  - {submit, withdraw, update, replace} intent
 *  - {submit, update, recompiling} intent with failed compilation
 *  - failed reservation
 *  - push timeout recovery
 *  - failed items recovery
 *
 *  in general, verify intents store, flow store, and work queue
 */

public class IntentManagerTest {

    private static final int SUBMIT_TIMEOUT_MS = 1000;
    private static final ApplicationId APPID = new TestApplicationId("manager-test");

    private IntentManager manager;
    private MockFlowRuleService flowRuleService;

    protected IntentService service;
    protected IntentExtensionService extensionService;
    protected IntentInstallCoordinator intentInstallCoordinator;
    protected TestListener listener = new TestListener();
    protected TestIntentCompiler compiler = new TestIntentCompiler();
    protected TestIntentInstaller installer;
    protected TestIntentTracker trackerService = new TestIntentTracker();

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
                assertTrue("Timed out waiting for: " + type,
                        latchMap.get(type).await(5, TimeUnit.SECONDS));
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
        public void addTrackedResources(Key key, Collection<NetworkResource> resources) {
            //TODO
        }

        @Override
        public void removeTrackedResources(Key key, Collection<NetworkResource> resources) {
            //TODO
        }

        @Override
        public void trackIntent(IntentData intentData) {
            //TODO
        }
    }

    private static class MockInstallableIntent extends FlowRuleIntent {

        public MockInstallableIntent() {
            super(APPID, null, Collections.singletonList(new MockFlowRule(100)), Collections.emptyList(),
                    PathIntent.ProtectionType.PRIMARY, null);
        }
    }

    private static class TestIntentCompiler implements IntentCompiler<MockIntent> {
        @Override
        public List<Intent> compile(MockIntent intent, List<Intent> installable) {
            return Lists.newArrayList(new MockInstallableIntent());
        }
    }

    private static class TestIntentCompilerMultipleFlows implements IntentCompiler<MockIntent> {
        @Override
        public List<Intent> compile(MockIntent intent, List<Intent> installable) {

            return IntStream.rangeClosed(1, 5)
                            .mapToObj(mock -> (new MockInstallableIntent()))
                            .collect(Collectors.toList());
        }
    }


    private static class TestIntentCompilerError implements IntentCompiler<MockIntent> {
        @Override
        public List<Intent> compile(MockIntent intent, List<Intent> installable) {
            throw new IntentCompilationException("Compilation always fails");
        }
    }

    /**
     * Hamcrest matcher to check that a collection of Intents contains an
     * Intent with the specified Intent Id.
     */
    public static class EntryForIntentMatcher extends TypeSafeMatcher<Collection<Intent>> {
        private final IntentId id;

        public EntryForIntentMatcher(IntentId idValue) {
            id = idValue;
        }

        @Override
        public boolean matchesSafely(Collection<Intent> intents) {
            for (Intent intent : intents) {
                if (intent.id().equals(id)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("an intent with id \" ").
                    appendText(id.toString()).
                    appendText("\"");
        }
    }

    public static class TestIntentInstaller implements IntentInstaller<MockInstallableIntent> {

        protected IntentExtensionService intentExtensionService;
        protected ObjectiveTrackerService trackerService;
        protected IntentInstallCoordinator intentInstallCoordinator;
        protected FlowRuleService flowRuleService;

        public TestIntentInstaller(IntentExtensionService intentExtensionService,
                                   ObjectiveTrackerService trackerService,
                                   IntentInstallCoordinator intentInstallCoordinator,
                                   FlowRuleService flowRuleService) {
            this.intentExtensionService = intentExtensionService;
            this.trackerService = trackerService;
            this.intentInstallCoordinator = intentInstallCoordinator;
            this.flowRuleService = flowRuleService;
        }

        @Override
        public void apply(IntentOperationContext<MockInstallableIntent> context) {
            List<MockInstallableIntent> uninstallIntents = context.intentsToUninstall();
            List<MockInstallableIntent> installIntents = context.intentsToInstall();

            FlowRuleOperations.Builder builder = FlowRuleOperations.builder();

            uninstallIntents.stream()
                    .map(FlowRuleIntent::flowRules)
                    .flatMap(Collection::stream)
                    .forEach(builder::remove);

            installIntents.stream()
                    .map(FlowRuleIntent::flowRules)
                    .flatMap(Collection::stream)
                    .forEach(builder::add);

            FlowRuleOperationsContext ctx = new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    intentInstallCoordinator.intentInstallSuccess(context);
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    intentInstallCoordinator.intentInstallFailed(context);
                }
            };

            flowRuleService.apply(builder.build(ctx));
        }
    }

    private static EntryForIntentMatcher hasIntentWithId(IntentId id) {
        return new EntryForIntentMatcher(id);
    }

    @Before
    public void setUp() {
        manager = new IntentManager();
        flowRuleService = new MockFlowRuleService();
        manager.store = new SimpleIntentStore();
        injectEventDispatcher(manager, new TestEventDispatcher());
        manager.trackerService = trackerService;
        manager.flowRuleService = flowRuleService;
        manager.coreService = new TestCoreManager();
        manager.configService = mock(ComponentConfigService.class);
        service = manager;
        extensionService = manager;
        intentInstallCoordinator = manager;


        manager.activate();
        service.addListener(listener);
        extensionService.registerCompiler(MockIntent.class, compiler);

        installer = new TestIntentInstaller(extensionService, trackerService,
                                            intentInstallCoordinator, flowRuleService);

        extensionService.registerInstaller(MockInstallableIntent.class, installer);

        assertTrue("store should be empty",
                   Sets.newHashSet(service.getIntents()).isEmpty());
        assertEquals(0L, flowRuleService.getFlowRuleCount());
    }

    public void verifyState() {
        // verify that all intents are parked and the batch operation is unblocked
        Set<IntentState> parked = Sets.newHashSet(INSTALLED, WITHDRAWN, FAILED, CORRUPT);
        for (Intent i : service.getIntents()) {
            IntentState state = service.getIntentState(i.key());
            assertTrue("Intent " + i.id() + " is in invalid state " + state,
                       parked.contains(state));
        }
        //the batch has not yet been removed when we receive the last event
        // FIXME: this doesn't guarantee to avoid the race

        //FIXME
//        for (int tries = 0; tries < 10; tries++) {
//            if (manager.batchService.getPendingOperations().isEmpty()) {
//                break;
//            }
//            delay(10);
//        }
//        assertTrue("There are still pending batch operations.",
//                   manager.batchService.getPendingOperations().isEmpty());

    }

    @After
    public void tearDown() {
        extensionService.unregisterCompiler(MockIntent.class);
        extensionService.unregisterInstaller(MockInstallableIntent.class);
        service.removeListener(listener);
        manager.deactivate();
        // TODO null the other refs?
    }

    @Test
    public void submitIntent() {
        flowRuleService.setFuture(true);

        listener.setLatch(1, Type.INSTALL_REQ);
        listener.setLatch(1, Type.INSTALLED);
        Intent intent = new MockIntent(MockIntent.nextId());
        service.submit(intent);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.INSTALLED);
        assertEquals(1L, service.getIntentCount());
        assertEquals(1L, flowRuleService.getFlowRuleCount());
        verifyState();
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
        assertEquals(0L, flowRuleService.getFlowRuleCount());
        verifyState();
    }

    @Test
    @Ignore("This is disabled because we are seeing intermittent failures on Jenkins")
    public void stressSubmitWithdrawUnique() {
        flowRuleService.setFuture(true);

        int count = 500;
        Intent[] intents = new Intent[count];

        listener.setLatch(count, Type.WITHDRAWN);

        for (int i = 0; i < count; i++) {
            intents[i] = new MockIntent(MockIntent.nextId());
            service.submit(intents[i]);
        }

        for (int i = 0; i < count; i++) {
            service.withdraw(intents[i]);
        }

        listener.await(Type.WITHDRAWN);
        assertEquals(0L, flowRuleService.getFlowRuleCount());
        verifyState();
    }

    @Test
    public void stressSubmitWithdrawSame() {
        flowRuleService.setFuture(true);

        int count = 50;

        Intent intent = new MockIntent(MockIntent.nextId());
        for (int i = 0; i < count; i++) {
            service.submit(intent);
            service.withdraw(intent);
        }

        assertAfter(SUBMIT_TIMEOUT_MS, () -> {
            assertEquals(1L, service.getIntentCount());
            assertEquals(0L, flowRuleService.getFlowRuleCount());
        });
        verifyState();
    }


    /**
     * Tests for proper behavior of installation of an intent that triggers
     * a compilation error.
     */
    @Test
    public void errorIntentCompile() {
        final TestIntentCompilerError errorCompiler = new TestIntentCompilerError();
        extensionService.registerCompiler(MockIntent.class, errorCompiler);
        MockIntent intent = new MockIntent(MockIntent.nextId());
        listener.setLatch(1, Type.INSTALL_REQ);
        listener.setLatch(1, Type.FAILED);
        service.submit(intent);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.FAILED);
        verifyState();
    }

    /**
     * Tests handling a future that contains an error as a result of
     * installing an intent.
     */
    @Ignore("skipping until we fix update ordering problem")
    @Test
    public void errorIntentInstallFromFlows() {
        final Long id = MockIntent.nextId();
        flowRuleService.setFuture(false);
        MockIntent intent = new MockIntent(id);
        listener.setLatch(1, Type.FAILED);
        listener.setLatch(1, Type.INSTALL_REQ);
        service.submit(intent);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.FAILED);
        // FIXME the intent will be moved into INSTALLED immediately which overrides FAILED
        // ... the updates come out of order
        verifyState();
    }

    /**
     * Tests handling a future that contains an unresolvable error as a result of
     * installing an intent.
     */
    @Test
    public void errorIntentInstallNeverTrue() {
        final Long id = MockIntent.nextId();
        flowRuleService.setFuture(false);
        MockIntent intent = new MockIntent(id);
        listener.setLatch(1, Type.CORRUPT);
        listener.setLatch(1, Type.INSTALL_REQ);
        service.submit(intent);
        listener.await(Type.INSTALL_REQ);
        // The delay here forces the retry loop in the intent manager to time out
        delay(100);
        flowRuleService.setFuture(false);
        service.withdraw(intent);
        listener.await(Type.CORRUPT);
        verifyState();
    }

    /**
     * Tests that a compiler for a subclass of an intent that already has a
     * compiler is automatically added.
     */
    @Test
    public void intentSubclassCompile() {
        class MockIntentSubclass extends MockIntent {
            public MockIntentSubclass(Long number) {
                super(number);
            }
        }
        flowRuleService.setFuture(true);

        listener.setLatch(1, Type.INSTALL_REQ);
        listener.setLatch(1, Type.INSTALLED);
        Intent intent = new MockIntentSubclass(MockIntent.nextId());
        service.submit(intent);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.INSTALLED);
        assertEquals(1L, service.getIntentCount());
        assertEquals(1L, flowRuleService.getFlowRuleCount());

        final Map<Class<? extends Intent>, IntentCompiler<? extends Intent>> compilers =
                extensionService.getCompilers();
        assertEquals(2, compilers.size());
        assertNotNull(compilers.get(MockIntentSubclass.class));
        assertNotNull(compilers.get(MockIntent.class));
        verifyState();
    }

    /**
     * Tests an intent with no compiler.
     */
    @Test
    public void intentWithoutCompiler() {
        class IntentNoCompiler extends Intent {
            IntentNoCompiler() {
                super(APPID, null, Collections.emptyList(),
                        Intent.DEFAULT_INTENT_PRIORITY, null);
            }
        }

        Intent intent = new IntentNoCompiler();
        listener.setLatch(1, Type.INSTALL_REQ);
        listener.setLatch(1, Type.FAILED);
        service.submit(intent);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.FAILED);
        verifyState();
    }

    /**
     * Tests an intent with no installer.
     */
    @Test
    public void intentWithoutInstaller() {
        MockIntent intent = new MockIntent(MockIntent.nextId());
        listener.setLatch(1, Type.INSTALL_REQ);
        listener.setLatch(1, Type.CORRUPT);
        service.submit(intent);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.CORRUPT);
        verifyState();
    }

    /**
     * Tests that the intent fetching methods are correct.
     */
    @Test
    public void testIntentFetching() {
        List<Intent> intents;

        flowRuleService.setFuture(true);

        intents = Lists.newArrayList(service.getIntents());
        assertThat(intents, hasSize(0));

        final MockIntent intent1 = new MockIntent(MockIntent.nextId(), NetTestTools.APP_ID);
        final MockIntent intent2 = new MockIntent(MockIntent.nextId(), NetTestTools.APP_ID_2);

        listener.setLatch(2, Type.INSTALL_REQ);
        listener.setLatch(2, Type.INSTALLED);
        service.submit(intent1);
        service.submit(intent2);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.INSTALLED);
        listener.await(Type.INSTALLED);

        intents = Lists.newArrayList(service.getIntents());
        assertThat(intents, hasSize(2));

        assertThat(intents, hasIntentWithId(intent1.id()));
        assertThat(intents, hasIntentWithId(intent2.id()));
        verifyState();

        List<Intent> intentsAppId = Lists.newArrayList(service.getIntentsByAppId(NetTestTools.APP_ID));
        assertThat(intentsAppId, hasSize(1));
        assertThat(intentsAppId, hasIntentWithId(intent1.id()));

        List<Intent> intentsAppId2 = Lists.newArrayList(service.getIntentsByAppId(NetTestTools.APP_ID_2));
        assertThat(intentsAppId2, hasSize(1));
        assertThat(intentsAppId2, hasIntentWithId(intent2.id()));
        verifyState();
    }

    /**
     * Tests that removing all intents results in no flows remaining.
     */
    @Test
    public void testFlowRemoval() {
        List<Intent> intents;

        flowRuleService.setFuture(true);

        intents = Lists.newArrayList(service.getIntents());
        assertThat(intents, hasSize(0));

        final MockIntent intent1 = new MockIntent(MockIntent.nextId());
        final MockIntent intent2 = new MockIntent(MockIntent.nextId());

        listener.setLatch(1, Type.INSTALL_REQ);
        listener.setLatch(1, Type.INSTALLED);

        service.submit(intent1);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.INSTALLED);


        listener.setLatch(1, Type.INSTALL_REQ);
        listener.setLatch(1, Type.INSTALLED);

        service.submit(intent2);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.INSTALLED);

        assertThat(listener.getCounts(Type.INSTALLED), is(2));
        assertThat(flowRuleService.getFlowRuleCount(), is(2));

        listener.setLatch(1, Type.WITHDRAWN);
        service.withdraw(intent1);
        listener.await(Type.WITHDRAWN);

        listener.setLatch(1, Type.WITHDRAWN);
        service.withdraw(intent2);
        listener.await(Type.WITHDRAWN);

        assertThat(listener.getCounts(Type.WITHDRAWN), is(2));
        assertThat(flowRuleService.getFlowRuleCount(), is(0));
    }

    /**
     * Test failure to install an intent, then succeed on retry via IntentCleanup.
     */
    @Test
    public void testCorruptCleanup() {
        IntentCleanup cleanup = new IntentCleanup();
        cleanup.service = manager;
        cleanup.store = manager.store;
        cleanup.cfgService = new ComponentConfigAdapter();

        try {
            cleanup.activate();

            final TestIntentCompilerMultipleFlows errorCompiler = new TestIntentCompilerMultipleFlows();
            extensionService.registerCompiler(MockIntent.class, errorCompiler);
            List<Intent> intents;

            flowRuleService.setFuture(false);

            intents = Lists.newArrayList(service.getIntents());
            assertThat(intents, hasSize(0));

            final MockIntent intent1 = new MockIntent(MockIntent.nextId());

            listener.setLatch(1, Type.INSTALL_REQ);
            listener.setLatch(1, Type.CORRUPT);
            listener.setLatch(1, Type.INSTALLED);

            service.submit(intent1);

            listener.await(Type.INSTALL_REQ);
            listener.await(Type.CORRUPT);

            flowRuleService.setFuture(true);

            listener.await(Type.INSTALLED);

            assertThat(listener.getCounts(Type.CORRUPT), is(1));
            assertThat(listener.getCounts(Type.INSTALLED), is(1));
            assertEquals(INSTALLED, manager.getIntentState(intent1.key()));
            assertThat(flowRuleService.getFlowRuleCount(), is(5));
        } finally {
            cleanup.deactivate();
        }
    }

    /**
     * Test failure to install an intent, and verify retries.
     */
    @Test
    public void testCorruptRetry() {
        IntentCleanup cleanup = new IntentCleanup();
        cleanup.service = manager;
        cleanup.store = manager.store;
        cleanup.cfgService = new ComponentConfigAdapter();
        cleanup.period = 1_000_000;
        cleanup.retryThreshold = 3;

        try {
            cleanup.activate();

            final TestIntentCompilerMultipleFlows errorCompiler = new TestIntentCompilerMultipleFlows();
            extensionService.registerCompiler(MockIntent.class, errorCompiler);
            List<Intent> intents;

            flowRuleService.setFuture(false);

            intents = Lists.newArrayList(service.getIntents());
            assertThat(intents, hasSize(0));

            final MockIntent intent1 = new MockIntent(MockIntent.nextId());

            listener.setLatch(1, Type.INSTALL_REQ);
            listener.setLatch(cleanup.retryThreshold, Type.CORRUPT);
            listener.setLatch(1, Type.INSTALLED);

            service.submit(intent1);

            listener.await(Type.INSTALL_REQ);
            listener.await(Type.CORRUPT);
            assertEquals(CORRUPT, manager.getIntentState(intent1.key()));
            assertThat(listener.getCounts(Type.CORRUPT), is(cleanup.retryThreshold));

        } finally {
            cleanup.deactivate();
        }
    }

    /**
     * Tests that an intent that fails installation results in no flows remaining.
     */
    @Test
    @Ignore("MockFlowRule numbering issue") //test works if run independently
    public void testFlowRemovalInstallError() {
        final TestIntentCompilerMultipleFlows errorCompiler = new TestIntentCompilerMultipleFlows();
        extensionService.registerCompiler(MockIntent.class, errorCompiler);
        List<Intent> intents;

        flowRuleService.setFuture(true);
        //FIXME relying on "3" is brittle
        flowRuleService.setErrorFlow(3);

        intents = Lists.newArrayList(service.getIntents());
        assertThat(intents, hasSize(0));

        final MockIntent intent1 = new MockIntent(MockIntent.nextId());

        listener.setLatch(1, Type.INSTALL_REQ);
        listener.setLatch(1, Type.CORRUPT);

        service.submit(intent1);
        listener.await(Type.INSTALL_REQ);
        listener.await(Type.CORRUPT);

        assertThat(listener.getCounts(Type.CORRUPT), is(1));
        // in this test, there will still be flows abandoned on the data plane
        //assertThat(flowRuleService.getFlowRuleCount(), is(0));
    }
}
