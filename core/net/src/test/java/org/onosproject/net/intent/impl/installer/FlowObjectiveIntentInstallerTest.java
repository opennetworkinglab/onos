/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveServiceAdapter;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.intent.FlowObjectiveIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentInstallationContext;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentState;
import org.onosproject.store.service.WallClockTimestamp;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onosproject.net.flowobjective.ObjectiveError.*;

/**
 * Tests for flow objective Intent installer.
 */
public class FlowObjectiveIntentInstallerTest extends AbstractIntentInstallerTest {
    private static final int NEXT_ID_1 = 1;
    protected FlowObjectiveIntentInstaller installer;
    protected TestFlowObjectiveService flowObjectiveService;

    @Before
    public void setup() {
        super.setup();
        flowObjectiveService = new TestFlowObjectiveService();
        installer = new FlowObjectiveIntentInstaller();
        installer.flowObjectiveService = flowObjectiveService;
        installer.trackerService = trackerService;
        installer.intentExtensionService = intentExtensionService;
        installer.intentInstallCoordinator = intentInstallCoordinator;

        installer.activate();
    }

    @After
    public void tearDown() {
        super.tearDown();
        installer.deactivated();
    }

    /**
     * Installs flow objective Intents.
     */
    @Test
    public void testInstallIntent() {
        List<Intent> intentsToUninstall = Lists.newArrayList();
        List<Intent> intentsToInstall = createFlowObjectiveIntents();

        IntentData toUninstall = null;
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);


