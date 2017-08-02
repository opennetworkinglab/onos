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
package org.onosproject.d.config.sync;

import java.util.concurrent.CompletableFuture;

import org.onosproject.d.config.sync.operation.SetRequest;
import org.onosproject.d.config.sync.operation.SetResponse;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.Provider;

import com.google.common.annotations.Beta;

// TODO might want to remove Device~ prefix, class name too long.
/**
 * Abstraction of a device config synchronization provider.
 * <p>
 * Provides a mean for propagating dynamic config triggered change down to
 * the device.
 */
@Beta
public interface DeviceConfigSynchronizationProvider extends Provider {

    // TODO API to propagate dynamic config subsystem side change down to the
    // device

    /**
     * Requests a device to set configuration as specified.
     *
     * @param deviceId target Device identifier
     * @param request configuration requests
     * @return result
     */
    CompletableFuture<SetResponse> setConfiguration(DeviceId deviceId, SetRequest request);

    // TODO API for Get from Device
    // CompletableFuture<GetResponse> getConfiguration(DeviceId deviceId, GetRequest request);

}
