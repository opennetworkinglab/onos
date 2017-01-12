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

package org.onosproject.protocol.rest;

import org.onosproject.net.DeviceId;
import org.onosproject.protocol.http.HttpSBController;

import java.util.Set;

/**
 * Abstraction of an REST controller. Serves as a one stop shop for obtaining
 * Rest southbound devices.
 */
public interface RestSBController extends HttpSBController {

    /**
     * Add a new association between a proxied device exposed to ONOS and
     * a REST proxy server.
     * @param deviceId REST device identifier
     * @param proxy REST proxy device
     */
    void addProxiedDevice(DeviceId deviceId, RestSBDevice proxy);

    /**
     * Remove the association between a proxied device exposed to ONOS
     * and a REST proxy server.
     * @param deviceId REST device identifier
     */
    void removeProxiedDevice(DeviceId deviceId);

    /**
     * Get all the proxied device exposed to ONOS ids under the same
     * REST proxy server.
     * @param proxyId REST proxy device identifier
     * @return set of device ids under same proxy
     */
    Set<DeviceId> getProxiedDevices(DeviceId proxyId);

    /**
     * Get a REST proxied server given a device id.
     * @param deviceId the id of proxied device exposed to ONOS
     * @return the corresponding REST proxied device
     */
    RestSBDevice getProxySBDevice(DeviceId deviceId);
}
