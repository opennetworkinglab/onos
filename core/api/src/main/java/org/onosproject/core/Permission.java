/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.core;

/**
 * Representation of an application permission.
 */
public enum Permission {
    APP_READ,
    APP_EVENT,
    CONFIG_READ,
    CONFIG_WRITE,
    CLUSTER_READ,
    CLUSTER_WRITE,
    CLUSTER_EVENT,
    DEVICE_READ,
    DEVICE_EVENT,
    DRIVER_READ,
    DRIVER_WRITE,
    FLOWRULE_READ,
    FLOWRULE_WRITE,
    FLOWRULE_EVENT,
    GROUP_READ,
    GROUP_WRITE,
    GROUP_EVENT,
    HOST_READ,
    HOST_WRITE,
    HOST_EVENT,
    INTENT_READ,
    INTENT_WRITE,
    INTENT_EVENT,
    LINK_READ,
    LINK_WRITE,
    LINK_EVENT,
    PACKET_READ,
    PACKET_WRITE,
    PACKET_EVENT,
    STATISTIC_READ,
    TOPOLOGY_READ,
    TOPOLOGY_EVENT,
    TUNNEL_READ,
    TUNNEL_WRITE,
    TUNNEL_EVENT,
    STORAGE_WRITE
}
