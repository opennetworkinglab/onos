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

package org.onosproject.p4runtime.api;

import com.google.common.annotations.Beta;
import org.onosproject.event.ListenerService;
import org.onosproject.grpc.api.GrpcClientController;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.provider.ProviderId;

/**
 * Controller of P4Runtime devices.
 */
@Beta
public interface P4RuntimeController
        extends GrpcClientController<P4RuntimeClientKey, P4RuntimeClient>,
                ListenerService<P4RuntimeEvent, P4RuntimeEventListener> {
    /**
     * Adds a listener for device agent events for the given provider.
     *
     * @param deviceId device identifier
     * @param providerId provider ID
     * @param listener the device agent listener
     */
    void addDeviceAgentListener(DeviceId deviceId, ProviderId providerId,
                                DeviceAgentListener listener);

    /**
     * Removes the listener for device agent events that was previously
     * registered for the given provider.
     *
     * @param deviceId   device identifier
     * @param providerId the provider ID
     */
    void removeDeviceAgentListener(DeviceId deviceId, ProviderId providerId);
}
