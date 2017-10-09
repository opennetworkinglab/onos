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
package org.onosproject.upgrade;

import com.google.common.annotations.Beta;
import org.onosproject.core.Version;
import org.onosproject.event.ListenerService;

/**
 * Upgrade service.
 */
@Beta
public interface UpgradeService
        extends ListenerService<UpgradeEvent, UpgradeEventListener> {

    /**
     * Returns the current upgrade state.
     *
     * @return the current upgrade state
     */
    Upgrade getState();

    /**
     * Returns whether an upgrade is in progress.
     * <p>
     * An upgrade is in progress if the upgrade {@link Upgrade.Status} is active, e.g.
     * {@link Upgrade.Status#INITIALIZED}, {@link Upgrade.Status#UPGRADED}, etc.
     *
     * @return indicates whether an upgrade is in progress
     */
    boolean isUpgrading();

    /**
     * Returns the currently active software version.
     * <p>
     * The returned version is representative of the version currently in control of the network. When the upgrade
     * transitions to the {@link Upgrade.Status#UPGRADING} state, control over the network is transferred from
     * {@link Upgrade#source()} nodes to {@link Upgrade#target()} nodes, and the version returned by this method
     * represents that change.
     *
     * @return the software version
     */
    Version getVersion();

    /**
     * Returns whether the local node is active.
     * <p>
     * The local node will be active if its {@link Version} matches the version returned by {@link #getVersion()}.
     *
     * @return indicates whether the local node is active according to its version
     */
    boolean isLocalActive();

    /**
     * Returns whether the local node is an upgraded node.
     *
     * @return {@code true} if the local node's version matches {@link Upgrade#target()}
     */
    boolean isLocalUpgraded();

}
