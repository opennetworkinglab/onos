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
package org.onosproject.net.flowextend;

import java.util.Collection;
import java.util.concurrent.Future;
import org.onosproject.net.DeviceId;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Service for injecting extended flow rules into the environment. This implements
 * semantics of a distributed authoritative flow table where the master copy
 * of the flow rules lies with the controller and the devices hold only the
 * 'cached' copy.
 */
public interface FlowRuleExtendService {

    /**
     * Returns the collection of flow entries applied on the specified device.
     * This will include flow rules which may not yet have been applied to
     * the device.
     *
     * @param deviceId device identifier
     * @return collection of flow rules
     */
    Iterable<FlowRuleExtendEntry> getFlowEntries(DeviceId deviceId);

    /**
     * Applies a batch operation of FlowRules.
     *
     * @param batch batch operation to apply
     * @return future indicating the state of the batch operation
     */
    Future<FlowExtendCompletedOperation> applyBatch(Collection<FlowRuleExtendEntry> batch);

    /**
     * Adds the specified flow rule listener.
     *
     * @param listener flow rule listener
     */
    void addListener(FlowRuleExtendListener listener);

    /**
     * Removes the specified flow rule listener.
     *
     * @param listener flow rule listener
     */
    void removeListener(FlowRuleExtendListener listener);

    /**
     * @param deviceId the device ID
     * @return message partly parsed from OF1.4 -> OF1.3, lost some info
     */
    Iterable<OFMessage> getOFMessages(DeviceId fpid);
}
