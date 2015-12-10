/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.provider.snmp.device.impl;
    /**
     * The Device State is used to determine whether the device is active or inactive. This state information will help
     * Device Creator to add or delete the device from the core.
     */
    public  enum DeviceState {
        /* Used to specify Active state of the device */

        ACTIVE,
        /* Used to specify inactive state of the device */
        INACTIVE,
        /* Used to specify invalid state of the device */
        INVALID
    }
