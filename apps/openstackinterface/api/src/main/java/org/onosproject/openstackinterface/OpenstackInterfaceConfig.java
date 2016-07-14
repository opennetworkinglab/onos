/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstackinterface;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.basics.BasicElementConfig;

/**
 * Handles configuration for OpenstackInterface app.
 */
public class OpenstackInterfaceConfig extends Config<ApplicationId> {
    public static final String NEUTRON_SERVER = "neutronServer";
    public static final String KEYSTONE_SERVER = "keystoneServer";
    public static final String USER_NAME = "userName";
    public static final String PASSWORD = "password";
    public static final String PHYSICAL_ROUTER_MAC = "physicalRouterMac";

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
     * Returns the MacAddress for physical router.
     *
     * @return physical router mac
     */
    public String physicalRouterMac() {
        return get(PHYSICAL_ROUTER_MAC, "");
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
