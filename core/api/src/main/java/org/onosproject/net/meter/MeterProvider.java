/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.meter;

import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.Provider;

/**
 * Abstraction of a Meter provider.
 */
public interface MeterProvider extends Provider {
    /**
     * Meter capable property name.
     * A driver is assumed to be meter capable if this property is undefined.
     */
    String METER_CAPABLE = "meterCapable";

    /**
     * Performs a batch of meter operation on the specified device with the
     * specified parameters.
     *
     * @param deviceId device identifier on which the batch of group
     * operations to be executed
     * @param meterOps immutable list of meter operation
     */
    void performMeterOperation(DeviceId deviceId,
                               MeterOperations meterOps);


    /**
     * Performs a meter operation on the specified device with the
     * specified parameters.
     *
     * @param deviceId device identifier on which the batch of group
     * operations to be executed
     * @param meterOp a meter operation
     */
    void performMeterOperation(DeviceId deviceId,
                               MeterOperation meterOp);
}
