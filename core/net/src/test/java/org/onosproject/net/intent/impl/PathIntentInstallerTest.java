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
package org.onosproject.net.intent.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.intent.FakeIntentManager;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.PathIntent;

import com.google.common.collect.ImmutableList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.PID;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Unit tests for path intent installer.
 */
public class PathIntentInstallerTest {

    CoreService testCoreService;
    IdGenerator idGenerator = new MockIdGenerator();
    PathIntentInstaller installer;

    private final IntentTestsMocks.MockSelector selector = new IntentTestsMocks.MockSelector();
    private final IntentTestsMocks.MockTreatment treatment = new IntentTestsMocks.MockTreatment();
    private final ConnectPoint d1p1 = connectPoint("s1", 0);
    private final ConnectPoint d2p0 = connectPoint("s2", 0);
    private final ConnectPoint d2p1 = connectPoint("s2", 1);
    private final ConnectPoint d3p1 = connectPoint("s3", 1);
    private final ConnectPoint d3p0 = connectPoint("s3", 10);
    private final ConnectPoint d1p0 = connectPoint("s1", 10);

    private final List<Link> links = Arrays.asList(
            createEdgeLink(d1p0, true),
            new DefaultLink(PID, d1p1, d2p0, DIRECT),
            new DefaultLink(PID, d2p1, d3p1, DIRECT),
            createEdgeLink(d3p0, false)
    );
    private final int hops = links.size() - 1;
    private PathIntent intent;

    /**
     * Configures objects used in all the test cases.
     */
    @Before
    public void localSetup() {
        testCoreService = new TestCoreService();
        Intent.bindIdGenerator(idGenerator);
        installer = new PathIntentInstaller();
        installer.coreService = testCoreService;
        installer.intentManager = new MockIntentManager();
        intent = new PathIntent(APP_ID, selector, treatment,
                new DefaultPath(PID, links, hops), ImmutableList.of());
    }

    /**
     * Tears down objects used in all the test cases.
     */
    @After
    public void localTearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Mock for core service.
     */
    private static class TestCoreService extends CoreServiceAdapter {

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
    private static class MockIntentManager extends FakeIntentManager {

        boolean installerRegistered = false;

        @Override
        public <T extends Intent> void registerInstaller(
                Class<T> cls,
                IntentInstaller<T> installer) {
            assertThat(cls.getCanonicalName(),
                    equalTo("org.onosproject.net.intent.PathIntent"));
            installerRegistered = true;
        }

        @Override
        public <T extends Intent> void unregisterInstaller(Class<T> cls) {
            assertThat(cls.getCanonicalName(),
                    equalTo("org.onosproject.net.intent.PathIntent"));
            assertThat(installerRegistered, is(true));
        }

    }

    /**
     * Tests activation and deactivation of the installer.
     */
    @Test
    public void activateDeactivate() {
        installer.activate();
        installer.deactivate();
    }

    /**
     * Checks that a flow operation contains the correct values.
     *
     * @param op flow rule operation to check
     * @param type type the flow rule operation should have
     * @param deviceId device id the flow rule operation should have
     */
    private void checkFlowOperation(FlowRuleOperation op,
                                    FlowRuleOperation.Type type,
                                    DeviceId deviceId) {
        assertThat(op.type(), is(type));
        assertThat(op.rule().deviceId(), equalTo(deviceId));
    }

    /**
     * Tests installation operation of the path intent installer.
     */
    @Test
    public void install() {
        installer.activate();

        List<Collection<FlowRuleOperation>> operations =
            installer.install(intent);
        assertThat(operations, notNullValue());
        assertThat(operations, hasSize(1));

        Collection<FlowRuleOperation> flowRuleOpsCollection = operations.get(0);
        assertThat(flowRuleOpsCollection, hasSize(hops));
        FlowRuleOperation[] flowRuleOps =
                flowRuleOpsCollection.toArray(new FlowRuleOperation[hops]);

        FlowRuleOperation op0 = flowRuleOps[0];
        checkFlowOperation(op0, FlowRuleOperation.Type.ADD, d1p0.deviceId());

        FlowRuleOperation op1 = flowRuleOps[1];
        checkFlowOperation(op1, FlowRuleOperation.Type.ADD, d2p0.deviceId());

        FlowRuleOperation op2 = flowRuleOps[2];
        checkFlowOperation(op2, FlowRuleOperation.Type.ADD, d3p0.deviceId());

        installer.deactivate();
    }

    /**
     * Checks the uninstall operation of the path intent installer.
     */
    @Test
    public void uninstall() {
        installer.activate();

        List<Collection<FlowRuleOperation>> operations =
                installer.uninstall(intent);
        assertThat(operations, notNullValue());
        assertThat(operations, hasSize(1));

        Collection<FlowRuleOperation> flowRuleOpsCollection = operations.get(0);
        assertThat(flowRuleOpsCollection, hasSize(hops));
        FlowRuleOperation[] flowRuleOps =
                flowRuleOpsCollection.toArray(new FlowRuleOperation[hops]);

        FlowRuleOperation op0 = flowRuleOps[0];
        checkFlowOperation(op0, FlowRuleOperation.Type.REMOVE, d1p0.deviceId());

        FlowRuleOperation op1 = flowRuleOps[1];
        checkFlowOperation(op1, FlowRuleOperation.Type.REMOVE, d2p0.deviceId());

        FlowRuleOperation op2 = flowRuleOps[2];
        checkFlowOperation(op2, FlowRuleOperation.Type.REMOVE, d3p0.deviceId());

        installer.deactivate();
    }
}
