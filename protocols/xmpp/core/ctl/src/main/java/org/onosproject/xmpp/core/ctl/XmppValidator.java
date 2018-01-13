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

package org.onosproject.xmpp.core.ctl;

import org.dom4j.Element;
import org.onosproject.xmpp.core.XmppConstants;
import org.onosproject.xmpp.core.ctl.exception.XmppValidationException;
import org.onosproject.xmpp.core.stream.XmppStreamOpen;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Validates incoming XMPP packets.
 */
public class XmppValidator {

    public void validateStream(XmppStreamOpen stream) throws XmppValidationException {
        try {
            String jid = stream.getElement().attribute("from").getValue();
            validateJid(jid);
        } catch (Exception e) {
            throw new XmppValidationException(true);
        }
    }

    public void validate(Packet packet) throws XmppValidationException {
        validateBasicXmpp(packet);
        Element root = packet.getElement();
        if (root.getName().equals(XmppConstants.IQ_QNAME)) {
            validateIQ((IQ) packet);
        } else if (root.getName().equals(XmppConstants.MESSAGE_QNAME)) {
            validateMessage((Message) packet);
        } else if (root.getName().equals(XmppConstants.PRESENCE_QNAME)) {
            validatePresence((Presence) packet);
        }
    }

    public void validateIQ(IQ iq) throws XmppValidationException{
        // TODO: implement IQ validation
    }

    public void validateMessage(Message message) throws XmppValidationException {
        // TODO: implement Message validation
    }

    public void validatePresence(Presence presence) throws XmppValidationException {
        // TODO: implement Presence validation
    }

    private void validateBasicXmpp(Packet packet) throws XmppValidationException {
        try {
            validateJid(packet.getFrom());
            validateJid(packet.getTo());
        } catch (Exception e) {
            throw new XmppValidationException(false);
        }
    }

    public void validateJid(String jid) throws XmppValidationException {
        try {
            checkNotNull(jid);
            JID testJid = new JID(jid);
        } catch (Exception e) {
            throw new XmppValidationException(false);
        }
    }

    public void validateJid(JID jid) {
        checkNotNull(jid);
    }
}
