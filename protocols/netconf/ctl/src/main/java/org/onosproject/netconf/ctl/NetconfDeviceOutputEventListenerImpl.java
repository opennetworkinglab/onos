/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.netconf.ctl;

import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example of a listener for events that happen a Netconf session established
 * for a particular NETCONF device.
 *
 * @deprecated in 1.10.0 use FilteringNetconfDeviceOutputEventListener
 */
@Deprecated
public class NetconfDeviceOutputEventListenerImpl implements NetconfDeviceOutputEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(NetconfDeviceOutputEventListenerImpl.class);

    private NetconfDeviceInfo deviceInfo;

    public NetconfDeviceOutputEventListenerImpl(NetconfDeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    @Override
    public void event(NetconfDeviceOutputEvent event) {
        switch (event.type()) {
            case DEVICE_REPLY:
                log.debug("Device {} has reply: {}", deviceInfo, event.getMessagePayload());
                break;
            case DEVICE_NOTIFICATION:
                log.info("Device {} has notification: {}", deviceInfo, event.getMessagePayload());
                break;
            case DEVICE_UNREGISTERED:
                log.warn("Device {} has closed session", deviceInfo);
                break;
            case DEVICE_ERROR:
                log.warn("Device {} has error: {}", deviceInfo, event.getMessagePayload());
                break;
            case SESSION_CLOSED:
                log.warn("Device {} has closed Session: {}", deviceInfo, event.getMessagePayload());
                break;
            default:
                log.warn("Wrong event type {} ", event.type());
        }

    }

    @Override
    public boolean isRelevant(NetconfDeviceOutputEvent event) {
        return deviceInfo.equals(event.getDeviceInfo());
    }
}
