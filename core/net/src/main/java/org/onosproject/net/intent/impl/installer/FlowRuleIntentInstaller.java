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

import com.google.common.collect.Lists;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentInstallCoordinator;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentStore;
import org.onosproject.net.intent.ObjectiveTrackerService;
import org.onosproject.net.intent.impl.IntentManager;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.OsgiPropertyConstants.NON_DISRUPTIVE_INSTALLATION_WAITING_TIME;
import static org.onosproject.net.OsgiPropertyConstants.NON_DISRUPTIVE_INSTALLATION_WAITING_TIME_DEFAULT;
import static org.onosproject.net.intent.IntentInstaller.Direction.ADD;
import static org.onosproject.net.intent.IntentInstaller.Direction.REMOVE;
import static org.onosproject.net.intent.IntentState.INSTALLED;
import static org.onosproject.net.intent.IntentState.REALLOCATING;
import static org.onosproject.net.intent.constraint.NonDisruptiveConstraint.requireNonDisruptive;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Installer for FlowRuleIntent.
 */
@Component(
    immediate = true,
    property = {
        NON_DISRUPTIVE_INSTALLATION_WAITING_TIME + ":Integer=" + NON_DISRUPTIVE_INSTALLATION_WAITING_TIME_DEFAULT
    }
)
public class FlowRuleIntentInstaller implements IntentInstaller<FlowRuleIntent> {
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentExtensionService intentExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ObjectiveTrackerService trackerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentInstallCoordinator intentInstallCoordinator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentStore store;

    private ScheduledExecutorService nonDisruptiveIntentInstaller;

    /** Number of seconds to wait during the non-disruptive installation phases. */
    private int nonDisruptiveInstallationWaitingTime = NON_DISRUPTIVE_INSTALLATION_WAITING_TIME_DEFAULT;

    protected final Logger log = getLogger(IntentManager.class);

    private boolean isReallocationStageFailed = false;

    private static final LinkComparator LINK_COMPARATOR = new LinkComparator();

    @Activate
    public void activate() {
        intentExtensionService.registerInstaller(FlowRuleIntent.class, this);
        nonDisruptiveIntentInstaller =
                newSingleThreadScheduledExecutor(groupedThreads("onos/intent", "non-disruptive-installer", log));
        configService.registerProperties(getClass());
    }

    @Deactivate
    public void deactivated() {
        intentExtensionService.unregisterInstaller(FlowRuleIntent.class);
        configService.unregisterProperties(getClass(), false);
    }

    @Modified
    public void modified(ComponentContext context) {

        if (context == null) {
            nonDisruptiveInstallationWaitingTime = NON_DISRUPTIVE_INSTALLATION_WAITING_TIME_DEFAULT;
            log.info("Restored default installation time for non-disruptive reallocation (1 sec.)");
            return;
        }

        String s = Tools.get(context.getProperties(), NON_DISRUPTIVE_INSTALLATION_WAITING_TIME);
        int nonDisruptiveTime = isNullOrEmpty(s) ? nonDisruptiveInstallationWaitingTime : Integer.parseInt(s);
        if (nonDisruptiveTime != nonDisruptiveInstallationWaitingTime) {
            nonDisruptiveInstallationWaitingTime = nonDisruptiveTime;
            log.info("Reconfigured non-disruptive reallocation with installation delay {} sec.",
                     nonDisruptiveInstallationWaitingTime);
        }
    }

