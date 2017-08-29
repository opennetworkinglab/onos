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

import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.yang.model.ModelObjectData;

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
    ModelObjectData createInstance(ModelObjectData objectData);

    /**
     * Binds requested virtual routing forwarding instance to interface on the
     * requested device with given standard device model object data.
     *
     * @param objectData standard device model object data
     * @return device model object data
     */
    ModelObjectData bindInterface(ModelObjectData objectData);

    /**
     * Deletes virtual routing forwarding instance on requested device with
     * given standard device model object data.
     *
     * @param objectData standard device model object data
     * @return device model object data
     */
    ModelObjectData deleteInstance(ModelObjectData objectData);

    /**
     * Unbinds requested virtual routing forwarding instance to interface on the
     * requested device with given standard device model object data.
     *
     * @param objectData standard device model object data
     * @return device model object data
     */
    ModelObjectData unbindInterface(ModelObjectData objectData);

    /**
     * Deletes tunnel on requested device with the given tunnel info.
     *
     * @param tnlInfo tunnel info
     * @return device model object data
     */
    ModelObjectData deleteTnl(TunnelInfo tnlInfo);

    /**
     * Creates BGP routing protocol info on requested device with given
     * BGP info object.
     *
     * @param bgpInfo   BGP info object
     * @param bgpConfig BGP driver config
     * @return device model object data
     */
    ModelObjectData createBgpInfo(BgpInfo bgpInfo, BgpDriverInfo bgpConfig);

    /**
     * Deletes BGP routing protocol info on requested device with given
     * BGP info object.
     *
     * @param bgpInfo   BGP info object
     * @param bgpConfig BGP driver config
     * @return device model object data
     */
    ModelObjectData deleteBgpInfo(BgpInfo bgpInfo, BgpDriverInfo bgpConfig);

    /**
     * Creates device and devices level on requested device for tunnel creation.
     *
     * @param tnlInfo tunnel info
     * @return device model object data
     */
    ModelObjectData createTnlDev(TunnelInfo tnlInfo);

    /**
     * Creates tunnel policy on requested device with given tunnel info.
     *
     * @param tnlInfo tunnel info
     * @return device model object data
     */
    ModelObjectData createTnlPol(TunnelInfo tnlInfo);

    /**
     * Creates tunnel on requested device with given tunnel info.
     *
     * @param tnlInfo tunnel info
     * @return device model object data
     */
    ModelObjectData createTnl(TunnelInfo tnlInfo);

    /**
     * Binds requested tunnel policy name to the VPN to the requested device
     * with given tunnel info.
     *
     * @param tnlInfo tunnel info
     * @return device model object data
     */
    ModelObjectData bindTnl(TunnelInfo tnlInfo);
}
