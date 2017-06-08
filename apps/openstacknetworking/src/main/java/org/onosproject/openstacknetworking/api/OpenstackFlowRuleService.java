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
}
