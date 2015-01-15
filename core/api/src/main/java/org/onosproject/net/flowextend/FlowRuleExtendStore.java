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
import org.onosproject.store.Store;
import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Manages inventory of flow rules; not intended for direct use.
 */
public interface FlowRuleExtendStore extends Store<FlowRuleBatchExtendEvent, FlowRuleExtendStoreDelegate> {
    /**
     * Returns the flow entries associated with a device.
     *
     * @param deviceId the device ID
     * @return the flow entries
     */
    Iterable<FlowRuleExtendEntry> getFlowEntries(DeviceId deviceId);

    /**
     * Stores a batch of flow rules.
     *
     * @param batchOperation batch of flow rules.
     *           A batch can contain flow rules for a single device only.
     * @return Future response indicating success/failure of the batch operation
     * all the way down to the device.
     */
    Future<FlowExtendCompletedOperation> storeBatch(Collection<FlowRuleExtendEntry> batchOperation);

    /**
     * Invoked on the completion of a storeBatch operation.
     *
     * @param event flow rule batch event
     */
    void batchOperationComplete(FlowRuleBatchExtendEvent event);

    /**
     * @param deviceId the device ID
     * @param message partly parsed from OF1.4 -> OF1.3, lost some info
     */
    void storeFlowRule(DeviceId deviceId, OFMessage message);

    /**
     * @param deviceId the device ID
     * @return message partly parsed from OF1.4 -> OF1.3, lost some info
     */
    Iterable<OFMessage> getOFMessages(DeviceId fpid);
}
