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

package org.onosproject.net.behaviour;

import org.onosproject.net.driver.HandlerBehaviour;

/**
 * Behaviour for handling various drivers for l3vpn configurations.
 */
public interface L3VpnConfig extends HandlerBehaviour {

    /**
     * Create virtual routing forwarding instance on requested device with
     * given standard device model object data.
     *
     * @param objectData standard device model object data
     * @return device model object data
     */
    Object createInstance(Object objectData);

    /**
     * Binds requested virtual routing forwarding instance to interface on the
     * requested device with given standard device model object data.
     *
     * @param objectData standard device model object data
     * @return device model object data
     */
    Object bindInterface(Object objectData);

    /**
     * Deletes virtual routing forwarding instance on requested device with
     * given standard device model object data.
     *
     * @param objectData standard device model object data
     * @return device model object data
     */
    Object deleteInstance(Object objectData);

    /**
     * Unbinds requested virtual routing forwarding instance to interface on the
     * requested device with given standard device model object data.
     *
     * @param objectData standard device model object data
     * @return device model object data
     */
    Object unbindInterface(Object objectData);

    /**
     * Creates BGP routing protocol info on requested device with given
     * BGP info object.
     *
     * @param bgpInfo   BGP info object
     * @param bgpConfig BGP driver config
     * @return device model object data
     */
    Object createBgpInfo(Object bgpInfo, Object bgpConfig);

    /**
     * Deletes BGP routing protocol info on requested device with given
     * BGP info object.
     *
     * @param bgpInfo   BGP info object
     * @param bgpConfig BGP driver config
     * @return device model object data
     */
    Object deleteBgpInfo(Object bgpInfo, Object bgpConfig);
}
