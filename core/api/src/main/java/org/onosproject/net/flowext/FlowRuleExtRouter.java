/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.flowext;

import org.onosproject.net.flow.FlowRuleBatchEvent;
import org.onosproject.net.flow.FlowRuleBatchRequest;

import java.util.concurrent.Future;

/**
 * Experimental extension to the flow rule subsystem; still under development.
 * Represents a router-like mechanism which is in charge of sending flow rule to master;
 * <p>
 * The Router is in charge of sending flow rule to master;
 * the core component of routing-like mechanism.
 * </p>
 */
public interface FlowRuleExtRouter {

    /**
     * apply the sub batch of flow extension rules.
     *
     * @param batchOperation batch of flow rules.
     *                       A batch can contain flow rules for a single device only.
     * @return Future response indicating success/failure of the batch operation
     * all the way down to the device.
     */
    Future<FlowExtCompletedOperation> applySubBatch(FlowRuleBatchRequest batchOperation);

    /**
     * Invoked on the completion of a storeBatch operation.
     *
     * @param event flow rule batch event
     */
    void batchOperationComplete(FlowRuleBatchEvent event);

    /**
     * Register the listener to monitor Router,
     * The Router find master to send downStream.
     *
     * @param listener the listener to register
     */
    public void addListener(FlowRuleExtRouterListener listener);

    /**
     * Remove the listener of Router.
     *
     * @param listener the listener to remove
     */
    public void removeListener(FlowRuleExtRouterListener listener);
}
