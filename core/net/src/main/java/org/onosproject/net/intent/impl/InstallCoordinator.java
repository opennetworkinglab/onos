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

package org.onosproject.net.intent.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentInstallationContext;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentStore;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of IntentInstallCoordinator.
 */
public class InstallCoordinator {
    private static final String INSTALLER_NOT_FOUND = "Intent installer not found, Intent: {}";
    private final Logger log = getLogger(InstallCoordinator.class);

    private InstallerRegistry installerRegistry;
    private IntentStore intentStore;

    /**
     * Creates an InstallCoordinator.
     *
     * @param installerRegistry the installer registry
     * @param intentStore the Intent store
     */
    public InstallCoordinator(InstallerRegistry installerRegistry,
                              IntentStore intentStore) {
        this.installerRegistry = installerRegistry;
        this.intentStore = intentStore;
    }

    /**
     * Applies Intent data to be uninstalled and to be installed.
     *
     * @param toUninstall Intent data to be uninstalled
     * @param toInstall Intent data to be installed
     */
    public void installIntents(Optional<IntentData> toUninstall, Optional<IntentData> toInstall) {
        // If no any Intents to be uninstalled or installed, ignore it.
        if (!toUninstall.isPresent() && !toInstall.isPresent()) {
            return;
        }

        // Classify installable Intents to different installers.
        ArrayListMultimap<IntentInstaller, Intent> uninstallInstallers;
        ArrayListMultimap<IntentInstaller, Intent> installInstallers;
        Set<IntentInstaller> allInstallers = Sets.newHashSet();

        if (toUninstall.isPresent()) {
            uninstallInstallers = getInstallers(toUninstall.get());
            allInstallers.addAll(uninstallInstallers.keySet());
        } else {
            uninstallInstallers = ArrayListMultimap.create();
        }

        if (toInstall.isPresent()) {
            installInstallers = getInstallers(toInstall.get());
            allInstallers.addAll(installInstallers.keySet());
        } else {
            installInstallers = ArrayListMultimap.create();
        }

        // Generates an installation context for the high level Intent.
        IntentInstallationContext installationContext =
                new IntentInstallationContext(toUninstall.orElse(null), toInstall.orElse(null));

        //Generates different operation context for different installable Intents.
        Map<IntentInstaller, IntentOperationContext> contexts = Maps.newHashMap();
        allInstallers.forEach(installer -> {
            List<Intent> intentsToUninstall = uninstallInstallers.get(installer);
            List<Intent> intentsToInstall = installInstallers.get(installer);

            // Connect context to high level installation context
            IntentOperationContext context =
                    new IntentOperationContext(intentsToUninstall, intentsToInstall,
                                               installationContext);
            installationContext.addPendingContext(context);
            contexts.put(installer, context);
        });

        // Apply contexts to installers
        contexts.forEach((installer, context) -> {
            installer.apply(context);
        });
    }

    /**
     * Generates a mapping for installable Intents to installers.
     *
     * @param intentData the Intent data which contains installable Intents
     * @return the mapping for installable Intents to installers
     */
    private ArrayListMultimap<IntentInstaller, Intent> getInstallers(IntentData intentData) {
        ArrayListMultimap<IntentInstaller, Intent> intentInstallers = ArrayListMultimap.create();
        intentData.installables().forEach(intent -> {
            IntentInstaller installer = installerRegistry.getInstaller(intent.getClass());
            if (installer != null) {
                intentInstallers.put(installer, intent);
            } else {
                log.warn(INSTALLER_NOT_FOUND, intent);
            }
        });
        return intentInstallers;
    }

    /**
     * Handles success operation context.
     *
     * @param context the operation context
     */
    public void success(IntentOperationContext context) {
        IntentInstallationContext intentInstallationContext =
                context.intentInstallationContext();
        intentInstallationContext.removePendingContext(context);

        if (intentInstallationContext.isPendingContextsEmpty()) {
            finish(intentInstallationContext);
        }
    }

    /**
     * Handles failed operation context.
     *
     * @param context the operation context
     */
    public void failed(IntentOperationContext context) {
        IntentInstallationContext intentInstallationContext =
                context.intentInstallationContext();
        intentInstallationContext.addErrorContext(context);
        intentInstallationContext.removePendingContext(context);

        if (intentInstallationContext.isPendingContextsEmpty()) {
            finish(intentInstallationContext);
        }
    }

    /**
     * Completed the installation context and update the Intent store.
     *
     * @param intentInstallationContext the installation context
     */
    private void finish(IntentInstallationContext intentInstallationContext) {
        Set<IntentOperationContext> errCtxs = intentInstallationContext.errorContexts();
        Optional<IntentData> toUninstall = intentInstallationContext.toUninstall();
        Optional<IntentData> toInstall = intentInstallationContext.toInstall();

        // Intent install success
        if (errCtxs == null || errCtxs.isEmpty()) {
            if (toInstall.isPresent()) {
                IntentData installData = toInstall.get();
                log.debug("Completed installing: {}:{}",
                          installData.key(),
                          installData.intent().id());
                installData = new IntentData(installData, installData.installables());
                installData.setState(INSTALLED);
                intentStore.write(installData);
            } else if (toUninstall.isPresent()) {
                IntentData uninstallData = toUninstall.get();
                uninstallData = new IntentData(uninstallData, Collections.emptyList());
                log.debug("Completed withdrawing: {}:{}",
                          uninstallData.key(),
                          uninstallData.intent().id());
                switch (uninstallData.request()) {
                    case INSTALL_REQ:
                        // INSTALLED intent was damaged & clean up is now complete
                        uninstallData.setState(FAILED);
                        break;
                    case WITHDRAW_REQ:
                    default: //TODO "default" case should not happen
                        uninstallData.setState(WITHDRAWN);
                        break;
                }
                // Intent has been withdrawn; we can clear the installables
                intentStore.write(uninstallData);
            }
        } else {
            // if toInstall was cause of error, then recompile (manage/increment counter, when exceeded -> CORRUPT)
            if (toInstall.isPresent()) {
                IntentData installData = toInstall.get();
                intentStore.write(IntentData.corrupt(installData));
            }
            // if toUninstall was cause of error, then CORRUPT (another job will clean this up)
            if (toUninstall.isPresent()) {
                IntentData uninstallData = toUninstall.get();
                intentStore.write(IntentData.corrupt(uninstallData));
            }
        }
    }
}
