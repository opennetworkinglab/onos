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
package org.onosproject.upgrade.impl;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.UnifiedClusterService;
import org.onosproject.core.Version;
import org.onosproject.core.VersionService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicValue;
import org.onosproject.store.service.AtomicValueEvent;
import org.onosproject.store.service.AtomicValueEventListener;
import org.onosproject.store.service.CoordinationService;
import org.onosproject.store.service.Serializer;
import org.onosproject.upgrade.Upgrade;
import org.onosproject.upgrade.UpgradeAdminService;
import org.onosproject.upgrade.UpgradeEvent;
import org.onosproject.upgrade.UpgradeEventListener;
import org.onosproject.upgrade.UpgradeService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Upgrade service implementation.
 * <p>
 * This implementation uses the {@link CoordinationService} to store upgrade state in a version-agnostic primitive.
 * Upgrade state can be seen by current and future version nodes.
 */
@Component(immediate = true)
@Service
public class UpgradeManager
        extends AbstractListenerManager<UpgradeEvent, UpgradeEventListener>
        implements UpgradeService, UpgradeAdminService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VersionService versionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoordinationService coordinationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UnifiedClusterService clusterService;

    private Version localVersion;
    private AtomicValue<Upgrade> state;
    private final AtomicReference<Upgrade> currentState = new AtomicReference<>();
    private final AtomicValueEventListener<Upgrade> stateListener = event -> handleChange(event);

    @Activate
    public void activate() {
        state = coordinationService.<Upgrade>atomicValueBuilder()
                .withName("onos-upgrade-state")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asAtomicValue();
        localVersion = versionService.version();

        currentState.set(state.get());
        if (currentState.get() == null) {
            currentState.set(new Upgrade(localVersion, localVersion, Upgrade.Status.INACTIVE));
            state.set(currentState.get());
        }

        Upgrade upgrade = currentState.get();

        // If the upgrade state is not initialized, ensure this node matches the version of the cluster.
        if (!upgrade.status().active() && !Objects.equals(upgrade.source(), localVersion)) {
            log.error("Node version {} inconsistent with cluster version {}", localVersion, upgrade.source());
            throw new IllegalStateException("Node version " + localVersion +
                    " inconsistent with cluster version " + upgrade.source());
        }

        // If the upgrade state is initialized then check the node version.
        if (upgrade.status() == Upgrade.Status.INITIALIZED) {
            // If the source version equals the target version, attempt to update the target version.
            if (Objects.equals(upgrade.source(), upgrade.target()) && !Objects.equals(upgrade.target(), localVersion)) {
                upgrade = new Upgrade(upgrade.source(), localVersion, upgrade.status());
                currentState.set(upgrade);
                state.set(upgrade);
            }
        }

        // If the upgrade status is active, verify that the local version matches the upgrade version.
        if (upgrade.status().active() && !Objects.equals(upgrade.source(), upgrade.target())) {
            // If the upgrade source/target are not equal, validate that the node's version is consistent
            // with versions in the upgrade. There are two possibilities: that a not-yet-upgraded node is being
            // restarted, or that a node has been upgraded, so we need to check that this node is running either
            // the source or target version.
            if (!Objects.equals(localVersion, upgrade.source()) && !Objects.equals(localVersion, upgrade.target())) {
                log.error("Cannot upgrade node to version {}; Upgrade to {} already in progress",
                        localVersion, upgrade.target());
                throw new IllegalStateException("Cannot upgrade node to version " + localVersion + "; Upgrade to " +
                        upgrade.target() + " already in progress");
            }
        }

        state.addListener(stateListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        state.removeListener(stateListener);
        log.info("Stopped");
    }

    @Override
    public boolean isUpgrading() {
        return getState().status().active();
    }

    @Override
    public Upgrade getState() {
        return currentState.get();
    }

    @Override
    public Version getVersion() {
        Upgrade upgrade = currentState.get();
        return upgrade.status().upgraded()
                ? upgrade.target()
                : upgrade.source();
    }

    @Override
    public boolean isLocalActive() {
        return localVersion.equals(getVersion());
    }

    @Override
    public boolean isLocalUpgraded() {
        Upgrade upgrade = currentState.get();
        return upgrade.status().active()
                && !upgrade.source().equals(upgrade.target())
                && localVersion.equals(upgrade.target());
    }

    @Override
    public void initialize() {
        Upgrade inactive = currentState.get();

        // If the current upgrade status is active, fail initialization.
        if (inactive.status().active()) {
            throw new IllegalStateException("Upgrade already active");
        }

        // Set the upgrade status to INITIALIZING.
        Upgrade initializing = new Upgrade(
                localVersion,
                localVersion,
                Upgrade.Status.INITIALIZING);
        if (!state.compareAndSet(inactive, initializing)) {
            throw new IllegalStateException("Concurrent upgrade modification");
        } else {
            currentState.set(initializing);

            // Set the upgrade status to INITIALIZED.
            Upgrade initialized = new Upgrade(
                    initializing.source(),
                    initializing.target(),
                    Upgrade.Status.INITIALIZED);
            if (!state.compareAndSet(initializing, initialized)) {
                throw new IllegalStateException("Concurrent upgrade modification");
            } else {
                currentState.set(initialized);
            }
        }
    }

    @Override
    public void upgrade() {
        Upgrade initialized = currentState.get();

        // If the current upgrade status is not INITIALIZED, throw an exception.
        if (initialized.status() != Upgrade.Status.INITIALIZED) {
            throw new IllegalStateException("Upgrade not initialized");
        }

        // Set the upgrade status to UPGRADING.
        Upgrade upgrading = new Upgrade(
                initialized.source(),
                initialized.target(),
                Upgrade.Status.UPGRADING);
        if (!state.compareAndSet(initialized, upgrading)) {
            throw new IllegalStateException("Concurrent upgrade modification");
        } else {
            currentState.set(upgrading);

            // Set the upgrade status to UPGRADED.
            Upgrade upgraded = new Upgrade(
                    upgrading.source(),
                    upgrading.target(),
                    Upgrade.Status.UPGRADED);
            if (!state.compareAndSet(upgrading, upgraded)) {
                throw new IllegalStateException("Concurrent upgrade modification");
            } else {
                currentState.set(upgraded);
            }
        }
    }

    @Override
    public void commit() {
        Upgrade upgraded = currentState.get();

        // If the current upgrade status is not UPGRADED, throw an exception.
        if (upgraded.status() != Upgrade.Status.UPGRADED) {
            throw new IllegalStateException("Upgrade not performed");
        }

        // Determine whether any nodes have not been upgraded to the target version.
        boolean upgradeComplete = clusterService.getNodes()
                .stream()
                .allMatch(node -> {
                    ControllerNode.State state = clusterService.getState(node.id());
                    Version version = clusterService.getVersion(node.id());
                    return state.isActive() && version != null && version.equals(upgraded.target());
                });

        // If some nodes have not yet been upgraded, throw an exception.
        if (!upgradeComplete) {
            throw new IllegalStateException("Some nodes have not yet been upgraded to version " + upgraded.target());
        }

        // Set the upgrade status to COMMITTING.
        Upgrade committing = new Upgrade(
                upgraded.source(),
                upgraded.target(),
                Upgrade.Status.COMMITTING);
        if (!state.compareAndSet(upgraded, committing)) {
            throw new IllegalStateException("Concurrent upgrade modification");
        } else {
            currentState.set(committing);

            // Set the upgrade status to COMMITTED.
            Upgrade committed = new Upgrade(
                    committing.source(),
                    committing.target(),
                    Upgrade.Status.COMMITTED);
            if (!state.compareAndSet(committing, committed)) {
                throw new IllegalStateException("Concurrent upgrade modification");
            } else {
                currentState.set(committed);

                // Set the upgrade status to INACTIVE.
                Upgrade inactive = new Upgrade(
                        localVersion,
                        localVersion,
                        Upgrade.Status.INACTIVE);
                if (!state.compareAndSet(committed, inactive)) {
                    throw new IllegalStateException("Concurrent upgrade modification");
                } else {
                    currentState.set(inactive);
                }
            }
        }
    }

    @Override
    public void rollback() {
        Upgrade upgraded = currentState.get();

        // If the current upgrade status is not UPGRADED, throw an exception.
        if (upgraded.status() != Upgrade.Status.UPGRADED) {
            throw new IllegalStateException("Upgrade not performed");
        }

        // Set the upgrade status to ROLLING_BACK.
        Upgrade rollingBack = new Upgrade(
                upgraded.source(),
                upgraded.target(),
                Upgrade.Status.ROLLING_BACK);
        if (!state.compareAndSet(upgraded, rollingBack)) {
            throw new IllegalStateException("Concurrent upgrade modification");
        } else {
            currentState.set(rollingBack);

            // Set the upgrade status to ROLLED_BACK.
            Upgrade rolledBack = new Upgrade(
                    rollingBack.source(),
                    rollingBack.target(),
                    Upgrade.Status.ROLLED_BACK);
            if (!state.compareAndSet(rollingBack, rolledBack)) {
                throw new IllegalStateException("Concurrent upgrade modification");
            } else {
                currentState.set(rolledBack);
            }
        }
    }

    @Override
    public void reset() {
        Upgrade upgraded = currentState.get();

        // If the current upgrade status is not INITIALIZED or ROLLED_BACK, throw an exception.
        if (upgraded.status() != Upgrade.Status.INITIALIZED
                && upgraded.status() != Upgrade.Status.ROLLED_BACK) {
            throw new IllegalStateException("Upgrade not rolled back");
        }

        // Determine whether any nodes are still running the target version.
        boolean rollbackComplete = clusterService.getNodes()
                .stream()
                .allMatch(node -> {
                    ControllerNode.State state = clusterService.getState(node.id());
                    Version version = clusterService.getVersion(node.id());
                    return state.isActive() && version != null && version.equals(upgraded.source());
                });

        // If some nodes have not yet been downgraded, throw an exception.
        if (!rollbackComplete) {
            throw new IllegalStateException("Some nodes have not yet been downgraded to version " + upgraded.source());
        }

        // Set the upgrade status to RESETTING.
        Upgrade resetting = new Upgrade(
                upgraded.source(),
                upgraded.target(),
                Upgrade.Status.RESETTING);
        if (!state.compareAndSet(upgraded, resetting)) {
            throw new IllegalStateException("Concurrent upgrade modification");
        } else {
            currentState.set(resetting);

            // Set the upgrade status to RESET.
            Upgrade reset = new Upgrade(
                    resetting.source(),
                    resetting.target(),
                    Upgrade.Status.RESET);
            if (!state.compareAndSet(resetting, reset)) {
                throw new IllegalStateException("Concurrent upgrade modification");
            } else {
                currentState.set(reset);

                // Set the upgrade status to INACTIVE.
                Upgrade inactive = new Upgrade(
                        localVersion,
                        localVersion,
                        Upgrade.Status.INACTIVE);
                if (!state.compareAndSet(reset, inactive)) {
                    throw new IllegalStateException("Concurrent upgrade modification");
                } else {
                    currentState.set(inactive);
                }
            }
        }
    }

    private void handleChange(AtomicValueEvent<Upgrade> event) {
        currentState.set(event.newValue());
        switch (event.newValue().status()) {
            case INITIALIZED:
                post(new UpgradeEvent(UpgradeEvent.Type.INITIALIZED, event.newValue()));
                break;
            case UPGRADED:
                post(new UpgradeEvent(UpgradeEvent.Type.UPGRADED, event.newValue()));
                break;
            case COMMITTED:
                post(new UpgradeEvent(UpgradeEvent.Type.COMMITTED, event.newValue()));
                break;
            case ROLLED_BACK:
                post(new UpgradeEvent(UpgradeEvent.Type.ROLLED_BACK, event.newValue()));
                break;
            case RESET:
                post(new UpgradeEvent(UpgradeEvent.Type.RESET, event.newValue()));
                break;
            default:
                break;
        }
    }
}