    @Override
    public void apply(IntentOperationContext<FlowRuleIntent> context) {
        Optional<IntentData> toUninstall = context.toUninstall();
        Optional<IntentData> toInstall = context.toInstall();

        if (toInstall.isPresent() && toUninstall.isPresent()) {
            Intent intentToInstall = toInstall.get().intent();
            if (requireNonDisruptive(intentToInstall) && INSTALLED.equals(toUninstall.get().state())) {
                reallocate(context);
                return;
            }
        }

        if (!toInstall.isPresent() && !toUninstall.isPresent()) {
            // Nothing to do.
            intentInstallCoordinator.intentInstallSuccess(context);
            return;
        }

        List<FlowRuleIntent> uninstallIntents = context.intentsToUninstall();
        List<FlowRuleIntent> installIntents = context.intentsToInstall();

        List<FlowRule> flowRulesToUninstall;
        List<FlowRule> flowRulesToInstall;

        if (toUninstall.isPresent()) {
            // Remove tracked resource from both Intent and installable Intents.
            trackIntentResources(toUninstall.get(), uninstallIntents, REMOVE);

            // Retrieves all flow rules from all flow rule Intents.
            flowRulesToUninstall = uninstallIntents.stream()
                    .map(FlowRuleIntent::flowRules)
                    .flatMap(Collection::stream)
                    .filter(flowRule -> flowRuleService.getFlowEntry(flowRule) != null)
                    .collect(Collectors.toList());
        } else {
            // No flow rules to be uninstalled.
            flowRulesToUninstall = Collections.emptyList();
        }

        if (toInstall.isPresent()) {
            // Track resource from both Intent and installable Intents.
            trackIntentResources(toInstall.get(), installIntents, ADD);

            // Retrieves all flow rules from all flow rule Intents.
            flowRulesToInstall = installIntents.stream()
                    .map(FlowRuleIntent::flowRules)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        } else {
            // No flow rules to be installed.
            flowRulesToInstall = Collections.emptyList();
        }

        List<FlowRule> flowRuleToModify;
        List<FlowRule> dontTouch;

        // If both uninstall/install list contained equal (=match conditions are equal) FlowRules,
        // omit it from remove list, since it will/should be overwritten by install
        flowRuleToModify = flowRulesToInstall.stream()
                .filter(flowRule -> flowRulesToUninstall.stream().anyMatch(flowRule::equals))
                .collect(Collectors.toList());

        // If both contained exactMatch-ing FlowRules, remove from both list,
        // since it will result in no-op.
        dontTouch = flowRulesToInstall.stream()
                .filter(flowRule -> flowRulesToUninstall.stream().anyMatch(flowRule::exactMatch))
                .collect(Collectors.toList());

        flowRulesToUninstall.removeAll(flowRuleToModify);
        flowRulesToUninstall.removeAll(dontTouch);
        flowRulesToInstall.removeAll(flowRuleToModify);
        flowRulesToInstall.removeAll(dontTouch);
        flowRuleToModify.removeAll(dontTouch);

        if (flowRulesToInstall.isEmpty() && flowRulesToUninstall.isEmpty() && flowRuleToModify.isEmpty()) {
            // There is no flow rules to install/uninstall
            intentInstallCoordinator.intentInstallSuccess(context);
            return;
        }

        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        // Add flows
        flowRulesToInstall.forEach(builder::add);
        // Modify flows
        flowRuleToModify.forEach(builder::modify);
        // Remove flows
        flowRulesToUninstall.forEach(builder::remove);

        FlowRuleOperationsContext flowRuleOperationsContext = new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                intentInstallCoordinator.intentInstallSuccess(context);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                intentInstallCoordinator.intentInstallFailed(context);
            }
        };

        FlowRuleOperations operations = builder.build(flowRuleOperationsContext);
        log.debug("applying intent {} -> {} with {} rules: {}",
                  toUninstall.map(x -> x.key().toString()).orElse("<empty>"),
                  toInstall.map(x -> x.key().toString()).orElse("<empty>"),
                  operations.stages().stream().mapToLong(Set::size).sum(),
                  operations.stages());
        flowRuleService.apply(operations);
    }

    private void reallocate(IntentOperationContext<FlowRuleIntent> context) {

        Optional<IntentData> toUninstall = context.toUninstall();
        Optional<IntentData> toInstall = context.toInstall();

        //TODO: Update the Intent store with this information
        toInstall.get().setState(REALLOCATING);

        store.write(toInstall.get());

        List<FlowRuleIntent> uninstallIntents = Lists.newArrayList(context.intentsToUninstall());
        List<FlowRuleIntent> installIntents = Lists.newArrayList(context.intentsToInstall());
        FlowRuleOperations.Builder firstStageOperationsBuilder = FlowRuleOperations.builder();
        List<FlowRule> secondStageFlowRules = Lists.newArrayList();
        FlowRuleOperations.Builder thirdStageOperationsBuilder = FlowRuleOperations.builder();
        FlowRuleOperations.Builder finalStageOperationsBuilder = FlowRuleOperations.builder();

        prepareReallocation(uninstallIntents, installIntents,
                            firstStageOperationsBuilder, secondStageFlowRules,
                            thirdStageOperationsBuilder, finalStageOperationsBuilder);

        trackIntentResources(toUninstall.get(), uninstallIntents, REMOVE);
        trackIntentResources(toInstall.get(), installIntents, ADD);

        CountDownLatch stageCompleteLatch = new CountDownLatch(1);

        FlowRuleOperations firstStageOperations = firstStageOperationsBuilder
                .build(new StageOperation(context, stageCompleteLatch));

        flowRuleService.apply(firstStageOperations);

        try {
            stageCompleteLatch.await(nonDisruptiveInstallationWaitingTime, TimeUnit.SECONDS);
            if (isReallocationStageFailed) {
                log.error("Reallocation FAILED in stage one: the following FlowRuleOperations are not executed {}",
                          firstStageOperations);
                return;
            } else {
                log.debug("Reallocation stage one completed");
            }
        } catch (Exception e) {
            log.warn("Latch exception in the reallocation stage one");
        }

        for (FlowRule flowRule : secondStageFlowRules) {
            stageCompleteLatch = new CountDownLatch(1);
            FlowRuleOperations operations = FlowRuleOperations.builder()
                    .newStage()
                    .remove(flowRule)
                    .build(new StageOperation(context, stageCompleteLatch));
            nonDisruptiveIntentInstaller.schedule(new NonDisruptiveInstallation(operations),
                                                  nonDisruptiveInstallationWaitingTime,
                                                  TimeUnit.SECONDS);
            try {
                stageCompleteLatch.await(nonDisruptiveInstallationWaitingTime, TimeUnit.SECONDS);
                if (isReallocationStageFailed) {
                    log.error("Reallocation FAILED in stage two: " +
                                      "the following FlowRuleOperations are not executed {}",
                              operations);
                    return;
                } else {
                    log.debug("Reallocation stage two completed");
                }
            } catch (Exception e) {
                log.warn("Latch exception in the reallocation stage two");
            }
        }

        stageCompleteLatch = new CountDownLatch(1);
        FlowRuleOperations thirdStageOperations = thirdStageOperationsBuilder
                .build(new StageOperation(context, stageCompleteLatch));

        nonDisruptiveIntentInstaller.schedule(new NonDisruptiveInstallation(thirdStageOperations),
                                              nonDisruptiveInstallationWaitingTime,
                                              TimeUnit.SECONDS);
        try {
            stageCompleteLatch.await(nonDisruptiveInstallationWaitingTime, TimeUnit.SECONDS);
            if (isReallocationStageFailed) {
                log.error("Reallocation FAILED in stage three: " +
                                  "the following FlowRuleOperations are not executed {}",
                          thirdStageOperations);
                return;
            } else {
                log.debug("Reallocation stage three completed");
            }
        } catch (Exception e) {
            log.warn("Latch exception in the reallocation stage three");
        }

        FlowRuleOperationsContext flowRuleOperationsContext = new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                intentInstallCoordinator.intentInstallSuccess(context);
                log.info("Non-disruptive reallocation completed for intent {}", toInstall.get().key());
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                intentInstallCoordinator.intentInstallFailed(context);
            }
        };

        FlowRuleOperations finalStageOperations = finalStageOperationsBuilder.build(flowRuleOperationsContext);
        flowRuleService.apply(finalStageOperations);
    }

    /**
     * This method prepares the {@link FlowRule} required for every reallocation stage.
     *     <p>Stage 1: the FlowRules of the new path are installed,
     *     with a lower priority only on the devices shared with the old path;</p>
     *     <p>Stage 2: the FlowRules of the old path are removed from the ingress to the egress points,
     *     only in the shared devices;</p>
     *     <p>Stage 3: the FlowRules with a lower priority are restored to the original one;</p>
     *     <p>Stage 4: the remaining FlowRules of the old path are deleted.</p>
     *
     * @param uninstallIntents the previous FlowRuleIntent
     * @param installIntents the new FlowRuleIntent to be installed
     * @param firstStageBuilder the first stage operation builder
     * @param secondStageFlowRules the second stage FlowRules
     * @param thirdStageBuilder the third stage operation builder
     * @param finalStageBuilder the last stage operation builder
     */
    private void prepareReallocation(List<FlowRuleIntent> uninstallIntents, List<FlowRuleIntent> installIntents,
                                     FlowRuleOperations.Builder firstStageBuilder,
                                     List<FlowRule> secondStageFlowRules,
                                     FlowRuleOperations.Builder thirdStageBuilder,
                                     FlowRuleOperations.Builder finalStageBuilder) {


        // Filter out same intents and intents with same flow rules
        installIntents.forEach(installIntent -> {
            uninstallIntents.forEach(uninstallIntent -> {

                List<FlowRule> uninstallFlowRules = Lists.newArrayList(uninstallIntent.flowRules());
                List<FlowRule> installFlowRules = Lists.newArrayList(installIntent.flowRules());

                List<FlowRule> secondStageRules = Lists.newArrayList();
                List<FlowRule> thirdStageRules = Lists.newArrayList();

                List<DeviceId> orderedDeviceList = createIngressToEgressDeviceList(installIntent.resources());

                uninstallIntent.flowRules().forEach(flowRuleToUnistall -> {
                    installIntent.flowRules().forEach(flowRuleToInstall -> {

                        if (flowRuleToInstall.exactMatch(flowRuleToUnistall)) {
                            //The FlowRules are in common (i.e., we are sharing the path)
                            uninstallFlowRules.remove(flowRuleToInstall);
                            installFlowRules.remove(flowRuleToInstall);
                        } else if (flowRuleToInstall.deviceId().equals(flowRuleToUnistall.deviceId())) {
                            //FlowRules that have a device in common but
                            // different treatment/selector (i.e., overlapping path)
                            FlowRule flowRuleWithLowerPriority = DefaultFlowRule.builder()
                                    .withPriority(flowRuleToInstall.priority() - 1)
                                    .withSelector(flowRuleToInstall.selector())
                                    .forDevice(flowRuleToInstall.deviceId())
                                    .makePermanent()
                                    .withTreatment(flowRuleToInstall.treatment())
                                    .fromApp(new DefaultApplicationId(flowRuleToInstall.appId(),
                                                                      "org.onosproject.net.intent"))
                                    .build();

                            //Update the FlowRule to be installed with one with a lower priority
                            installFlowRules.remove(flowRuleToInstall);
                            installFlowRules.add(flowRuleWithLowerPriority);

                            //Add the FlowRule to be uninstalled to the second stage of non-disruptive update
                            secondStageRules.add(flowRuleToUnistall);
                            uninstallFlowRules.remove(flowRuleToUnistall);

                            thirdStageRules.add(flowRuleToInstall);
                            uninstallFlowRules.add(flowRuleWithLowerPriority);
                        }
                    });
                });

                firstStageBuilder.newStage();
                installFlowRules.forEach(firstStageBuilder::add);

                Collections.sort(secondStageRules, new SecondStageComparator(orderedDeviceList));
                secondStageFlowRules.addAll(secondStageRules);

                thirdStageBuilder.newStage();
                thirdStageRules.forEach(thirdStageBuilder::add);

                finalStageBuilder.newStage();
                uninstallFlowRules.forEach(finalStageBuilder::remove);
            });
        });

    }

    private class StageOperation implements FlowRuleOperationsContext {

        private IntentOperationContext<FlowRuleIntent> context;
        private CountDownLatch stageCompleteLatch;

        public StageOperation(IntentOperationContext<FlowRuleIntent> context, CountDownLatch stageCompleteLatch) {
            this.context = context;
            this.stageCompleteLatch = stageCompleteLatch;
            isReallocationStageFailed = false;
        }

        @Override
        public void onSuccess(FlowRuleOperations ops) {
            stageCompleteLatch.countDown();
            log.debug("FlowRuleOperations correctly completed");
        }

        @Override
        public void onError(FlowRuleOperations ops) {
            intentInstallCoordinator.intentInstallFailed(context);
            isReallocationStageFailed = true;
            stageCompleteLatch.countDown();
            log.debug("Installation error for {}", ops);
        }
    }

    private final class SecondStageComparator implements Comparator<FlowRule> {

        private List<DeviceId> deviceIds;

        private SecondStageComparator(List<DeviceId> deviceIds) {
            this.deviceIds = deviceIds;
        }

        @Override
        public int compare(FlowRule o1, FlowRule o2) {
            Integer index1 = deviceIds.indexOf(o1.deviceId());
            Integer index2 = deviceIds.indexOf(o2.deviceId());
            return index1.compareTo(index2);
        }
    }

    /**
     * Create a list of devices ordered from the ingress to the egress of a path.
     * @param resources the resources of the intent
     * @return a list of devices
     */
    private List<DeviceId> createIngressToEgressDeviceList(Collection<NetworkResource> resources) {
        List<DeviceId> deviceIds = Lists.newArrayList();
        List<Link> links = Lists.newArrayList();

        for (NetworkResource resource : resources) {
            if (resource instanceof Link) {
                Link linkToAdd = (Link) resource;
                if (linkToAdd.type() != Link.Type.EDGE) {
                    links.add(linkToAdd);
                }
            }
        }

        Collections.sort(links, LINK_COMPARATOR);

        int i = 0;
        for (Link orderedLink : links) {
            deviceIds.add(orderedLink.src().deviceId());
            if (i == resources.size() - 1) {
                deviceIds.add(orderedLink.dst().deviceId());
            }
            i++;
        }

        return deviceIds;
    }

    /**
     * Compares two links in order to find which one is before or after the other.
     */
    private static class LinkComparator implements Comparator<Link> {

        @Override
        public int compare(Link l1, Link l2) {

            //l1 is before l2
            if (l1.dst().deviceId() == l2.src().deviceId()) {
                return -1;
            }

            //l1 is after l2
            if (l1.src().deviceId() == l2.dst().deviceId()) {
                return 1;
            }

            //l2 and l1 are not connected to a common device
            return 0;
        }
    }

    private final class NonDisruptiveInstallation implements Runnable {

        private FlowRuleOperations op;

        private NonDisruptiveInstallation(FlowRuleOperations op) {
            this.op = op;
        }
        @Override
        public void run() {
            flowRuleService.apply(this.op);
        }
    }

    /**
     * Track or un-track network resource of a Intent and it's installable
     * Intents.
     *
     * @param intentData the Intent data
     * @param intentsToApply the list of flow rule Intents from the Intent
     * @param direction the direction to determine track or un-track
     */
    private void trackIntentResources(IntentData intentData, List<FlowRuleIntent> intentsToApply, Direction direction) {
        switch (direction) {
            case ADD:
                trackerService.addTrackedResources(intentData.key(), intentData.intent().resources());
                intentsToApply.forEach(installable ->
                                               trackerService.addTrackedResources(intentData.key(),
                                                                                  installable.resources()));
                break;
            case REMOVE:
                trackerService.removeTrackedResources(intentData.key(), intentData.intent().resources());
                intentsToApply.forEach(installable ->
                                               trackerService.removeTrackedResources(intentData.intent().key(),
                                                                                     installable.resources()));
                break;
            default:
                log.warn("Unknown resource tracking direction.");
                break;
        }
    }
}