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
import org.xmpp.packet.IQ;

/**
 * Abstracts Publish message of XMPP PubSub protocol.
 */
public class XmppPublish extends IQ {

    private String jabberId;
    private String nodeID;
    private Element item;
    private String itemID;
    private Element itemEntry;
    private String itemEntryNamespace;

    /**
     * Constructor for XmppPublish class.
     * @param iq XMPP IQ stanza, which XmppPublish is based on.
     */
    public XmppPublish(IQ iq) {
        super(iq.getElement());
        this.jabberId = this.fromJID.toString();
        this.nodeID = this.getChildElement().element("publish").attribute("node").getValue();
        this.item = this.getChildElement().element("publish").element("item");
        this.itemID = this.item.attribute("id").getValue();
        this.itemEntry = this.item.element("entry");
        this.itemEntryNamespace = this.itemEntry.getNamespaceURI();
    }

    public String getJabberId() {
        return this.jabberId;
    }

    public String getNodeID() {
        return this.nodeID;
    }

    public Element getItem() {
        return this.item;
    }

    public String getItemID() {
        return this.itemID;
    }

    public Element getItemEntry() {
        return this.itemEntry;
    }

    public String getItemEntryNamespace() {
        return this.itemEntryNamespace;
    }

    @Override
    public String toString() {
        return "Publish{" +
                "JID=" + fromJID +
                "NodeID=" + this.getNodeID() +
                "Item=\n" + this.getItem().asXML() +
                '}';
    }

}
