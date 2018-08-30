/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.net.pi.service;

import com.google.common.annotations.Beta;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPipeconfId;

/**
 * Service that periodically probes pipeline programmable devices, to check that
 * their pipeline is configured with the expected pipeconf. It emits events
 * about pipeline status changes.
 */
@Beta
public interface PiPipeconfWatchdogService
        extends ListenerService<PiPipeconfWatchdogEvent, PiPipeconfWatchdogListener> {

    /**
     * Status of a device pipeline.
     */
    enum PipelineStatus {
        /**
         * The device pipeline is ready to process packets.
         */
        READY,
        /**
         * The status is unknown and the device might not be able to process
         * packets yet.
         */
        UNKNOWN,
    }

    /**
     * Asynchronously triggers a probe task that checks the device pipeline
     * status and, if required, configures it with the pipeconf associated to
     * this device (via {@link PiPipeconfService#bindToDevice(PiPipeconfId,
     * DeviceId)}).
     *
     * @param deviceId device to probe
     */
    void triggerProbe(DeviceId deviceId);

    /**
     * Returns the last known pipeline status of the given device.
     *
     * @param deviceId device ID
     * @return pipeline status
     */
    PipelineStatus getStatus(DeviceId deviceId);
}
