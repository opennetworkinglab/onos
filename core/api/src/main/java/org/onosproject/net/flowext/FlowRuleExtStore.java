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
package org.onosproject.net.flowext;

import java.util.Collection;
import java.util.concurrent.Future;

import org.onosproject.net.DeviceId;
import org.onosproject.store.Store;

import com.esotericsoftware.kryo.Serializer;

/**
 * Manages inventory of flow rules; not intended for direct use.
 */
public interface FlowRuleExtStore extends Store<FlowRuleBatchExtEvent, FlowRuleExtStoreDelegate> {

    /**
     * Stores a batch of flow extension rules.
     *
     * @param batchOperation batch of flow rules.
     *           A batch can contain flow rules for a single device only.
     * @return Future response indicating success/failure of the batch operation
     * all the way down to the device.
     */
    Future<FlowExtCompletedOperation> storeBatch(Collection<FlowRuleExtEntry> batchOperation);

    /**
     * Invoked on the completion of a storeBatch operation.
     *
     * @param event flow rule batch event
     */
    void batchOperationComplete(FlowRuleBatchExtEvent event);


    /**
     * @param deviceId the device ID
     * @param classT the flowEntryExtension of which classT you want to get
     * @return message parsed from byte stream
     */
    Iterable<?> getExtMessages(DeviceId deviceId, Class<?> classT);

    /**
     * @param classT the class flowEntryExtension can be decoded to.
     * @param serializer the serializer apps provide using to decode flowEntryExtension
     */    
    void registerSerializer(Class<?> classT, Serializer<?> serializer);
}
