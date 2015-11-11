/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.openstackswitching;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.basics.BasicElementConfig;

/**
 * Handles configuration for OpenstackSwitching app.
 */
public class OpenstackSwitchingConfig extends Config<ApplicationId> {
    public static final String DONOTPUSH = "do_not_push_flows";
    public static final String NEUTRON_SERVER = "neutron_server";
    public static final String KEYSTONE_SERVER = "keystone_server";
    public static final String USER_NAME = "user_name";
    public static final String PASSWORD = "password";

    /**
     * Returns the flag whether the app pushes flows or not.
     *
     * @return the flag or false if not set
     */
    public boolean doNotPushFlows() {
        String flag = get(DONOTPUSH, "false");
        return Boolean.valueOf(flag);
    }

    /**
     * Returns the Neutron server IP address.
     *
     * @return Neutron server IP
     */
    public String neutronServer() {
        return get(NEUTRON_SERVER, "");
    }

    /**
     * Returns the Keystone server IP address.
     *
     * @return Keystone server IP
     */
    public String keystoneServer() {
        return get(KEYSTONE_SERVER, "");
    }

    /**
     * Returns the username for openstack.
     *
     * @return username for openstack
     */
    public String userName() {
        return get(USER_NAME, "");
    }

    /**
     * Returns the password for openstack.
     *
     * @return password for openstack
     */
    public String password() {
        return get(PASSWORD, "");
    }

    /**
     * Sets the flag whether the app pushes flows or not.
     *
     * @param flag the flag whether the app pushes flows or not
     * @return self
     */
    public BasicElementConfig doNotPushFlows(boolean flag) {
        return (BasicElementConfig) setOrClear(DONOTPUSH, flag);
    }

    /**
     * Sets the neutron server IP address.
     *
     * @param url neutron server IP address
     * @return itself
     */
    public BasicElementConfig neutronServer(String url) {
        return (BasicElementConfig) setOrClear(NEUTRON_SERVER, url);
    }

    /**
     * Sets the keystone server IP address.
     *
     * @param url keystone server IP address
     * @return itself
     */
    public BasicElementConfig keystoneServer(String url) {
        return (BasicElementConfig) setOrClear(KEYSTONE_SERVER, url);
    }

    /**
     * Sets the username for openstack.
     *
     * @param username user name for openstack
     * @return itself
     */
    public BasicElementConfig userName(String username) {
        return (BasicElementConfig) setOrClear(USER_NAME, username);
    }

    /**
     * Sets the password for openstack.
     *
     * @param password password for openstack
     * @return itself
     */
    public BasicElementConfig password(String password) {
        return (BasicElementConfig) setOrClear(PASSWORD, password);
    }
}
