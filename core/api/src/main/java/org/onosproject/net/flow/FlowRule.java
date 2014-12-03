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
package org.onosproject.net.flow;

import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;

/**
 * Represents a generalized match &amp; action pair to be applied to
 * an infrastructure device.
 */
public interface FlowRule extends BatchOperationTarget {

    static final int MAX_TIMEOUT = 60;
    static final int MIN_PRIORITY = 0;

    //TODO: build cookie value
    /**
     * Returns the ID of this flow.
     *
     * @return the flow ID
     */
    FlowId id();

    /**
     * Returns the application id of this flow.
     *
     * @return an applicationId
     */
    short appId();

    /**
     * Returns the group id of this flow.
     *
     * @return an groupId
     */
    GroupId groupId();

    /**
     * Returns the flow rule priority given in natural order; higher numbers
     * mean higher priorities.
     *
     * @return flow rule priority
     */
    int priority();

    /**
     * Returns the identity of the device where this rule applies.
     *
     * @return device identifier
     */
    DeviceId deviceId();

    /**
     * Returns the traffic selector that identifies what traffic this
     * rule should apply to.
     *
     * @return traffic selector
     */
    TrafficSelector selector();

    /**
     * Returns the traffic treatment that applies to selected traffic.
     *
     * @return traffic treatment
     */
    TrafficTreatment treatment();

    /**
     * Returns the timeout for this flow requested by an application.
     *
     * @return integer value of the timeout
     */
    int timeout();

    /**
     * Returns whether the flow is permanent i.e. does not time out.
     *
     * @return true if the flow is permanent, otherwise false
     */
    boolean isPermanent();

}
