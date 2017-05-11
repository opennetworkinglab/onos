/*
 * Copyright 2017-present Open Networking Laboratory
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
 * Represents the model id level of BGP information to be added to store.
 * //TODO: Further more levels of BGP addition has to be added.
 */
public enum BgpModelIdLevel {

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
    VPN
}
