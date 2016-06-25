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

package org.onosproject.net.device;

import org.onosproject.net.driver.HandlerBehaviour;

import java.util.List;

/**
 * Handler behaviour capable of creating device and port descriptions.
 * These descriptions should be appropriately annotated to support downstream
 * projections of the respective devices and their ports.
 */
public interface DeviceDescriptionDiscovery extends HandlerBehaviour {

    /**
     * Returns a device description appropriately annotated to support
     * downstream model extension via projections of the resulting device,
     * as in the following example.
     * <pre>
     * MicrowaveDevice device = deviceService.get(id).as(MicrowaveDevice.class);
     * </pre>
     *
     * @return annotated device description
     */
    DeviceDescription discoverDeviceDetails();

    /**
     * Returns a list of port descriptions appropriately annotated to support
     * downstream model extension via projections of their parent device,
     * as in the following example.
     * <pre>
     * MicrowaveDevice device = deviceService.get(id).as(MicrowaveDevice.class);
     * List&lt;MicrowavePort&gt; ports = device.microwavePorts(deviceService.getPorts(id));
     * </pre>
     *
     * @return annotated device description
     */
    List<PortDescription> discoverPortDetails();

}
