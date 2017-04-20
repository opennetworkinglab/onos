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
package org.onosproject.netconf;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener to listen for event about specific Device.
 */
public class FilteringNetconfDeviceOutputEventListener
        implements NetconfDeviceOutputEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(FilteringNetconfDeviceOutputEventListener.class);

    private final NetconfDeviceInfo deviceInfo;

    public FilteringNetconfDeviceOutputEventListener(NetconfDeviceInfo deviceInfo) {
        this.deviceInfo = checkNotNull(deviceInfo);
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
