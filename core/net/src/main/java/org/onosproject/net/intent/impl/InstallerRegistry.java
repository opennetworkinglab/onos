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

import com.google.common.collect.ImmutableMap;
import org.onosproject.net.flow.FlowRuleOperation;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentException;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkState;
import static org.onlab.util.Tools.isNullOrEmpty;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.INSTALLED;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;

// TODO: consider a better name
class InstallerRegistry {

    private static final Logger log = LoggerFactory.getLogger(InstallerRegistry.class);

    private final ConcurrentMap<Class<? extends Intent>,
            IntentInstaller<? extends Intent>> installers = new ConcurrentHashMap<>();
    /**
     * Registers the specified installer for the given installable intent class.
     *
     * @param cls       installable intent class
     * @param installer intent installer
     * @param <T>       the type of installable intent
     */
    <T extends Intent> void registerInstaller(Class<T> cls, IntentInstaller<T> installer) {
        installers.put(cls, installer);
    }

    /**
     * Unregisters the installer for the given installable intent class.
     *
     * @param cls installable intent class
     * @param <T> the type of installable intent
     */
    <T extends Intent> void unregisterInstaller(Class<T> cls) {
        installers.remove(cls);
    }

    /**
     * Returns immutable set of bindings of currently registered intent installers.
     *
     * @return the set of installer bindings
     */
    Map<Class<? extends Intent>, IntentInstaller<? extends Intent>> getInstallers() {
        return ImmutableMap.copyOf(installers);
    }

    /**
     * Returns the corresponding intent installer to the specified installable intent.
     *
     * @param intent intent
     * @param <T>    the type of installable intent
     * @return intent installer corresponding to the specified installable intent
     */
    private <T extends Intent> IntentInstaller<T> getInstaller(T intent) {
        @SuppressWarnings("unchecked")
        IntentInstaller<T> installer = (IntentInstaller<T>) installers.get(intent.getClass());
        if (installer == null) {
            throw new IntentException("no installer for class " + intent.getClass());
        }
        return installer;
    }

    /**
     * Registers an intent installer of the specified intent if an intent installer
     * for the intent is not registered. This method traverses the class hierarchy of
     * the intent. Once an intent installer for a parent type is found, this method
     * registers the found intent installer.
     *
     * @param intent intent
     */
    private void registerSubclassInstallerIfNeeded(Intent intent) {
        if (!installers.containsKey(intent.getClass())) {
            Class<?> cls = intent.getClass();
            while (cls != Object.class) {
                // As long as we're within the Intent class descendants
                if (Intent.class.isAssignableFrom(cls)) {
                    IntentInstaller<?> installer = installers.get(cls);
                    if (installer != null) {
                        installers.put(intent.getClass(), installer);
                        return;
                    }
                }
                cls = cls.getSuperclass();
            }
        }
    }

    /**
     * Generate a {@link FlowRuleOperations} instance from the specified intent data.
     *
     * @param current        intent data stored in the store
     * @param pending        intent data being processed
     * @param store          intent store saving the intent state in this method
     * @param trackerService objective tracker that is used in this method
     * @return flow rule operations
     */
    public FlowRuleOperations coordinate(IntentData current, IntentData pending,
                                         IntentStore store, ObjectiveTrackerService trackerService) {
        List<Intent> oldInstallables = (current != null) ? current.installables() : null;
        List<Intent> newInstallables = pending.installables();

        checkState(isNullOrEmpty(oldInstallables) ||
                        oldInstallables.size() == newInstallables.size(),
                "Old and New Intent must have equivalent installable intents.");

        List<List<Collection<FlowRuleOperation>>> plans = new ArrayList<>();
        for (int i = 0; i < newInstallables.size(); i++) {
            Intent newInstallable = newInstallables.get(i);
            registerSubclassInstallerIfNeeded(newInstallable);
            //TODO consider migrating installers to FlowRuleOperations
            /* FIXME
               - we need to do another pass on this method about that doesn't
               require the length of installables to be equal, and also doesn't
               depend on ordering
               - we should also reconsider when to start/stop tracking resources
             */
            if (isNullOrEmpty(oldInstallables)) {
                plans.add(getInstaller(newInstallable).install(newInstallable));
            } else {
                Intent oldInstallable = oldInstallables.get(i);
                checkState(oldInstallable.getClass().equals(newInstallable.getClass()),
                        "Installable Intent type mismatch.");
                trackerService.removeTrackedResources(pending.key(), oldInstallable.resources());
                plans.add(getInstaller(newInstallable).replace(oldInstallable, newInstallable));
            }
            trackerService.addTrackedResources(pending.key(), newInstallable.resources());
//            } catch (IntentException e) {
//                log.warn("Unable to update intent {} due to:", oldIntent.id(), e);
//                //FIXME... we failed. need to uninstall (if same) or revert (if different)
//                trackerService.removeTrackedResources(newIntent.id(), newInstallable.resources());
//                exception = e;
//                batches = uninstallIntent(oldIntent, oldInstallables);
//            }
        }

        return merge(plans).build(new FlowRuleOperationsContext() { // TODO move this out
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Completed installing: {}", pending.key());
                pending.setState(INSTALLED);
                store.write(pending);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.warn("Failed installation: {} {} on {}", pending.key(),
                        pending.intent(), ops);
                //TODO store.write(pending.setState(BROKEN));
                pending.setState(FAILED);
                store.write(pending);
            }
        });
    }

    /**
     * Generate a {@link FlowRuleOperations} instance from the specified intent data.
     *
     * @param current        intent data stored in the store
     * @param pending        intent date being processed
     * @param store          intent store saving the intent state in this method
     * @param trackerService objective tracker that is used in this method
     * @return flow rule operations
     */
    FlowRuleOperations uninstallCoordinate(IntentData current, IntentData pending,
                                                  IntentStore store, ObjectiveTrackerService trackerService) {
        List<Intent> installables = current.installables();
        List<List<Collection<FlowRuleOperation>>> plans = new ArrayList<>();
        for (Intent installable : installables) {
            plans.add(getInstaller(installable).uninstall(installable));
            trackerService.removeTrackedResources(pending.key(), installable.resources());
        }

        return merge(plans).build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Completed withdrawing: {}", pending.key());
                pending.setState(WITHDRAWN);
                pending.setInstallables(Collections.emptyList());
                store.write(pending);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.warn("Failed withdraw: {}", pending.key());
                pending.setState(FAILED);
                store.write(pending);
            }
        });
    }


    // TODO needs tests... or maybe it's just perfect
    private FlowRuleOperations.Builder merge(List<List<Collection<FlowRuleOperation>>> plans) {
        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        // Build a batch one stage at a time
        for (int stageNumber = 0;; stageNumber++) {
            // Get the sub-stage from each plan (List<Set<FlowRuleOperation>)
            for (Iterator<List<Collection<FlowRuleOperation>>> itr = plans.iterator(); itr.hasNext();) {
                List<Collection<FlowRuleOperation>> plan = itr.next();
                if (plan.size() <= stageNumber) {
                    // we have consumed all stages from this plan, so remove it
                    itr.remove();
                    continue;
                }
                // write operations from this sub-stage into the builder
                Collection<FlowRuleOperation> stage = plan.get(stageNumber);
                for (FlowRuleOperation entry : stage) {
                    builder.operation(entry);
                }
            }
            // we are done with the stage, start the next one...
            if (plans.isEmpty()) {
                break; // we don't need to start a new stage, we are done.
            }
            builder.newStage();
        }
        return builder;
    }
}
