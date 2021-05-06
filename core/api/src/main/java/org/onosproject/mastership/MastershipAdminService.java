/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.mastership;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.onlab.util.Tools;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;

/**
 * Service for administering the inventory of device masterships.
 */
public interface MastershipAdminService {

    long TIMEOUT_MILLIS = 3000;

    /**
     * Applies the current mastership role for the specified device.
     *
     * @param instance controller instance identifier
     * @param deviceId device identifier
     * @param role     requested role
     * @return future that is completed when the role is set
     */
    CompletableFuture<Void> setRole(NodeId instance, DeviceId deviceId, MastershipRole role);

    /**
     * Synchronous version of setRole.
     * Applies the current mastership role for the specified device.
     *
     * @param instance controller instance identifier
     * @param deviceId device identifier
     * @param role     requested role
     */
    default void setRoleSync(NodeId instance, DeviceId deviceId, MastershipRole role) {
        Tools.futureGetOrElse(setRole(instance, deviceId, role), TIMEOUT_MILLIS, TimeUnit.MILLISECONDS, null);
    }

    /**
     * Balances the mastership to be shared as evenly as possibly by all
     * online instances.
     */
    void balanceRoles();

    /**
     * Attempts to demote a node to the bottom of the backup list. It is not allowed
     * to demote the current master
     *
     * @param instance controller instance identifier
     * @param deviceId device identifier
     */
    void demote(NodeId instance, DeviceId deviceId);

}
