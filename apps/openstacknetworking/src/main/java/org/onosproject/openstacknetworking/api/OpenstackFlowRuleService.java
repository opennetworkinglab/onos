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
package org.onosproject.openstacknetworking.api;


import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

/**
 * Service for setting flow rules.
 *
 */
public interface OpenstackFlowRuleService {
    /**
     * Sets the flow rule.
     *
     * @param appId application ID
     * @param deviceId  device ID
     * @param selector matches of the flow rule
     * @param treatment actions of the flow rule
     * @param priority priority of the flow rule
     * @param tableType table number to put the flow rule
     * @param install add the rule if true, remove it otherwise
     */
    void setRule(ApplicationId appId,
              DeviceId deviceId,
              TrafficSelector selector,
              TrafficTreatment treatment,
              int priority,
              int tableType,
              boolean install);

    /**
     * Install table miss entry (drop rule) in the table.
     *
     * @param deviceId device ID
     * @param table table number
     */
    void setUpTableMissEntry(DeviceId deviceId, int table);

    /**
     * Install a flor rule for transition from table A to table B.
     *
     * @param deviceId device Id
     * @param fromTable table number of table A
     * @param toTable table number of table B
     */
    void connectTables(DeviceId deviceId, int fromTable, int toTable);
}
