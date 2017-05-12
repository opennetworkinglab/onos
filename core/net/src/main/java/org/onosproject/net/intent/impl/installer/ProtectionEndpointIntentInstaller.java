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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription;
import org.onosproject.net.behaviour.protection.ProtectionConfig;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentException;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.IntentInstallCoordinator;
import org.onosproject.net.intent.IntentOperationContext;
import org.onosproject.net.intent.IntentInstaller;
import org.onosproject.net.intent.ProtectionEndpointIntent;
import org.onosproject.net.intent.impl.IntentManager;
import org.onosproject.net.intent.ObjectiveTrackerService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.config.NetworkConfigEvent.Type.*;
import static org.onosproject.net.intent.IntentInstaller.Direction.ADD;
import static org.onosproject.net.intent.IntentInstaller.Direction.REMOVE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Installer for ProtectionEndpointIntent.
 */
@Component(immediate = true)
public class ProtectionEndpointIntentInstaller implements IntentInstaller<ProtectionEndpointIntent> {
    private static final String CONFIG_FAILED = "Config operation unsuccessful, expected %s, actual %s.";
    private final Logger log = getLogger(IntentManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentExtensionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    IntentInstallCoordinator intentInstallCoordinator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    ObjectiveTrackerService trackerService;

    @Activate
    public void activate() {
        intentExtensionService.registerInstaller(ProtectionEndpointIntent.class, this);
    }

    @Deactivate
    public void deactivated() {
        intentExtensionService.unregisterInstaller(ProtectionEndpointIntent.class);
    }

    @Override
    public void apply(IntentOperationContext<ProtectionEndpointIntent> context) {
        Optional<IntentData> toUninstall = context.toUninstall();
        Optional<IntentData> toInstall = context.toInstall();

        List<ProtectionEndpointIntent> uninstallIntents = context.intentsToUninstall();
        List<ProtectionEndpointIntent> installIntents = context.intentsToInstall();

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

        List<Stage> stages = new ArrayList<>();

        stages.add(new Stage(uninstallIntents.stream()
                                     .map(i -> Pair.of(i, REMOVE))
                                     .collect(Collectors.toList())));

        stages.add(new Stage(installIntents.stream()
                                     .map(i -> Pair.of(i, ADD))
                                     .collect(Collectors.toList())));
        for (Stage stage : stages) {
            log.debug("applying Stage {}", stage);
            try {
                // wait for stage completion
                stage.apply();
                stage.listeners().forEach(networkConfigService::removeListener);
            } catch (IntentException e) {
                log.error("Stage {} failed, reason: {}", stage, e.toString());
                intentInstallCoordinator.intentInstallFailed(context);
                return;
            }
        }
        // All stage success
        intentInstallCoordinator.intentInstallSuccess(context);
    }

    /**
     * Stage of installable Intents which can be processed in parallel.
     */
    private final class Stage {
        // should it have progress state, how far it went?
        private final Collection<Pair<ProtectionEndpointIntent, Direction>> ops;
        private final Set<NetworkConfigListener> listeners = Sets.newHashSet();

        /**
         * Create a stage with given operations.
         *
         * @param ops the operations
         */
        Stage(Collection<Pair<ProtectionEndpointIntent, Direction>> ops) {
            this.ops = checkNotNull(ops);
        }

        /**
         * Applies all operations for this stage.
         *
         * @return the CompletableFuture object for this operation
         */
        void apply() {
            ops.stream()
                    .map(op -> applyOp(op.getRight(), op.getLeft()))
                    .forEach(future -> {
                        try {
                            future.get(100, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException | InterruptedException | ExecutionException e) {
                            throw new IntentException(e.toString());
                        }
                    });
        }

        /**
         * Applies the protection endpoint Intent with a given direction.
         *
         * @param dir the direction
         * @param intent the protection endpoint Intent
         * @return the CompletableFuture object for this operation
         */
        private CompletableFuture<Void> applyOp(Direction dir, ProtectionEndpointIntent intent) {
            log.trace("applying {}: {}", dir, intent);
            if (dir == REMOVE) {
                ProtectionConfigListener listener =
                        new ProtectionConfigListener(ImmutableSet.of(CONFIG_REMOVED),
                                                     intent.deviceId());
                networkConfigService.addListener(listener);
                listeners.add(listener);
                networkConfigService.removeConfig(intent.deviceId(), ProtectionConfig.class);
                return listener.completableFuture();
            } else {
                ProtectedTransportEndpointDescription description = intent.description();

                // Can't do following. Will trigger empty CONFIG_ADDED
                //ProtectionConfig cfg = networkConfigService.addConfig(intent.deviceId(),
                //                                                      ProtectionConfig.class);
                ProtectionConfig cfg = new ProtectionConfig(intent.deviceId());
                cfg.fingerprint(description.fingerprint());
                cfg.peer(description.peer());
                cfg.paths(description.paths());
                ProtectionConfigListener listener =
                        new ProtectionConfigListener(ImmutableSet.of(CONFIG_ADDED, CONFIG_UPDATED),
                                                     intent.deviceId());

                networkConfigService.addListener(listener);
                listeners.add(listener);
                networkConfigService.applyConfig(intent.deviceId(),
                                                 ProtectionConfig.class,
                                                 cfg.node());
                return listener.completableFuture();
            }
        }

        @Override
        public String toString() {
            return ops.toString();
        }

        public Set<NetworkConfigListener> listeners() {
            return listeners;
        }

        /**
         * Listener for protection config for specific config event and device.
         */
        class ProtectionConfigListener implements NetworkConfigListener {
            private CompletableFuture<Void> completableFuture;
            private Set<NetworkConfigEvent.Type> listenTypes;
            private DeviceId listenDevice;

            public ProtectionConfigListener(Set<NetworkConfigEvent.Type> listenTypes, DeviceId listenDevice) {
                completableFuture = new CompletableFuture<>();
                this.listenTypes = listenTypes;
                this.listenDevice = listenDevice;
            }

            @Override
            public void event(NetworkConfigEvent event) {
                if (!event.subject().equals(listenDevice)) {
                    return;
                }
                if (!listenTypes.contains(event.type())) {
                    String errorMsg = String.format(CONFIG_FAILED, listenTypes.toString(), event.type());
                    completableFuture.completeExceptionally(new IntentException(errorMsg));
                } else {
                    completableFuture.complete(null);
                }
            }

            public CompletableFuture<Void> completableFuture() {
                return completableFuture;
            }
        }
    }
}
