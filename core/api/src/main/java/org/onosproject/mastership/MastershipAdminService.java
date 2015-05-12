/*
 * Copyright 2014 Open Networking Laboratory
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

import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;

/**
 * Service for administering the inventory of device masterships.
 */
public interface MastershipAdminService {

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
     * Balances the mastership to be shared as evenly as possibly by all
     * online instances.
     */
    void balanceRoles();

}
