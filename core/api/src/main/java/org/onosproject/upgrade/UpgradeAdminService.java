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

/**
 * Abstraction for executing the stages of the upgrade process.
 * <p>
 * Upgrades are performed in three phases:
 * <ul>
 *     <li>{@code initialize} - Initializes an upgrade</li>
 *     <li>{@code upgrade} - Performs an upgrade, transferring device mastership from the current version to the
 *     upgraded version</li>
 *     <li>{@code commit} or {@code rollback} - Completes or rolls back an upgrade, transferring mastership back
 *     to nodes running the previous version</li>
 * </ul>
 */
@Beta
public interface UpgradeAdminService {

    /**
     * Initializes an upgrade.
     * <p>
     * This method must be called to initialize an upgrade and prior to physically upgrading any nodes.
     *
     * @throws IllegalStateException if an upgrade is already in progress
     */
    void initialize();

    /**
     * Performs an upgrade, transferring device mastership to upgraded nodes.
     * <p>
     * This method transfers mastership from the current version of the software to the upgraded version. Thus,
     * a subset of the nodes in the cluster must have been physically upgraded and restarted prior to executing this
     * phase of the upgrade protocol.
     *
     * @throws IllegalStateException if no upgrade has been initialized
     */
    void upgrade();

    /**
     * Commits an upgrade.
     * <p>
     * Completes the upgrade process, committing the new cluster version.
     *
     * @throws IllegalStateException if no upgrade is in progress or not all nodes have been upgraded
     */
    void commit();

    /**
     * Rolls back an upgrade.
     * <p>
     * When an upgrade is rolled back, mastership is transferred from upgraded nodes back to nodes running the
     * version of the software prior to the upgrade.
     *
     * @throws IllegalStateException if no upgrade is in progress
     */
    void rollback();

    /**
     * Resets an upgrade.
     * <p>
     * When an upgrade is rolled back, once nodes have been restored to the previos version the upgrade must be reset
     * to restore the upgrade state to {@link Upgrade.Status#INACTIVE}.
     *
     * @throws IllegalStateException if nodes have not been restored to the previous state
     */
    void reset();

}
