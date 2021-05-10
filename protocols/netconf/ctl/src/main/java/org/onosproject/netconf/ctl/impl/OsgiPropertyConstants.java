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

package org.onosproject.netconf.ctl.impl;

/**
 * Constants for default values of configurable properties.
 */
public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {}

    public static final String NETCONF_CONNECT_TIMEOUT = "netconfConnectTimeout";
    public static final int NETCONF_CONNECT_TIMEOUT_DEFAULT = 5;

    public static final String NETCONF_REPLY_TIMEOUT = "netconfReplyTimeout";
    public static final int NETCONF_REPLY_TIMEOUT_DEFAULT = 5;

    public static final String NETCONF_IDLE_TIMEOUT = "netconfIdleTimeout";
    public static final int NETCONF_IDLE_TIMEOUT_DEFAULT = 300;

    public static final String SSH_LIBRARY = "sshLibrary";
    public static final String SSH_LIBRARY_DEFAULT = "apache-mina";

    public static final String SSH_KEY_PATH = "sshKeyPath";
    public static final String SSH_KEY_PATH_DEFAULT = "/root/.ssh/id_rsa";
}
