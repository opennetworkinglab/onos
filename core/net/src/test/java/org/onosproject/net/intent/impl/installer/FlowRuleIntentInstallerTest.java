/*
 * Copyright 2017-present Open Networking Foundation
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperation;
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
import org.onosproject.store.trivial.SimpleIntentStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.mock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.flow.FlowRuleOperation.Type.ADD;
import static org.onosproject.net.flow.FlowRuleOperation.Type.REMOVE;

/**
 * Tests for flow rule Intent installer.
 */
public class FlowRuleIntentInstallerTest extends AbstractIntentInstallerTest {

    private TestFlowRuleService flowRuleService;
    private final TestFlowRuleServiceNonDisruptive flowRuleServiceNonDisruptive =
            new TestFlowRuleServiceNonDisruptive();
    private FlowRuleIntentInstaller installer;

    @Before
    public void setup() {
        super.setup();
        flowRuleService = new TestFlowRuleService();
        installer = new FlowRuleIntentInstaller();
        installer.flowRuleService = flowRuleService;
        installer.store = new SimpleIntentStore();
        installer.intentExtensionService = intentExtensionService;
        installer.intentInstallCoordinator = intentInstallCoordinator;
        installer.trackerService = trackerService;
        installer.configService = mock(ComponentConfigService.class);

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
        toInstall = IntentData.compiled(toInstall, intentsToInstall);


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
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);


        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        flowRuleService.load(operationContext.intentsToUninstall());

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
     * Uninstalls Intents only, no Intents to be install.  However, the flow rules do not exist
     * in the FlowRuleService.
     */
    @Test
    public void testUninstallOnlyMissing() {
        List<Intent> intentsToInstall = Lists.newArrayList();
        List<Intent> intentsToUninstall = createFlowRuleIntents();

        IntentData toInstall = null;
        IntentData toUninstall = new IntentData(createP2PIntent(),
                IntentState.WITHDRAWING,
                new WallClockTimestamp());
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);


        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        assertEquals(0, flowRuleService.flowRulesRemove.size());
        assertEquals(0, flowRuleService.flowRulesAdd.size());
        assertEquals(0, flowRuleService.flowRulesModify.size());
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
        toInstall = IntentData.compiled(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        flowRuleService.load(operationContext.intentsToUninstall());

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
     * Do both install and uninstall Intents with different flow rules.  However, the flow rules do not exist
     * in the FlowRuleService.
     */
    @Test
    public void testUninstallAndInstallMissing() {
        List<Intent> intentsToInstall = createAnotherFlowRuleIntents();
        List<Intent> intentsToUninstall = createFlowRuleIntents();

        IntentData toInstall = new IntentData(createP2PIntent(),
                IntentState.INSTALLING,
                new WallClockTimestamp());
        toInstall = IntentData.compiled(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                IntentState.INSTALLED,
                new WallClockTimestamp());
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        Set<FlowRule> expectedFlowRules = Sets.newHashSet();

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
        toInstall = IntentData.compiled(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        flowRuleService.load(operationContext.intentsToUninstall());

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        assertEquals(0, flowRuleService.flowRulesRemove.size());
        assertEquals(0, flowRuleService.flowRulesAdd.size());
        assertEquals(0, flowRuleService.flowRulesModify.size());
    }

    /**
     * Do both install and uninstall Intents with same flow rules.  However, the flow rules do not exist
     * in the FlowRuleService.
     */
    @Test
    public void testUninstallAndInstallUnchangedMissing() {
        List<Intent> intentsToInstall = createFlowRuleIntents();
        List<Intent> intentsToUninstall = createFlowRuleIntents();

        IntentData toInstall = new IntentData(createP2PIntent(),
                IntentState.INSTALLING,
                new WallClockTimestamp());
        toInstall = IntentData.compiled(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                IntentState.INSTALLED,
                new WallClockTimestamp());
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        assertEquals(0, flowRuleService.flowRulesRemove.size());
        assertEquals(1, flowRuleService.flowRulesAdd.size());
        assertEquals(0, flowRuleService.flowRulesModify.size());
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
        toInstall = IntentData.compiled(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        flowRuleService.load(operationContext.intentsToUninstall());

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        assertEquals(0, flowRuleService.flowRulesRemove.size());
        assertEquals(0, flowRuleService.flowRulesAdd.size());
        assertEquals(0, flowRuleService.flowRulesModify.size());
    }

    /**
     * Do both install and uninstall Intents with same flow rule Intent. However, the flow rules do not exist
     * in the FlowRuleService.
     */
    @Test
    public void testUninstallAndInstallSameMissing() {
        List<Intent> intentsToInstall = createFlowRuleIntents();
        List<Intent> intentsToUninstall = intentsToInstall;

        IntentData toInstall = new IntentData(createP2PIntent(),
                IntentState.INSTALLING,
                new WallClockTimestamp());
        toInstall = IntentData.compiled(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                IntentState.INSTALLED,
                new WallClockTimestamp());
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        assertEquals(0, flowRuleService.flowRulesRemove.size());
        assertEquals(1, flowRuleService.flowRulesAdd.size());
        assertEquals(0, flowRuleService.flowRulesModify.size());
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
        assertEquals(0, flowRuleService.flowRulesModify.size());
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
        toInstall = IntentData.compiled(toInstall, intentsToInstall);


        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext failedContext = intentInstallCoordinator.failedContext;
        assertEquals(failedContext, operationContext);
    }

    /**
     * Test intents with same match rules, should do modify instead of add.
     */
    @Test
    public void testRuleModify() {
        List<Intent> intentsToInstall = createFlowRuleIntents();
        List<Intent> intentsToUninstall = createFlowRuleIntentsWithSameMatch();

        IntentData toInstall = new IntentData(createP2PIntent(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = IntentData.compiled(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        flowRuleService.load(operationContext.intentsToUninstall());

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        assertEquals(0, flowRuleService.flowRulesRemove.size());
        assertEquals(0, flowRuleService.flowRulesAdd.size());
        assertEquals(1, flowRuleService.flowRulesModify.size());

        FlowRuleIntent installedIntent = (FlowRuleIntent) intentsToInstall.get(0);
        assertEquals(flowRuleService.flowRulesModify.size(), installedIntent.flowRules().size());
        assertTrue(flowRuleService.flowRulesModify.containsAll(installedIntent.flowRules()));
    }

    /**
     * Test intents with same match rules, should do modify instead of add.  However, the flow rules do not exist
     * in the FlowRuleService.
     */
    @Test
    public void testRuleModifyMissing() {
        List<Intent> intentsToInstall = createFlowRuleIntents();
        List<Intent> intentsToUninstall = createFlowRuleIntentsWithSameMatch();

        IntentData toInstall = new IntentData(createP2PIntent(),
                IntentState.INSTALLING,
                new WallClockTimestamp());
        toInstall = IntentData.compiled(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntent(),
                IntentState.INSTALLED,
                new WallClockTimestamp());
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);

        assertEquals(0, flowRuleService.flowRulesRemove.size());
        assertEquals(1, flowRuleService.flowRulesAdd.size());
        assertEquals(0, flowRuleService.flowRulesModify.size());

        FlowRuleIntent installedIntent = (FlowRuleIntent) intentsToInstall.get(0);
        assertEquals(flowRuleService.flowRulesAdd.size(), installedIntent.flowRules().size());
        assertTrue(flowRuleService.flowRulesAdd.containsAll(installedIntent.flowRules()));
    }

    /**
     * Testing the non-disruptive reallocation.
     */
    @Test
    public void testUninstallAndInstallNonDisruptive() throws InterruptedException {

        installer.flowRuleService = flowRuleServiceNonDisruptive;

        List<Intent> intentsToInstall = createAnotherFlowRuleIntentsNonDisruptive();
        List<Intent> intentsToUninstall = createFlowRuleIntentsNonDisruptive();

        IntentData toInstall = new IntentData(createP2PIntentNonDisruptive(),
                                              IntentState.INSTALLING,
                                              new WallClockTimestamp());
        toInstall = IntentData.compiled(toInstall, intentsToInstall);
        IntentData toUninstall = new IntentData(createP2PIntentNonDisruptive(),
                                                IntentState.INSTALLED,
                                                new WallClockTimestamp());
        toUninstall = IntentData.compiled(toUninstall, intentsToUninstall);

        IntentOperationContext<FlowRuleIntent> operationContext;
        IntentInstallationContext context = new IntentInstallationContext(toUninstall, toInstall);
        operationContext = new IntentOperationContext(intentsToUninstall, intentsToInstall, context);

        installer.apply(operationContext);

        //A single FlowRule is evaluated for every non-disruptive stage
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPhyPort(CP1.port())
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(CP3.port())
                .build();

        FlowRule firstStageInstalledRule = DefaultFlowRule.builder()
                .forDevice(CP1.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(APP_ID)
                .withPriority(DEFAULT_PRIORITY - 1)
                .makePermanent()
                .build();

        // We need to wait a bit in order to avoid
        // race conditions and failing builds
        synchronized (flowRuleServiceNonDisruptive) {
            while (!verifyFlowRule(ADD, firstStageInstalledRule)) {
                flowRuleServiceNonDisruptive.wait();
            }
        }

        assertTrue(flowRuleServiceNonDisruptive.flowRulesAdd.contains(firstStageInstalledRule));

        selector = DefaultTrafficSelector.builder()
                .matchInPhyPort(CP4_2.port())
                .build();
        treatment = DefaultTrafficTreatment.builder()
                .setOutput(CP4_1.port())
                .build();

        FlowRule secondStageUninstalledRule = DefaultFlowRule.builder()
                .forDevice(CP4_1.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(APP_ID)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .build();

        synchronized (flowRuleServiceNonDisruptive) {
            while (!verifyFlowRule(REMOVE, secondStageUninstalledRule)) {
                flowRuleServiceNonDisruptive.wait();
            }
        }

        assertTrue(flowRuleServiceNonDisruptive.flowRulesRemove.contains(secondStageUninstalledRule));

        selector = DefaultTrafficSelector.builder()
                .matchInPhyPort(CP4_3.port())
                .build();
        treatment = DefaultTrafficTreatment.builder()
                .setOutput(CP4_1.port())
                .build();

        FlowRule thirdStageInstalledRule = DefaultFlowRule.builder()
                .forDevice(CP4_1.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(APP_ID)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .build();

        synchronized (flowRuleServiceNonDisruptive) {
            while (!verifyFlowRule(ADD, thirdStageInstalledRule)) {
                flowRuleServiceNonDisruptive.wait();
            }
        }

        assertTrue(flowRuleServiceNonDisruptive.flowRulesAdd.contains(thirdStageInstalledRule));

        selector = DefaultTrafficSelector.builder()
                .matchInPhyPort(CP2_1.port())
                .build();
        treatment = DefaultTrafficTreatment.builder()
                .setOutput(CP2_2.port())
                .build();

        FlowRule lastStageUninstalledRule = DefaultFlowRule.builder()
                .forDevice(CP2_1.deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                .fromApp(APP_ID)
                .withPriority(DEFAULT_PRIORITY)
                .makePermanent()
                .build();

        synchronized (flowRuleServiceNonDisruptive) {
            while (!verifyFlowRule(REMOVE, lastStageUninstalledRule)) {
                flowRuleServiceNonDisruptive.wait();
            }
        }

        assertTrue(flowRuleServiceNonDisruptive.flowRulesRemove.contains(lastStageUninstalledRule));

        IntentOperationContext successContext = intentInstallCoordinator.successContext;
        assertEquals(successContext, operationContext);
    }

    private boolean verifyFlowRule(FlowRuleOperation.Type type, FlowRule flowRule) {
        return type == ADD ? flowRuleServiceNonDisruptive.flowRulesAdd.contains(flowRule) :
                flowRuleServiceNonDisruptive.flowRulesRemove.contains(flowRule);
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
     * Generates FlowRuleIntents for test. Flow rules in Intent should have same
     * match as we created by createFlowRuleIntents method, but action will be
     * different.
     *
     * @return the FlowRuleIntents for test
     */
    public List<Intent> createFlowRuleIntentsWithSameMatch() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPhyPort(CP1.port())
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .punt()
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
     * Generates FlowRuleIntents for testing non-disruptive reallocation.
     *
     * @return the FlowRuleIntents for test
     */
    public List<Intent> createFlowRuleIntentsNonDisruptive() {

        Map<ConnectPoint, ConnectPoint> portsAssociation = Maps.newHashMap();
        portsAssociation.put(CP1, CP2);
        portsAssociation.put(CP2_1, CP2_2);
        portsAssociation.put(CP4_2, CP4_1);

        List<FlowRule> flowRules = Lists.newArrayList();

        for (ConnectPoint srcPoint : portsAssociation.keySet()) {

            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPhyPort(srcPoint.port())
                    .build();
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(portsAssociation.get(srcPoint).port())
                    .build();

            FlowRule flowRule = DefaultFlowRule.builder()
                    .forDevice(srcPoint.deviceId())
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .fromApp(APP_ID)
                    .withPriority(DEFAULT_PRIORITY)
                    .makePermanent()
                    .build();
            flowRules.add(flowRule);
        }



        List<NetworkResource> resources = ImmutableList.of(S1_S2, S2_S4);

        FlowRuleIntent intent = new FlowRuleIntent(APP_ID,
                                                   KEY1,
                                                   flowRules,
                                                   resources,
                                                   PathIntent.ProtectionType.PRIMARY,
                                                   RG1);

        List<Intent> flowRuleIntents = Lists.newArrayList();
        flowRuleIntents.add(intent);

        return flowRuleIntents;
    }

    /**
     * Generates another FlowRuleIntent, going through a different path, for testing non-disruptive reallocation.
     *
     * @return the FlowRuleIntents for test
     */
    public List<Intent> createAnotherFlowRuleIntentsNonDisruptive() {
        Map<ConnectPoint, ConnectPoint> portsAssociation = Maps.newHashMap();
        portsAssociation.put(CP1, CP3);
        portsAssociation.put(CP3_1, CP3_2);
        portsAssociation.put(CP4_3, CP4_1);

        List<FlowRule> flowRules = Lists.newArrayList();

        for (ConnectPoint srcPoint : portsAssociation.keySet()) {

            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchInPhyPort(srcPoint.port())
                    .build();
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(portsAssociation.get(srcPoint).port())
                    .build();

            FlowRule flowRule = DefaultFlowRule.builder()
                    .forDevice(srcPoint.deviceId())
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .fromApp(APP_ID)
                    .withPriority(DEFAULT_PRIORITY)
                    .makePermanent()
                    .build();
            flowRules.add(flowRule);
        }



        List<NetworkResource> resources = ImmutableList.of(S1_S3, S3_S4);

        FlowRuleIntent intent = new FlowRuleIntent(APP_ID,
                                                   KEY1,
                                                   flowRules,
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

        Set<FlowEntry> flowEntries = Sets.newHashSet();
        Set<FlowRule> flowRulesAdd = Sets.newHashSet();
        Set<FlowRule> flowRulesModify = Sets.newHashSet();
        Set<FlowRule> flowRulesRemove = Sets.newHashSet();

        @Override
        public FlowEntry getFlowEntry(FlowRule flowRule) {
            for (FlowEntry entry : flowEntries) {
                if (entry.id().equals(flowRule.id()) && entry.deviceId().equals(flowRule.deviceId())) {
                    return entry;
                }
            }
            return null;
        }

        @Override
        public Iterable<FlowEntry> getFlowEntries(DeviceId deviceId) {
            return flowEntries.stream()
                    .filter(flow -> flow.deviceId().equals(deviceId))
                    .collect(Collectors.toList());
        }

        public void load(List<FlowRuleIntent> intents) {
            for (FlowRuleIntent flowRuleIntent : intents) {
                for (FlowRule flowRule : flowRuleIntent.flowRules()) {
                    flowEntries.add(new DefaultFlowEntry(flowRule, FlowEntry.FlowEntryState.ADDED));
                }
            }
        }

        public void record(FlowRuleOperations ops) {
            flowRulesAdd.clear();
            flowRulesRemove.clear();
            ops.stages().forEach(stage -> {
                stage.forEach(op -> {
                    switch (op.type()) {
                        case ADD:
                            flowEntries.add(new DefaultFlowEntry(op.rule(), FlowEntry.FlowEntryState.ADDED));
                            flowRulesAdd.add(op.rule());
                            break;
                        case REMOVE:
                            flowEntries.remove(new DefaultFlowEntry(op.rule(), FlowEntry.FlowEntryState.ADDED));
                            flowRulesRemove.add(op.rule());
                            break;
                        case MODIFY:
                            flowEntries.add(new DefaultFlowEntry(op.rule(), FlowEntry.FlowEntryState.ADDED));
                            flowRulesModify.add(op.rule());
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

    /**
     * The FlowRuleService for testing non-disruptive reallocation.
     * It keeps all the FlowRules installed/uninstalled.
     */
    class TestFlowRuleServiceNonDisruptive extends FlowRuleServiceAdapter {

        Set<FlowRule> flowRulesAdd = Sets.newHashSet();
        Set<FlowRule> flowRulesRemove = Sets.newHashSet();

        public void record(FlowRuleOperations ops) {
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
            synchronized (this) {
                this.notify();
            }
        }
    }

}
