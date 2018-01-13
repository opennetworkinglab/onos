/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.xmpp.core;

/**
 * Allows for providers interested in XMPP device events to be notified.
 */
public interface XmppDeviceListener {

    /**
     * Notify that the device was added.
     *
     * @param deviceId identifier of the device where the event occurred
     */
    void deviceConnected(XmppDeviceId deviceId);

    /**
     * Notify that the device was removed.
     *
     * @param deviceId identifier of the device where the event occurred
     */
    void deviceDisconnected(XmppDeviceId deviceId);

}
