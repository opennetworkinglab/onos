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

import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.MembershipService;
import org.onosproject.cluster.NodeId;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.CLUSTER_EVENT;
import static org.onosproject.security.AppPermission.Type.UPGRADE_EVENT;
import static org.onosproject.security.AppPermission.Type.UPGRADE_READ;
import static org.onosproject.security.AppPermission.Type.UPGRADE_WRITE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Upgrade service implementation.
 * <p>
 * This implementation uses the {@link CoordinationService} to store upgrade state in a version-agnostic primitive.
 * Upgrade state can be seen by current and future version nodes.
 */
@Component(immediate = true, service = { UpgradeService.class, UpgradeAdminService.class })
public class UpgradeManager
        extends AbstractListenerManager<UpgradeEvent, UpgradeEventListener>
        implements UpgradeService, UpgradeAdminService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected VersionService versionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoordinationService coordinationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MembershipService membershipService;

    private Version localVersion;
    private AtomicValue<Upgrade> state;
    private final AtomicReference<Upgrade> currentState = new AtomicReference<>();
    private final AtomicValueEventListener<Upgrade> stateListener = this::handleUpgradeEvent;
    private final ClusterEventListener clusterListener = this::handleClusterEvent;

    @Activate
    public void activate() {
        eventDispatcher.addSink(UpgradeEvent.class, listenerRegistry);

        state = coordinationService.<Upgrade>atomicValueBuilder()
                .withName("onos-upgrade-state")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asAtomicValue();
        localVersion = versionService.version();

        currentState.set(state.get());
        if (getState() == null) {
            initializeState(new Upgrade(localVersion, localVersion, Upgrade.Status.INACTIVE));
        }

        Upgrade upgrade = getState();

        // If the upgrade state is initialized then check the node version.
        if (upgrade.status() == Upgrade.Status.INITIALIZED) {
            // If the source version equals the target version, attempt to update the target version.
            if (Objects.equals(upgrade.source(), upgrade.target()) && !Objects.equals(upgrade.target(), localVersion)) {
                checkPermission(UPGRADE_WRITE);
                upgrade = new Upgrade(upgrade.source(), localVersion, upgrade.status());
                initializeState(upgrade);
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
        clusterService.addListener(clusterListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(UpgradeEvent.class);
        state.removeListener(stateListener);
        clusterService.removeListener(clusterListener);
        log.info("Stopped");
    }

    /**
     * Initializes the state when the cluster starts.
     * <p>
     * This method must be called when updating the state in order to check the permissions
     *
     * @param newState new state
     */
    private void initializeState(Upgrade newState) {
        checkPermission(UPGRADE_WRITE);
        currentState.set(newState);
        state.set(newState);
    }

    /**
     * Changes the current state to new one.
     * <p>
     * This method must be called when changing between states in order to check the permissions and
     * to avoid concurrent state modifications
     *
     * @param oldState current upgrade state
     * @param newState new upgrade state
     *
     * @throws IllegalStateException if an upgrade is already in progress
     */
    private void changeState(Upgrade oldState, Upgrade newState) {
        checkPermission(UPGRADE_WRITE);
        if (!state.compareAndSet(oldState, newState)) {
            throw new IllegalStateException("Concurrent upgrade modification");
        } else {
            currentState.set(newState);
        }
    }

    @Override
    public Upgrade getState() {
        checkPermission(UPGRADE_READ);
        return currentState.get();
    }

    @Override
    public boolean isUpgrading() {
        return getState().status().active();
    }

    @Override
    public Version getVersion() {
        Upgrade upgrade = getState();
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
        Upgrade upgrade = getState();
        return upgrade.status().active()
                && !upgrade.source().equals(upgrade.target())
                && localVersion.equals(upgrade.target());
    }

    @Override
    public void initialize() {
        Upgrade inactive = getState();

        // If the current upgrade status is active, fail initialization.
        if (inactive.status().active()) {
            throw new IllegalStateException("Upgrade already active");
        }

        // Set the upgrade status to INITIALIZING.
        Upgrade initializing = new Upgrade(
                localVersion,
                localVersion,
                Upgrade.Status.INITIALIZING);
        changeState(inactive, initializing);

        // Set the upgrade status to INITIALIZED.
        Upgrade initialized = new Upgrade(
                initializing.source(),
                initializing.target(),
                Upgrade.Status.INITIALIZED);
        changeState(initializing, initialized);
    }

    @Override
    public void upgrade() {
        Upgrade initialized = getState();

        // If the current upgrade status is not INITIALIZED, throw an exception.
        if (initialized.status() != Upgrade.Status.INITIALIZED) {
            throw new IllegalStateException("Upgrade not initialized");
        }

        // Set the upgrade status to UPGRADING.
        Upgrade upgrading = new Upgrade(
                initialized.source(),
                initialized.target(),
                Upgrade.Status.UPGRADING);
        changeState(initialized, upgrading);

        // Set the upgrade status to UPGRADED.
        Upgrade upgraded = new Upgrade(
                upgrading.source(),
                upgrading.target(),
                Upgrade.Status.UPGRADED);
        changeState(upgrading, upgraded);
    }

    @Override
    public void commit() {
        Upgrade upgraded = getState();

        // If the current upgrade status is not UPGRADED, throw an exception.
        if (upgraded.status() != Upgrade.Status.UPGRADED) {
            throw new IllegalStateException("Upgrade not performed");
        }

        // Determine whether any nodes have not been upgraded to the target version.
        boolean upgradeComplete = membershipService.getGroups().size() == 1
                && membershipService.getLocalGroup().version().equals(upgraded.target());

        // If some nodes have not yet been upgraded, throw an exception.
        if (!upgradeComplete) {
            throw new IllegalStateException("Some nodes have not yet been upgraded to version " + upgraded.target());
        }

        // Set the upgrade status to COMMITTING.
        Upgrade committing = new Upgrade(
                upgraded.source(),
                upgraded.target(),
                Upgrade.Status.COMMITTING);
        changeState(upgraded, committing);

        // Set the upgrade status to COMMITTED.
        Upgrade committed = new Upgrade(
                committing.source(),
                committing.target(),
                Upgrade.Status.COMMITTED);
        changeState(committing, committed);

        // Set the upgrade status to INACTIVE.
        Upgrade inactive = new Upgrade(
                localVersion,
                localVersion,
                Upgrade.Status.INACTIVE);
        changeState(committed, inactive);
    }

    @Override
    public void rollback() {
        Upgrade upgraded = getState();

        // If the current upgrade status is not UPGRADED, throw an exception.
        if (upgraded.status() != Upgrade.Status.UPGRADED) {
            throw new IllegalStateException("Upgrade not performed");
        }

        // Set the upgrade status to ROLLING_BACK.
        Upgrade rollingBack = new Upgrade(
                upgraded.source(),
                upgraded.target(),
                Upgrade.Status.ROLLING_BACK);
        changeState(upgraded, rollingBack);

        // Set the upgrade status to ROLLED_BACK.
        Upgrade rolledBack = new Upgrade(
                rollingBack.source(),
                rollingBack.target(),
                Upgrade.Status.ROLLED_BACK);
        changeState(rollingBack, rolledBack);
    }

    @Override
    public void reset() {
        Upgrade upgraded = getState();

        // If the current upgrade status is not INITIALIZED or ROLLED_BACK, throw an exception.
        if (upgraded.status() != Upgrade.Status.INITIALIZED
                && upgraded.status() != Upgrade.Status.ROLLED_BACK) {
            throw new IllegalStateException("Upgrade not rolled back");
        }

        // Determine whether any nodes are still running the target version.
        boolean rollbackComplete = membershipService.getGroups().size() == 1
                && membershipService.getLocalGroup().version().equals(upgraded.source());

        // If some nodes have not yet been downgraded, throw an exception.
        if (!rollbackComplete) {
            throw new IllegalStateException("Some nodes have not yet been downgraded to version " + upgraded.source());
        }

        // Set the upgrade status to RESETTING.
        Upgrade resetting = new Upgrade(
                upgraded.source(),
                upgraded.target(),
                Upgrade.Status.RESETTING);
        changeState(upgraded, resetting);

        // Set the upgrade status to RESET.
        Upgrade reset = new Upgrade(
                resetting.source(),
                resetting.target(),
                Upgrade.Status.RESET);
        changeState(resetting, reset);

        // Set the upgrade status to INACTIVE.
        Upgrade inactive = new Upgrade(
                localVersion,
                localVersion,
                Upgrade.Status.INACTIVE);
        changeState(reset, inactive);
    }

    /**
     * Handles a cluster event.
     *
     * @param event the cluster event
     */
    protected void handleClusterEvent(ClusterEvent event) {
        checkPermission(CLUSTER_EVENT);
        // If an instance was deactivated, check whether we need to roll back the upgrade.
        if (event.type() == ClusterEvent.Type.INSTANCE_DEACTIVATED) {
            Upgrade upgrade = getState();
            if (upgrade.status().upgraded()) {
                // Get the upgraded subset of the cluster and check whether the down node is a member
                // of the upgraded subset. If so, roll back the upgrade to tolerate the failure.
                Set<NodeId> upgradedNodes = clusterService.getNodes().stream()
                        .map(ControllerNode::id)
                        .filter(id -> clusterService.getVersion(id).equals(upgrade.target()))
                        .collect(Collectors.toSet());
                if (upgradedNodes.contains(event.subject().id())) {
                    log.warn("Upgrade failure detected: rolling back upgrade");
                    rollback();
                }
            }
        }
    }

    /**
     * Handles an upgrade state event.
     *
     * @param event the upgrade value event
     */
    protected void handleUpgradeEvent(AtomicValueEvent<Upgrade> event) {
        checkPermission(UPGRADE_EVENT);
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
