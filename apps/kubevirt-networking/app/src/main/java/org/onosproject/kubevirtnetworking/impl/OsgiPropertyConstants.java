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

package org.onosproject.kubevirtnetworking.impl;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    static final String PROVIDER_NETWORK_ONLY = "providerNetworkOnly";
    static final boolean PROVIDER_NETWORK_ONLY_DEFAULT = true;

    static final String DHCP_SERVER_MAC = "dhcpServerMac";
    static final String DHCP_SERVER_MAC_DEFAULT = "fe:00:00:00:00:02";

    static final String USE_SECURITY_GROUP = "useSecurityGroup";
    static final boolean USE_SECURITY_GROUP_DEFAULT = true;
}
