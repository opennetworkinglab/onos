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

package org.onosproject.netconf.ctl.impl;

import com.google.common.annotations.Beta;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;

import java.util.concurrent.CompletableFuture;

/**
 * Interface to represent an objects that does all the IO on a NETCONF session
 * with a device.
 */
public interface NetconfStreamHandler {
    /**
     * Sends the request on the stream that is used to communicate to and from the device.
     *
     * If this request does not contain a messageId then this will throw a NoSuchElementException
     *
     * @param request request to send to the physical device
     * @return a CompletableFuture of type String that will contain the response for the request.
     * @deprecated - use method with messageId parameter instead
     */
    @Deprecated
    CompletableFuture<String> sendMessage(String request);

    /**
     * Sends the request on the stream that is used to communicate to and from the device.
     *
     * @param request request to send to the physical device
     * @param messageId The identifier of the message - should be unique for the session
     * @return a CompletableFuture of type String that will contain the response for the request.
     */
    CompletableFuture<String> sendMessage(String request, int messageId);

    /**
     * Adds a listener for netconf events on the handled stream.
     *
     * @param listener Netconf device event listener
     */
    void addDeviceEventListener(NetconfDeviceOutputEventListener listener);

    /**
     * Removes a listener for netconf events on the handled stream.
     *
     * @param listener Netconf device event listener
     */
    void removeDeviceEventListener(NetconfDeviceOutputEventListener listener);

    @Beta
    /**
     * Sets instance variable that when true allows receipt of notifications.
     *
     * @param enableNotifications if true, allows action based off notifications
     *                             else, stops action based off notifications
     */
    void setEnableNotifications(boolean enableNotifications);
}
