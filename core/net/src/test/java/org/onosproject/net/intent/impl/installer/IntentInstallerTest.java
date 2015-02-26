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
package org.onosproject.net.intent.impl.installer;

import org.junit.After;
import org.junit.Before;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.MockIdGenerator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Base class for intent installer tests.
 */
public class IntentInstallerTest {

    /**
     * Mock for core service.
     */
    static class TestCoreService extends CoreServiceAdapter {

        String registeredId = "";

        @Override
        public ApplicationId registerApplication(String identifier) {
            registeredId = identifier;
            return APP_ID;
        }
    }

    /**
     * Mock for intent manager service. Checks that the PathIntent
     * installer installs and uninstalls properly.
     */
    static class MockIntentManager extends FakeIntentManager {

        boolean installerRegistered = false;
        final Class expectedClass;

        private MockIntentManager() {
            expectedClass = null;
        }

        MockIntentManager(Class expectedInstaller) {
            this.expectedClass = expectedInstaller;
        }

        @Override
        public <T extends Intent> void registerInstaller(
                Class<T> cls,
                IntentInstaller<T> installer) {
            assertThat(cls, equalTo(expectedClass));
            installerRegistered = true;
        }

        @Override
        public <T extends Intent> void unregisterInstaller(Class<T> cls) {
            assertThat(cls, equalTo(expectedClass));
            assertThat(installerRegistered, is(true));
        }

    }

    CoreService testCoreService;
    IdGenerator idGenerator = new MockIdGenerator();
    IntentInstaller installer;

    final IntentTestsMocks.MockSelector selector = new IntentTestsMocks.MockSelector();
    final IntentTestsMocks.MockTreatment treatment = new IntentTestsMocks.MockTreatment();
    final ConnectPoint d1p1 = connectPoint("s1", 0);
    final ConnectPoint d2p0 = connectPoint("s2", 0);
    final ConnectPoint d2p1 = connectPoint("s2", 1);
    final ConnectPoint d3p1 = connectPoint("s3", 1);
    final ConnectPoint d3p0 = connectPoint("s3", 10);
    final ConnectPoint d1p0 = connectPoint("s1", 10);

    /**
     * Configures objects used in all the test cases.
     */
    @Before
    public void setUp() {
        testCoreService = new TestCoreService();
        Intent.bindIdGenerator(idGenerator);
    }

    /**
     * Tears down objects used in all the test cases.
     */
    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Checks that a flow operation contains the correct values.
     *
     * @param op flow rule operation to check
     * @param type type the flow rule operation should have
     * @param deviceId device id the flow rule operation should have
     */
    void checkFlowOperation(FlowRuleOperation op,
                                    FlowRuleOperation.Type type,
                                    DeviceId deviceId) {
        assertThat(op.type(), is(type));
        assertThat(op.rule().deviceId(), equalTo(deviceId));
    }

}
