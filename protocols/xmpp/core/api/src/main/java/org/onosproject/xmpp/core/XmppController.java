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
 * Abstraction of a XMPP controller. Serves as a one stop
 * shop for obtaining XMPP devices and (un)register listeners
 * on XMPP events
 */
public interface XmppController {

    /**
     * Method allows to retrieve XMPP device for given XmppDeviceId, if one exists.
     *
     * @param xmppDeviceId the device to retrieve
     * @return the interface to XMPP Device
     */
    XmppDevice getDevice(XmppDeviceId xmppDeviceId);

    /**
     * Register a listener for device events.
     *
     * @param deviceListener the listener to notify
     */
    void addXmppDeviceListener(XmppDeviceListener deviceListener);

    /**
     * Unregister a listener for device events.
     *
     * @param deviceListener the listener to unregister
     */
    void removeXmppDeviceListener(XmppDeviceListener deviceListener);

    /**
     * Register a listener for IQ stanzas containing specific XML namespace.
     *
     * @param iqListener the listener to notify
     * @param namespace the XML namespace to observe
     */
    void addXmppIqListener(XmppIqListener iqListener, String namespace);

    /**
     * Unregister a listener for IQ stanzas containing specific XML namespace.
     *
     * @param iqListener the listener to unregister
     * @param namespace the XML namespace to observe
     */
    void removeXmppIqListener(XmppIqListener iqListener, String namespace);

    /**
     * Register a listener for Message stanza of XMPP protocol.
     *
     * @param messageListener the listener to notify
     */
    void addXmppMessageListener(XmppMessageListener messageListener);

    /**
     * Unregister a listener for Message stanza of XMPP protocol.
     *
     * @param messageListener the listener to unregister
     */
    void removeXmppMessageListener(XmppMessageListener messageListener);

    /**
     * Register a listener for Presence stanza of XMPP protocol.
     *
     * @param presenceListener the listener to notify
     */
    void addXmppPresenceListener(XmppPresenceListener presenceListener);

    /**
     * Unregister a listener for Presence stanza of XMPP protocol.
     *
     * @param presenceListener the listener to unregister
     */
    void removeXmppPresenceListener(XmppPresenceListener presenceListener);

}
