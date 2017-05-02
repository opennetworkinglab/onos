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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleServiceAdapter;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentInstallationContext;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.store.service.WallClockTimestamp;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Tests for flow rule Intent installer.
 */
public class FlowRuleIntentInstallerTest extends AbstractIntentInstallerTest {

    private TestFlowRuleService flowRuleService;
    private FlowRuleIntentInstaller installer;

    @Before
    public void setup() {
        super.setup();
        flowRuleService = new TestFlowRuleService();
        installer = new FlowRuleIntentInstaller();
        installer.flowRuleService = flowRuleService;
        installer.intentExtensionService = intentExtensionService;
        installer.intentInstallCoordinator = intentInstallCoordinator;
        installer.trackerService = trackerService;

        installer.activate();
    }

    @After
    public void tearDown() {
        super.tearDown();
        installer.deactivated();
    }

    /**
     * Installs Intents only, no Intents to be uninstall.
     */
    @Test
    public void testInstallOnly() {
        List<Intent> intentsToUninstall = Lists.newArrayList();
        List<Intent> intentsToInstall = createFlowRuleIntents();

        IntentData toUninstall = null;
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);


        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        Set<FlowRule> expectedFlowRules = intentsToInstall.stream()
                .map(intent -> (FlowRuleIntent) intent)
                .map(FlowRuleIntent::flowRules)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        assertEquals(expectedFlowRules, flowRuleService.flowRulesAdd);
    }

    /**
     * Uninstalls Intents only, no Intents to be install.
     */
    @Test
    public void testUninstallOnly() {
        List<Intent> intentsToInstall = Lists.newArrayList();
        List<Intent> intentsToUninstall = createFlowRuleIntents();

        IntentData toInstall = null;
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                              IntentState.WITHDRAWING,
                                              new WallClockTimestamp());
        toUninstall = new IntentData(toUninstall, intentsToUninstall);


        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        Set<FlowRule> expectedFlowRules = intentsToUninstall.stream()
                .map(intent -> (FlowRuleIntent) intent)
                .map(FlowRuleIntent::flowRules)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        assertEquals(expectedFlowRules, flowRuleService.flowRulesRemove);
    }

    /**
     * Do both install and uninstall Intents with different flow rules.
     */
    @Test
    public void testUninstallAndInstall() {
        List<Intent> intentsToInstall = createAnotherFlowRuleIntents();
        List<Intent> intentsToUninstall = createFlowRuleIntents();

        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = new IntentData(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        Set<FlowRule> expectedFlowRules = intentsToUninstall.stream()
                .map(intent -> (FlowRuleIntent) intent)
                .map(FlowRuleIntent::flowRules)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        assertEquals(expectedFlowRules, flowRuleService.flowRulesRemove);

        expectedFlowRules = intentsToInstall.stream()
                .map(intent -> (FlowRuleIntent) intent)
                .map(FlowRuleIntent::flowRules)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        assertEquals(expectedFlowRules, flowRuleService.flowRulesAdd);
    }

    /**
     * Do both install and uninstall Intents with same flow rules.
     */
    @Test
    public void testUninstallAndInstallUnchanged() {
        List<Intent> intentsToInstall = createFlowRuleIntents();
        List<Intent> intentsToUninstall = createFlowRuleIntents();

        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = new IntentData(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        assertEquals(0, flowRuleService.flowRulesRemove.size());
        assertEquals(0, flowRuleService.flowRulesAdd.size());
    }

    /**
     * Do both install and uninstall Intents with same flow rule Intent.
     */
    @Test
    public void testUninstallAndInstallSame() {
        List<Intent> intentsToInstall = createFlowRuleIntents();
        List<Intent> intentsToUninstall = intentsToInstall;

        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = new IntentData(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        assertEquals(0, flowRuleService.flowRulesRemove.size());
        assertEquals(0, flowRuleService.flowRulesAdd.size());
    }

    /**
     * Nothing to uninstall or install.
     */
    @Test
    public void testNoAnyIntentToApply() {
        IntentData toInstall = null;
        IntentData toUninstall = null;
        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext<>(ImmutableList.of(), ImmutableList.of(), context);
        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        assertEquals(0, flowRuleService.flowRulesRemove.size());
        assertEquals(0, flowRuleService.flowRulesAdd.size());
    }

    /**
     * Test if the flow installation failed.
     */
    @Test
    public void testFailed() {
        installer.flowRuleService = new TestFailedFlowRuleService();
        List<Intent> intentsToUninstall = Lists.newArrayList();
        List<Intent> intentsToInstall = createFlowRuleIntents();

        IntentData toUninstall = null;
        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = new IntentData(toInstall, intentsToInstall);


        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext failedContext = intentInstallCoordinator.failedContext;
        assertEquals(failedContext, operationContext);
    }

    /**
     * Generates FlowRuleIntents for test.
     *
     * @return the FlowRuleIntents for test
     */
    public List<Intent> createFlowRuleIntents() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPhyPort(CP1.port())
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(CP2.port())
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(CP1.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(APP_ID)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .build();

        List<NetworkResource> resources = ImmutableList.of(CP1.deviceId());

        FlowRuleIntent intent = new FlowRuleIntent(APP_ID,
                                                   KEY1,
                                                   ImmutableList.of(flowRule),
                                                   resources,
                                                   PathIntent.ProtectionType.PRIMARY,
                                                   RG1);

        List<Intent> flowRuleIntents = Lists.newArrayList();
        flowRuleIntents.add(intent);

        return flowRuleIntents;
    }

    /**
     * Generates another different FlowRuleIntents for test.
     *
     * @return the FlowRuleIntents for test
     */
    public List<Intent> createAnotherFlowRuleIntents() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.vlanId("100"))
                .matchInPhyPort(CP1.port())
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(CP2.port())
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(CP1.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(APP_ID)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .build();

        List<NetworkResource> resources = ImmutableList.of(CP1.deviceId());

        FlowRuleIntent intent = new FlowRuleIntent(APP_ID,
                                                   KEY1,
                                                   ImmutableList.of(flowRule),
                                                   resources,
                                                   PathIntent.ProtectionType.PRIMARY,
                                                   RG1);

        List<Intent> flowRuleIntents = Lists.newArrayList();
        flowRuleIntents.add(intent);

        return flowRuleIntents;
    }

    /**
     * The FlowRuleService for test; always success for any flow rule operations.
     */
    class TestFlowRuleService extends FlowRuleServiceAdapter {

        Set<FlowRule> flowRulesAdd = Sets.newHashSet();
        Set<FlowRule> flowRulesRemove = Sets.newHashSet();

        public void record(FlowRuleOperations ops) {
            flowRulesAdd.clear();
            flowRulesRemove.clear();
            ops.stages().forEach(stage -> {
                stage.forEach(op -> {
                    switch (op.type()) {
                        case ADD:
                            flowRulesAdd.add(op.rule());
                            break;
                        case REMOVE:
                            flowRulesRemove.add(op.rule());
                            break;
                        default:
                            break;
                    }
                });
            });
        }

        @Override
        public void apply(FlowRuleOperations ops) {
            record(ops);
            ops.callback().onSuccess(ops);
        }
    }

    /**
     * The FlowRuleService for test; always failed for any flow rule operations.
     */
    class TestFailedFlowRuleService extends TestFlowRuleService {
        @Override
        public void apply(FlowRuleOperations ops) {
            record(ops);
            ops.callback().onError(ops);
        }
    }

}
