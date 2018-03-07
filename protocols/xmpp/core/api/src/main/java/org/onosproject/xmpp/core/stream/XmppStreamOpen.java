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

package org.onosproject.xmpp.core.stream;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.xmpp.packet.JID;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstracts XMPP stream open event.
 */
public class XmppStreamOpen implements XmppStreamEvent {

    private final Logger log = LoggerFactory.getLogger(XmppStreamOpen.class);

    public static final String QNAME = "stream";

    private Element element;

    public XmppStreamOpen(Element element) {
        this.element = element;
    }

    @Override
    public String toXml() {
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter(out, OutputFormat.createCompactFormat());
        try {
            out.write("<");
            writer.write(element.getQualifiedName());
            for (Attribute attr : (List<Attribute>) element.attributes()) {
                writer.write(attr);
            }
            writer.write(Namespace.get(this.element.getNamespacePrefix(), this.element.getNamespaceURI()));
            writer.write(Namespace.get("jabber:client"));
            out.write(">");
        } catch (IOException ex) {
            log.info("Error writing XML", ex);
        }
        return out.toString();
    }

    public JID getFromJid() {
        String jid = this.element.attribute("from").getValue();
        return new JID(jid);
    }

    public Element getElement() {
        return this.element;
    }

    public JID getToJid() {
        String jid = this.element.attribute("to").getValue();
        return new JID(jid);
    }
}
