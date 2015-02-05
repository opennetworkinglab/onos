/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.onosproject.net.intent.IntentEvent.Type.FAILED;
import static org.onosproject.net.intent.IntentEvent.Type.INSTALLED;
import static org.onosproject.net.intent.IntentEvent.Type.INSTALL_REQ;
import static org.onosproject.net.intent.IntentEvent.Type.WITHDRAWN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.resource.LinkResourceAllocations;

/**
 * Suite of tests for the intent service contract.
 */
@Ignore
public class IntentServiceTest {

    public static final int IID = 123;
    public static final int INSTALLABLE_IID = 234;

    protected static final int GRACE_MS = 500; // millis

    protected TestableIntentService service;
    protected TestListener listener = new TestListener();
    protected IdGenerator idGenerator = new MockIdGenerator();

    @Before
    public void setUp() {
        service = createIntentService();
        service.addListener(listener);
        Intent.bindIdGenerator(idGenerator);
    }

    @After
    public void tearDown() {
        service.removeListener(listener);
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Creates a service instance appropriately instrumented for testing.
     *
     * @return testable intent service
     */
    protected TestableIntentService createIntentService() {
        return new FakeIntentManager();
    }

    @Test
    public void basics() {
        // Make sure there are no intents
        assertEquals("incorrect intent count", 0, service.getIntentCount());

        // Register a compiler and an installer both setup for success.
        service.registerCompiler(TestIntent.class, new TestCompiler(new TestInstallableIntent(INSTALLABLE_IID)));
        service.registerInstaller(TestInstallableIntent.class, new TestInstaller(false));

        final Intent intent = new TestIntent(IID);
        service.submit(intent);

        // Allow a small window of time until the intent is in the expected state
        TestTools.assertAfter(GRACE_MS, new Runnable() {
            @Override
            public void run() {
                assertEquals("incorrect intent state", IntentState.INSTALLED,
                             service.getIntentState(intent.key()));
            }
        });

        // Make sure that all expected events have been emitted
        validateEvents(intent, INSTALL_REQ, INSTALLED);

        // Make sure there is just one intent (and is ours)
        assertEquals("incorrect intent count", 1, service.getIntentCount());

        // Reset the listener events
        listener.events.clear();

        // Now withdraw the intent
        service.withdraw(intent);

        // Allow a small window of time until the event is in the expected state
        TestTools.assertAfter(GRACE_MS, new Runnable() {
            @Override
            public void run() {
                assertEquals("incorrect intent state", IntentState.WITHDRAWN,
                             service.getIntentState(intent.key()));
            }
        });

        // Make sure that all expected events have been emitted
        validateEvents(intent, WITHDRAWN);

        // TODO: discuss what is the fate of intents after they have been withdrawn
        // Make sure that the intent is no longer in the system
//        assertEquals("incorrect intent count", 0, service.getIntents().size());
//        assertNull("intent should not be found", service.getIntent(intent.id()));
//        assertNull("intent state should not be found", service.getIntentState(intent.id()));
    }

    @Test
    public void failedCompilation() {
        // Register a compiler programmed for success
        service.registerCompiler(TestIntent.class, new TestCompiler(true));

        // Submit an intent
        final Intent intent = new TestIntent(IID);
        service.submit(intent);

        // Allow a small window of time until the intent is in the expected state
        TestTools.assertAfter(GRACE_MS, new Runnable() {
            @Override
            public void run() {
                assertEquals("incorrect intent state", IntentState.FAILED,
                             service.getIntentState(intent.key()));
            }
        });

        // Make sure that all expected events have been emitted
        validateEvents(intent, INSTALL_REQ, FAILED);
    }

    @Test
    public void failedInstallation() {
        // Register a compiler programmed for success and installer for failure
        service.registerCompiler(TestIntent.class, new TestCompiler(new TestInstallableIntent(INSTALLABLE_IID)));
        service.registerInstaller(TestInstallableIntent.class, new TestInstaller(true));

        // Submit an intent
        final Intent intent = new TestIntent(IID);
        service.submit(intent);

        // Allow a small window of time until the intent is in the expected state
        TestTools.assertAfter(GRACE_MS, new Runnable() {
            @Override
            public void run() {
                assertEquals("incorrect intent state", IntentState.FAILED,
                             service.getIntentState(intent.key()));
            }
        });

        // Make sure that all expected events have been emitted
        validateEvents(intent, INSTALL_REQ, FAILED);
    }

    /**
     * Validates that the test event listener has received the following events
     * for the specified intent. Events received for other intents will not be
     * considered.
     *
     * @param intent intent subject
     * @param types  list of event types for which events are expected
     */
    protected void validateEvents(Intent intent, IntentEvent.Type... types) {
        Iterator<IntentEvent> events = listener.events.iterator();
        for (IntentEvent.Type type : types) {
            IntentEvent event = events.hasNext() ? events.next() : null;
            if (event == null) {
                fail("expected event not found: " + type);
            } else if (intent.equals(event.subject())) {
                assertEquals("incorrect state", type, event.type());
            }
        }

        // Remainder of events should not apply to this intent; make sure.
        while (events.hasNext()) {
            assertFalse("unexpected event for intent",
                        intent.equals(events.next().subject()));
        }
    }

    @Test
    public void compilerBasics() {
        // Make sure there are no compilers
        assertEquals("incorrect compiler count", 0, service.getCompilers().size());

        // Add a compiler and make sure that it appears in the map
        IntentCompiler<TestIntent> compiler = new TestCompiler(false);
        service.registerCompiler(TestIntent.class, compiler);
        assertEquals("incorrect compiler", compiler,
                     service.getCompilers().get(TestIntent.class));

        // Remove the same and make sure that it no longer appears in the map
        service.unregisterCompiler(TestIntent.class);
        assertNull("compiler should not be registered",
                   service.getCompilers().get(TestIntent.class));
    }

    @Test
    public void installerBasics() {
        // Make sure there are no installers
        assertEquals("incorrect installer count", 0, service.getInstallers().size());

        // Add an installer and make sure that it appears in the map
        IntentInstaller<TestInstallableIntent> installer = new TestInstaller(false);
        service.registerInstaller(TestInstallableIntent.class, installer);
        assertEquals("incorrect installer", installer,
                     service.getInstallers().get(TestInstallableIntent.class));

        // Remove the same and make sure that it no longer appears in the map
        service.unregisterInstaller(TestInstallableIntent.class);
        assertNull("installer should not be registered",
                   service.getInstallers().get(TestInstallableIntent.class));
    }

    @Test
    public void implicitRegistration() {
        // Add a compiler and make sure that it appears in the map
        IntentCompiler<TestIntent> compiler = new TestCompiler(new TestSubclassInstallableIntent(INSTALLABLE_IID));
        service.registerCompiler(TestIntent.class, compiler);
        assertEquals("incorrect compiler", compiler,
                     service.getCompilers().get(TestIntent.class));

        // Add a installer and make sure that it appears in the map
        IntentInstaller<TestInstallableIntent> installer = new TestInstaller(false);
        service.registerInstaller(TestInstallableIntent.class, installer);
        assertEquals("incorrect installer", installer,
                     service.getInstallers().get(TestInstallableIntent.class));


        // Submit an intent which is a subclass of the one we registered
        final Intent intent = new TestSubclassIntent(IID);
        service.submit(intent);

        // Allow some time for the intent to be compiled and installed
        TestTools.assertAfter(GRACE_MS, new Runnable() {
            @Override
            public void run() {
                assertEquals("incorrect intent state", IntentState.INSTALLED,
                             service.getIntentState(intent.key()));
            }
        });

        // Make sure that now we have an implicit registration of the compiler
        // under the intent subclass
        assertEquals("incorrect compiler", compiler,
                     service.getCompilers().get(TestSubclassIntent.class));

        // Make sure that now we have an implicit registration of the installer
        // under the intent subclass
        assertEquals("incorrect installer", installer,
                     service.getInstallers().get(TestSubclassInstallableIntent.class));

        // TODO: discuss whether or if implicit registration should require implicit unregistration
        // perhaps unregister by compiler or installer itself, rather than by class would be better
    }


    // Fixture to track emitted intent events
    protected class TestListener implements IntentListener {
        final List<IntentEvent> events = new ArrayList<>();

        @Override
        public void event(IntentEvent event) {
            events.add(event);
        }
    }

    // Controllable compiler
    private class TestCompiler implements IntentCompiler<TestIntent> {
        private final boolean fail;
        private final List<Intent> result;

        TestCompiler(boolean fail) {
            this.fail = fail;
            this.result = Collections.emptyList();
        }

        TestCompiler(Intent... result) {
            this.fail = false;
            this.result = Arrays.asList(result);
        }

        @Override
        public List<Intent> compile(TestIntent intent, List<Intent> installable,
                                    Set<LinkResourceAllocations> resources) {
            if (fail) {
                throw new IntentException("compile failed by design");
            }
            List<Intent> compiled = new ArrayList<>(result);
            return compiled;
        }
    }

    // Controllable installer
    private class TestInstaller implements IntentInstaller<TestInstallableIntent> {
        private final boolean fail;

        TestInstaller(boolean fail) {
            this.fail = fail;
        }

        @Override
        public List<FlowRuleBatchOperation> install(TestInstallableIntent intent) {
            if (fail) {
                throw new IntentException("install failed by design");
            }
            return null;
        }

        @Override
        public List<FlowRuleBatchOperation> uninstall(TestInstallableIntent intent) {
            if (fail) {
                throw new IntentException("remove failed by design");
            }
            return null;
        }

        @Override
        public List<FlowRuleBatchOperation> replace(TestInstallableIntent intent,
                                                    TestInstallableIntent newIntent) {
            return null;
        }
    }

}