        IntentOperationContext<FlowObjectiveIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);
    }

    /**
     * Uninstalls flow objective Intents.
     */
    @Test
    public void testUninstallIntent() {
        List<Intent> intentsToUninstall = createFlowObjectiveIntents();
        List<Intent> intentsToInstall = Lists.newArrayList();


        IntentData toInstall = null;
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                              IntentState.WITHDRAWING,
                                              new WallClockTimestamp());
        toUninstall = new IntentData(toUninstall, intentsToUninstall);
        IntentOperationContext<FlowObjectiveIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);
    }

    /**
     * Do both uninstall and install flow objective Intents.
     */
    @Test
    public void testUninstallAndInstallIntent() {
        List<Intent> intentsToUninstall = createFlowObjectiveIntents();
        List<Intent> intentsToInstall = createAnotherFlowObjectiveIntents();
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = new IntentData(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowObjectiveIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);
    }

    /**
     * Nothing to uninstall or install.
     */
    @Test
    public void testNoAnyIntentToApply() {
        IntentData toInstall = null;
        IntentData toUninstall = null;
        IntentOperationContext<FlowObjectiveIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext<>(ImmutableList.of(), ImmutableList.of(), context);
        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);
    }

    /*
     * Error handling
     */
    IntentOperationContext context;
    IntentOperationContext failedContext;
    IntentOperationContext successContext;
    List<ObjectiveError> errors;

    /**
     * Handles UNSUPPORTED error.
     */
    @Test
    public void testUnsupportedError() {
        // Unsupported, should just failed
        intentInstallCoordinator = new TestIntentInstallCoordinator();
        installer.intentInstallCoordinator = intentInstallCoordinator;
        installer.flowObjectiveService = new TestFailedFlowObjectiveService();
        context = createInstallContext();
        installer.apply(context);
        assertEquals(intentInstallCoordinator.failedContext, context);
    }

    /**
     * Handles FLOWINSTALLATIONFAILED error with touch the threshold.
     */
    @Test
    public void testFlowInstallationFailedError() {
        // flow install failed, should retry until retry threshold
        intentInstallCoordinator = new TestIntentInstallCoordinator();
        installer.intentInstallCoordinator = intentInstallCoordinator;
        errors = ImmutableList.of(FLOWINSTALLATIONFAILED, FLOWINSTALLATIONFAILED,
                                  FLOWINSTALLATIONFAILED, FLOWINSTALLATIONFAILED,
                                  FLOWINSTALLATIONFAILED, FLOWINSTALLATIONFAILED,
                                  FLOWINSTALLATIONFAILED);
        installer.flowObjectiveService = new TestFailedFlowObjectiveService(errors);
        context = createInstallContext();
        installer.apply(context);
        failedContext = intentInstallCoordinator.failedContext;
        assertEquals(failedContext, context);
    }

    /**
     * Handles FLOWINSTALLATIONFAILED error without touch the threshold.
     */
    @Test
    public void testFlowInstallationFailedErrorUnderThreshold() {
        // And retry two times and success
        intentInstallCoordinator = new TestIntentInstallCoordinator();
        installer.intentInstallCoordinator = intentInstallCoordinator;
        errors = ImmutableList.of(FLOWINSTALLATIONFAILED, FLOWINSTALLATIONFAILED);
        installer.flowObjectiveService = new TestFailedFlowObjectiveService(errors);
        context = createInstallContext();
        installer.apply(context);
        successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, context);
    }

    /**
     * Handles GROUPINSTALLATIONFAILED error with touch the threshold.
     */
    @Test
    public void testGroupInstallationFailedError() {
        // Group install failed, and retry threshold exceed
        intentInstallCoordinator = new TestIntentInstallCoordinator();
        installer.intentInstallCoordinator = intentInstallCoordinator;
        errors = ImmutableList.of(GROUPINSTALLATIONFAILED, GROUPINSTALLATIONFAILED,
                                  GROUPINSTALLATIONFAILED, GROUPINSTALLATIONFAILED,
                                  GROUPINSTALLATIONFAILED, GROUPINSTALLATIONFAILED,
                                  GROUPINSTALLATIONFAILED);
        installer.flowObjectiveService = new TestFailedFlowObjectiveService(errors);
        context = createInstallContext();
        installer.apply(context);
        failedContext = intentInstallCoordinator.failedContext;
        assertEquals(failedContext, context);

    }

    /**
     * Handles GROUPINSTALLATIONFAILED error without touch the threshold.
     */
    @Test
    public void testGroupInstallationFailedErrorUnderThreshold() {
        // group install failed, and retry two times.
        intentInstallCoordinator = new TestIntentInstallCoordinator();
        installer.intentInstallCoordinator = intentInstallCoordinator;
        errors = ImmutableList.of(GROUPINSTALLATIONFAILED, GROUPINSTALLATIONFAILED);
        installer.flowObjectiveService = new TestFailedFlowObjectiveService(errors);
        context = createInstallContext();
        installer.apply(context);
        successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, context);

    }

    /**
     * Handles GROUPEXISTS error.
     */
    @Test
    public void testGroupExistError() {
        // group exists, retry by using add to exist
        intentInstallCoordinator = new TestIntentInstallCoordinator();
        installer.intentInstallCoordinator = intentInstallCoordinator;
        errors = ImmutableList.of(GROUPEXISTS);
        installer.flowObjectiveService = new TestFailedFlowObjectiveService(errors);
        context = createInstallContext();
        installer.apply(context);
        successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, context);
    }

    /**
     * Handles GROUPMISSING error with ADD_TO_EXIST operation.
     */
    @Test
    public void testGroupMissingError() {
        // group exist -> group missing -> add group
        intentInstallCoordinator = new TestIntentInstallCoordinator();
        installer.intentInstallCoordinator = intentInstallCoordinator;
        errors = ImmutableList.of(GROUPEXISTS, GROUPMISSING);
        installer.flowObjectiveService = new TestFailedFlowObjectiveService(errors);
        context = createInstallContext();
        installer.apply(context);
        successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, context);
    }

    /**
     * Handles GROUPMISSING error with ADD operation.
     */
    @Test
    public void testGroupChainElementMissingError() {
        // group chain element missing
        intentInstallCoordinator = new TestIntentInstallCoordinator();
        installer.intentInstallCoordinator = intentInstallCoordinator;
        errors = ImmutableList.of(GROUPMISSING);
        installer.flowObjectiveService = new TestFailedFlowObjectiveService(errors);
        context = createInstallContext();
        installer.apply(context);
        successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, context);
    }

    /**
     * Handles GROUPMISSING error with REMOVE operation.
     */
    @Test
    public void testGroupAlreadyRemoved() {
        // group already removed
        intentInstallCoordinator = new TestIntentInstallCoordinator();
        installer.intentInstallCoordinator = intentInstallCoordinator;
        errors = ImmutableList.of(GROUPMISSING);
        installer.flowObjectiveService = new TestFailedFlowObjectiveService(errors);
        context = createUninstallContext();
        installer.apply(context);
        successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, context);
    }

    /**
     * Creates Intent operation context for uninstall Intents.
     *
     * @return the context
     */
    private IntentOperationContext createUninstallContext() {
        List<Intent> intentsToUninstall = createFlowObjectiveIntents();
        List<Intent> intentsToInstall = Lists.newArrayList();
        IntentData toInstall = null;
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toUninstall = new IntentData(toUninstall, intentsToUninstall);
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        return new IntentOperationContext(intentsToUninstall, intentsToInstall, context);
    }

    /**
     * Creates Intent operation context for install Intents.
     *
     * @return the context
     */
    private IntentOperationContext createInstallContext() {
        List<Intent> intentsToUninstall = Lists.newArrayList();
        List<Intent> intentsToInstall = createFlowObjectiveIntents();
        IntentData toUninstall = null;
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        return new IntentOperationContext(intentsToUninstall, intentsToInstall, context);
    }

    /**
     * Creates flow objective Intents.
     *
     * @return the flow objective intents
     */
    private List<Intent> createFlowObjectiveIntents() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(CP1.port())
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(CP2.port())
                .build();

        FilteringObjective filt = DefaultFilteringObjective.builder()
                .addCondition(selector.getCriterion(Criterion.Type.IN_PORT))
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(APP_ID)
                .permit()
                .add();

        NextObjective next = DefaultNextObjective.builder()
                .withMeta(selector)
                .addTreatment(treatment)
                .makePermanent()
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(APP_ID)
                .withType(NextObjective.Type.SIMPLE)
                .withId(NEXT_ID_1)
                .add();

        ForwardingObjective fwd = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .fromApp(APP_ID)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .nextStep(NEXT_ID_1)
                .add();

        List<Objective> objectives = ImmutableList.of(filt, next, fwd);
        List<DeviceId> deviceIds = ImmutableList.of(CP1.deviceId(), CP1.deviceId(), CP1.deviceId());
        List<NetworkResource> resources = ImmutableList.of(CP1.deviceId());

        Intent intent = new FlowObjectiveIntent(APP_ID, KEY1, deviceIds, objectives, resources, RG1);
        return ImmutableList.of(intent);
    }

    /**
     * Creates flow objective Intents with different selector.
     *
     * @return the flow objective Intents
     */
    private List<Intent> createAnotherFlowObjectiveIntents() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.vlanId("100"))
                .matchInPort(CP1.port())
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(CP2.port())
                .build();

        FilteringObjective filt = DefaultFilteringObjective.builder()
                .addCondition(selector.getCriterion(Criterion.Type.IN_PORT))
                .addCondition(selector.getCriterion(Criterion.Type.VLAN_VID))
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(APP_ID)
                .permit()
                .add();

        NextObjective next = DefaultNextObjective.builder()
                .withMeta(selector)
                .addTreatment(treatment)
                .makePermanent()
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(APP_ID)
                .withType(NextObjective.Type.SIMPLE)
                .withId(NEXT_ID_1)
                .add();

        ForwardingObjective fwd = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .fromApp(APP_ID)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .nextStep(NEXT_ID_1)
                .add();

        List<Objective> objectives = ImmutableList.of(filt, next, fwd);
        List<DeviceId> deviceIds = ImmutableList.of(CP1.deviceId(), CP1.deviceId(), CP1.deviceId());
        List<NetworkResource> resources = ImmutableList.of(CP1.deviceId());

        Intent intent = new FlowObjectiveIntent(APP_ID, KEY1, deviceIds, objectives, resources, RG1);
        return ImmutableList.of(intent);
    }

    /**
     * Flow objective service for test; always successful for every flow objectives.
     */
    class TestFlowObjectiveService extends FlowObjectiveServiceAdapter {
        List<DeviceId> devices = Lists.newArrayList();
        List<Objective> objectives = Lists.newArrayList();

        @Override
        public void apply(DeviceId deviceId, Objective objective) {
            devices.add(deviceId);
            objectives.add(objective);
            objective.context().ifPresent(context -> context.onSuccess(objective));
        }
    }

    /**
     * Flow objective service for test; contains errors for every flow objective
     * submission.
     */
    class TestFailedFlowObjectiveService extends TestFlowObjectiveService {
        private final Set<ObjectiveError> groupErrors =
                ImmutableSet.of(GROUPEXISTS, GROUPINSTALLATIONFAILED,
                                GROUPMISSING, GROUPREMOVALFAILED);

        /**
         * Error states to test error handler by given error queue
         * e.g.
         * FLOWINSTALLATIONFAILED -> FLOWINSTALLATIONFAILED -> null: should be success
         * FLOWINSTALLATIONFAILED -> five same error -> ....       : should be failed
         */
        List<ObjectiveError> errors;

        public TestFailedFlowObjectiveService() {
            errors = Lists.newArrayList();
            errors.add(UNSUPPORTED);
        }

        public TestFailedFlowObjectiveService(List<ObjectiveError> errors) {
            this.errors = Lists.newArrayList(errors);
        }

        @Override
        public void apply(DeviceId deviceId, Objective objective) {
            if (errors.size() != 0) {
                if (groupErrors.contains(errors.get(0)) && objective instanceof NextObjective) {
                    ObjectiveError error = errors.remove(0);
                    objective.context().ifPresent(context -> context.onError(objective, error));
                    return;
                }
                if (!groupErrors.contains(errors.get(0)) && !(objective instanceof NextObjective)) {
                    ObjectiveError error = errors.remove(0);
                    objective.context().ifPresent(context -> context.onError(objective, error));
                    return;
                }
            }
            objective.context().ifPresent(context -> context.onSuccess(objective));

        }
    }
}
