/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.drivers.juniper;

import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class JuniperAbstractHandlerBehaviour extends AbstractHandlerBehaviour {

    private static final Logger log = getLogger(JuniperAbstractHandlerBehaviour.class);

    /**
     * Lookup the current NETCONF session for specified device.
     * @param deviceId id of device
     * @return the current session (which may be null)
     */
    NetconfSession lookupNetconfSession(final DeviceId deviceId) {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        final NetconfDevice netconfDevice = controller.getDevicesMap().get(deviceId);

        if (netconfDevice == null) {
            log.warn("NETCONF session to device {} not yet established, can be retried", deviceId);
            return null;
        }

        return netconfDevice.getSession();
    }

}
