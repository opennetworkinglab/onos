/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;

/**
 * Test adapter for OpenstackFlowRuleService.
 */
public class OpenstackFlowRuleServiceAdapter implements OpenstackFlowRuleService {
    @Override
    public void setRule(ApplicationId appId, DeviceId deviceId, TrafficSelector selector,
                        TrafficTreatment treatment, int priority, int tableType, boolean install) {

    }

    @Override
    public void setUpTableMissEntry(DeviceId deviceId, int table) {

    }

    @Override
    public void connectTables(DeviceId deviceId, int fromTable, int toTable) {

    }
}
