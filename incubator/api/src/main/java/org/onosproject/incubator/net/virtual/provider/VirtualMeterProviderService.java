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

import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterFailReason;
import org.onosproject.net.meter.MeterOperation;

import java.util.Collection;

/**
 * Service through which meter providers can inject information
 * into the virtual network subsystem core.
 */
public interface VirtualMeterProviderService
        extends VirtualProviderService<VirtualMeterProvider> {
    /**
     * Notifies the core that a meter operation failed for a
     * specific reason.
     *
     * @param operation the failed operation
     * @param reason the failure reason
     */
    void meterOperationFailed(MeterOperation operation, MeterFailReason reason);

    /**
     * Pushes the collection of meters observed on the virtual data plane as
     * well as their associated statistics.
     *
     * @param deviceId a device id
     * @param meterEntries a collection of meter entries
     */
    void pushMeterMetrics(DeviceId deviceId, Collection<Meter> meterEntries);
}
