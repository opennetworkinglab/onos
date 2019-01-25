/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

/**
 * Service for setting flow rules.
 */
public interface K8sFlowRuleService {

    /**
     * Configure the flow rule.
     *
     * @param appId         application ID
     * @param deviceId      device ID
     * @param selector      traffic selector used for match header fields
     * @param treatment     traffic treatment for take actions for matched packets
     * @param priority      rule priority
     * @param tableType     table number to install flow rules
     * @param install       true for rule addition, false for rule removal
     */
    void setRule(ApplicationId appId, DeviceId deviceId,
                 TrafficSelector selector, TrafficTreatment treatment,
                 int priority, int tableType, boolean install);

    /**
     * Installs table miss entry (drop rule) for the given flow table.
     *
     * @param deviceId      device ID
     * @param table         table number
     */
    void setUpTableMissEntry(DeviceId deviceId, int table);

    /**
     * Installs a flow rule for transiting from table A to table B.
     *
     * @param deviceId      device ID
     * @param fromTable     table number of table A
     * @param toTable       table number of table B
     */
    void connectTables(DeviceId deviceId, int fromTable, int toTable);
}
