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

package org.onosproject.xmpp.pubsub.model;

import org.dom4j.Element;
import org.xmpp.packet.Message;

import static org.onosproject.xmpp.pubsub.XmppPubSubConstants.PUBSUB_EVENT_NS;

/**
 * Abstracts Event Notification message of XMPP PubSub protocol.
 */
public class XmppEventNotification extends Message {

    /**
     * Constructor for XmppEventNotification class. It generates representation
     * of XMPP NOTIFY message.
     *
     * @param node node attribute of PubSub extension
     * @param payload XML payload for XMPP NOTIFY message
     */
    public XmppEventNotification(String node, Element payload) {
        super(docFactory.createDocument().addElement("message"));
        this.addChildElement("event", PUBSUB_EVENT_NS);
        Element items = docFactory.createElement("items");
        items.addAttribute("node", node);
        items.add(payload);
        this.getElement().element("event").add(items);
    }

}
