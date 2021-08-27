/*
 * Copyright 2017-present Open Networking Foundation
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

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.Collection;

/**
 * Testing adapter for the meter service interface.
 */
public class MeterServiceAdapter implements MeterService {
    @Override
    public Meter submit(MeterRequest meter) {
        return null;
    }

    @Override
    public void withdraw(MeterRequest meter, MeterId meterId) {

    }

    @Override
    public void withdraw(MeterRequest meter, MeterCellId meterCellId) {

    }

    @Override
    public Meter getMeter(DeviceId deviceId, MeterId id) {
        return null;
    }

    @Override
    public Meter getMeter(DeviceId deviceId, MeterCellId meterCellId) {
        return null;
    }

    @Override
    public Collection<Meter> getAllMeters() {
        return null;
    }

    @Override
    public Collection<Meter> getMeters(DeviceId deviceId) {
        return null;
    }

    @Override
    public Collection<Meter> getMeters(DeviceId deviceId, MeterScope scope) {
        return null;
    }

    @Override
    public MeterId allocateMeterId(DeviceId deviceId) {
        return null;
    }

    @Override
    public void freeMeterId(DeviceId deviceId, MeterId meterId) {

    }

    @Override
    public void purgeMeters(DeviceId deviceId) {

    }

    @Override
    public void purgeMeters(DeviceId deviceId, ApplicationId appId) {

    }

    @Override
    public void addListener(MeterListener listener) {

    }

    @Override
    public void removeListener(MeterListener listener) {

    }
}
