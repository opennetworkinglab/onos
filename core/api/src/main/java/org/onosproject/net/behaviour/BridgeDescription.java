/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import org.onosproject.net.Description;
import org.onosproject.net.DeviceId;

/**
 * The abstraction of bridge in OVSDB protocol.
 */
public interface BridgeDescription extends Description {

    /**
     * Returns bridge name.
     *
     * @return bridge name
     */
    BridgeName bridgeName();

    /**
     * Returns controller identifier that this bridge belongs to.
     *
     * @return controller identifier
     */
    DeviceId cotrollerDeviceId();

    /**
     * Returns bridge identifier .
     *
     * @return bridge identifier
     */
    DeviceId deviceId();
}
