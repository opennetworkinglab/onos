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

import org.xmpp.packet.Packet;

/**
 * Responsible for keeping track of the current set of devices
 * connected to the system. As well as notifying Xmpp Stanza listeners.
 *
 */
public interface XmppDeviceAgent {

    /**
     * Add a device that has just connected to the system.
     * @param deviceId the identifier of device to add.
     * @param device the actual device object.
     * @return true if added, false otherwise.
     */
    boolean addConnectedDevice(XmppDeviceId deviceId, XmppDevice device);

    /**
     * Remove a device from a local repository that has been disconnected
     * from the local controller. Notify device listeners.
     * @param deviceId the identifier of device to remove.
     */
    void removeConnectedDevice(XmppDeviceId deviceId);

    /**
     * Returns XMPP device object that is stored in the local repository
     * containing already connected devices.
     * @param deviceId the identifier of device
     * @return the XMPP device object
     */
    XmppDevice getDevice(XmppDeviceId deviceId);

    /**
     * Process an event (incoming Packet) coming from a device.
     * @param deviceId the identifier of device the packet came on.
     * @param packet the packet to process
     */
    void processUpstreamEvent(XmppDeviceId deviceId, Packet packet);

}

