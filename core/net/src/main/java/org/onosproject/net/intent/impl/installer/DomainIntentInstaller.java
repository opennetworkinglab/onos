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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.net.domain.DomainIntent;
import org.onosproject.net.domain.DomainIntentOperations;
import org.onosproject.net.domain.DomainIntentOperationsContext;
import org.onosproject.net.domain.DomainIntentService;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentInstallCoordinator;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.impl.IntentManager;
import org.onosproject.net.intent.ObjectiveTrackerService;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Installer for domain Intent.
 */
@Component(immediate = true)
public class DomainIntentInstaller implements IntentInstaller<DomainIntent> {

    private final Logger log = getLogger(IntentManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentExtensionService intentExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ObjectiveTrackerService trackerService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentInstallCoordinator intentInstallCoordinator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DomainIntentService domainIntentService;

    @Activate
    public void activated() {
        intentExtensionService.registerInstaller(DomainIntent.class, this);
    }

    @Deactivate
    public void deactivated() {
        intentExtensionService.unregisterInstaller(DomainIntent.class);
    }

    @Override
    public void apply(IntentOperationContext<DomainIntent> context) {
        Optional<IntentData> toUninstall = context.toUninstall();
        Optional<IntentData> toInstall = context.toInstall();

        List<DomainIntent> uninstallIntents = context.intentsToUninstall();
        List<DomainIntent> installIntents = context.intentsToInstall();

        if (!toInstall.isPresent() && !toUninstall.isPresent()) {
            intentInstallCoordinator.intentInstallSuccess(context);
            return;
        }

        if (toUninstall.isPresent()) {
            IntentData intentData = toUninstall.get();
            trackerService.removeTrackedResources(intentData.key(), intentData.intent().resources());
            uninstallIntents.forEach(installable ->
                                             trackerService.removeTrackedResources(intentData.intent().key(),
                                                                                   installable.resources()));
        }

        if (toInstall.isPresent()) {
            IntentData intentData = toInstall.get();
            trackerService.addTrackedResources(intentData.key(), intentData.intent().resources());
            installIntents.forEach(installable ->
                                           trackerService.addTrackedResources(intentData.key(),
                                                                              installable.resources()));
        }

        // Generate domain Intent operations
        DomainIntentOperations.Builder builder = DomainIntentOperations.builder();
        DomainIntentOperationsContext domainOperationsContext;

        uninstallIntents.forEach(builder::remove);
        installIntents.forEach(builder::add);

        domainOperationsContext = new DomainIntentOperationsContext() {
            @Override
            public void onSuccess(DomainIntentOperations idops) {
                intentInstallCoordinator.intentInstallSuccess(context);
            }

            @Override
            public void onError(DomainIntentOperations idos) {
                intentInstallCoordinator.intentInstallFailed(context);
            }
        };
        log.debug("submitting domain intent {} -> {}",
                  toUninstall.map(x -> x.key().toString()).orElse("<empty>"),
                  toInstall.map(x -> x.key().toString()).orElse("<empty>"));

        // Submit domain Inten operations with domain context
        domainIntentService.sumbit(builder.build(domainOperationsContext));
    }
}
