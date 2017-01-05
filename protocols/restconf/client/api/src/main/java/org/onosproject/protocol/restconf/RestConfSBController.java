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
package org.onosproject.protocol.restconf;

import org.onosproject.net.DeviceId;
import org.onosproject.protocol.http.HttpSBController;

/**
 * Abstraction of a RESTCONF controller. Serves as a one stop shop for obtaining
 * RESTCONF southbound devices and (un)register listeners.
 */
public interface RestConfSBController extends HttpSBController {

    /**
     * This method is to be called by whoever is interested to receive
     * Notifications from a specific device. It does a REST GET request
     * with specified parameters to the device, and calls the provided
     * callBackListener upon receiving notifications to notify the requester
     * about notifications.
     *
     * @param device           device to make the request to
     * @param request          url of the request
     * @param mediaType        format to retrieve the content in
     * @param callBackListener method to call when notifications arrives
     */
    void enableNotifications(DeviceId device, String request, String mediaType,
                             RestconfNotificationEventListener callBackListener);

    /**
     * Registers a listener for notification events that occur to restconf
     * devices.
     *
     * @param deviceId identifier of the device to which the listener is attached
     * @param listener the listener to notify
     */
    void addNotificationListener(DeviceId deviceId,
                                 RestconfNotificationEventListener listener);

    /**
     * Unregisters the listener for the device.
     *
     * @param deviceId identifier of the device for which the listener
     *                 is to be removed
     * @param listener listener to be removed
     */
    void removeNotificationListener(DeviceId deviceId,
                                    RestconfNotificationEventListener listener);

    /**
     * Returns true if a listener has been installed to listen to RESTCONF
     * notifications sent from a particular device.
     *
     * @param deviceId identifier of the device from which the notifications
     *                 are generated
     * @return true if listener is installed; false otherwise
     */
    boolean isNotificationEnabled(DeviceId deviceId);
}
