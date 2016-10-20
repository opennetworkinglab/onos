/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.virtual.provider;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterOperations;

/**
 * Abstraction of a Meter provider for virtual network.
 */
public interface VirtualMeterProvider extends VirtualProvider {
    /**
     * Performs a batch of meter operation on the specified virtual device
     * with the specified parameters.
     *
     * @param networkId the identity of the virtual network where this rule applies
     * @param deviceId device identifier on which the batch of group
     * operations to be executed
     * @param meterOps immutable list of meter operation
     */
    void performMeterOperation(NetworkId networkId, DeviceId deviceId,
                               MeterOperations meterOps);


    /**
     * Performs a meter operation on the specified vitual device with the
     * specified parameters.
     *
     * @param networkId the identity of the virtual network where this rule applies
     * @param deviceId device identifier on which the batch of group
     * operations to be executed
     * @param meterOp a meter operation
     */
    void performMeterOperation(NetworkId networkId, DeviceId deviceId,
                               MeterOperation meterOp);
}
