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

import org.onlab.util.Identifier;
import org.onosproject.net.DeviceId;
import org.xmpp.packet.JID;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * The class representing a network device identifier.
 * This class is immutable.
 */
public final class XmppDeviceId extends Identifier<String> {

    private static final String SCHEME = "xmpp";

    private JID jid;

    public XmppDeviceId(JID jid) {
        super(uri(jid.toString()).toString());
        this.jid = jid;
    }

    @Override
    public String toString() {
        return identifier.toString();
    }

    public JID getJid() {
        return jid;
    }

    public static URI uri(String string) {
        try {
            return new URI(SCHEME, string, null);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static DeviceId asDeviceId(JID jid) {
        return DeviceId.deviceId(XmppDeviceId.uri(jid));
    }

    public static URI uri(JID jid) {
        try {
            return new URI(SCHEME, jid.toString(), null);
        } catch (URISyntaxException e) {
            return null;
        }
    }


}
