/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.iptopology.api.device;

import org.onosproject.iptopology.api.DeviceTed;
import org.onosproject.iptopology.api.IpDevice;
import org.onosproject.iptopology.api.IpDeviceIdentifier;
import org.onosproject.net.Description;


import java.net.URI;

/**
 * Carrier of immutable information about an ip device.
 */
public interface IpDeviceDescription extends Description {

    /**
     * Protocol/provider specific URI that can be used to encode the identity
     * information required to communicate with the ip device externally, e.g.
     * datapath ID.
     *
     * @return provider specific URI for the ip device
     */
    URI deviceUri();

    /**
     * Returns the type of the ip device. For ex: Psuedo or actual
     *
     * @return type of the device
     */
    IpDevice.Type type();

    /**
     * Returns the device identifier details.
     *
     * @return identifier of the device
     */
    IpDeviceIdentifier deviceIdentifier();

    /**
     * Returns the traffic engineering parameters of the device.
     *
     * @return traffic engineering parameters of the device
     */
    DeviceTed deviceTed();

}
