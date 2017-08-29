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

package org.onosproject.l3vpn.netl3vpn;

/**
 * Represents the model id level to add it in the store.
 * //TODO: Further levels has to be added.
 */
public enum ModelIdLevel {

    /**
     * Requested model id level is not present, representing top node.
     */
    ROOT,

    /**
     * Requested model id level is devices container.
     */
    DEVICES,

    /**
     * Requested model id level is device list.
     */
    DEVICE,

    /**
     * Requested model id level is VPN list.
     */
    VPN,

    /**
     * Requested model id level is tunnel manager.
     */
    TNL_M,

    /**
     * Requested model id level is tunnel policy.
     */
    TNL_POL,

    /**
     * Requested model id level is tunnel hop.
     */
    TP_HOP
}
