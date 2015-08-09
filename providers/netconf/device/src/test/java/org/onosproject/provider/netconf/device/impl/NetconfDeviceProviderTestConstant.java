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
package org.onosproject.provider.netconf.device.impl;

public final class NetconfDeviceProviderTestConstant {

    private NetconfDeviceProviderTestConstant() {
    }

    public static final int ZERO = 0;
    public static final int EVENTINTERVAL = 5;
    public static final String DEV_CONFIG = "devConfigs";
    public static final String CONFIG_WITH_INVALID_ENTRY_NUMBER = "cisco:cisco"
            + "@10.18.11.14:cisco:active";
    public static final String CONFIG_WITH_NULL_ENTRY = "null:null@null:0:active";
    public static final String CONFIG_WITH_DIFFERENT_DEVICE_STATE = "cisco:cisco@10.18.11.14:22:active,"
            + "cisco:cisco@10.18.11.18:22:inactive,cisco:cisco@10.18.11.14:22:invalid,"
            + "cisco:cisco@10.18.11.14:22:null";
    public static final String CONFIG_WITH_ARRAY_OUT_OF_BOUNDEX = "@10.18.11.14:22:active";
    public static final String CONFIG_ENTRY_FOR_DEACTIVATE = "netconf:cisco"
            + "@10.18.11.14:22:active";
    public static final String DEVICE_IP = "10.18.14.19";
    public static final int DEVICE_PORT = 22;
    public static final String DEVICE_USERNAME = "cisco";
    public static final String DEVICE_PASSWORD = "cisco";
    public static final String AT_THE_RATE = "@";
    public static final String COLON = ":";
    public static final String NULL = "";
    public static final String NULL_NULL = "null,null";
    public static final String SCHEME_NETCONF = "netconf";
    public static final String DEVICE_ID = "of:0000000000000001";

}
