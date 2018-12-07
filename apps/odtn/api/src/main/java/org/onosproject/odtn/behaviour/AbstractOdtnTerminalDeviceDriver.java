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
package org.onosproject.odtn.behaviour;

import com.google.common.io.CharSource;
import org.onlab.util.XmlString;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.Optional;

import static org.onlab.osgi.DefaultServiceDirectory.getService;
import static org.onosproject.odtn.utils.YangToolUtil.toCharSequence;
import static org.onosproject.odtn.utils.YangToolUtil.toDocument;

/**
 * Utility class for NETCONF driver.
 */
public abstract class AbstractOdtnTerminalDeviceDriver {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ENAME_ENVELOPE_RPC        = "rpc";
    private static final String ENAME_ENVELOPE_EDITCONFIG = "edit-config";
    private static final String ENAME_ENVELOPE_TARGET     = "target";
    private static final String ENAME_ENVELOPE_RUNNING    = "running";
    private static final String ENAME_ENVELOPE_CONFIG     = "config";

    private static final String NAMESPACE                 = "urn:ietf:params:xml:ns:netconf:base:1.0";
    private static final String CONFIG_NAMESPACE_NS       = "http://www.w3.org/2000/xmlns/";
    private static final String CONFIG_NS_PRIFIX          = "xmlns:xc";

    protected Document buildEditConfigBody(List<CharSequence> nodes) {

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            log.error("Unexpected error", e);
            throw new IllegalStateException(e);
        }

        Element appendRoot = addEditConfigEnvelope(doc);

        for (CharSequence node : nodes) {
            Document ldoc = toDocument(CharSource.wrap(node));
            Element cfgRoot = ldoc.getDocumentElement();

            // move (or copy) node to another Document
            appendRoot.appendChild(Optional.ofNullable(doc.adoptNode(cfgRoot))
                                       .orElseGet(() -> doc.importNode(cfgRoot, true)));
        }

        log.info("XML:\n{}", XmlString.prettifyXml(toCharSequence(doc)));
        return doc;
    }

    protected void configureDevice(DeviceId did, Document doc) {

        NetconfController ctr = getService(NetconfController.class);
        Optional.ofNullable(ctr.getNetconfDevice(did))
                .map(NetconfDevice::getSession)
                .ifPresent(session -> {
                    try {
                        session.rpc(toCharSequence(doc).toString()).join();
                    } catch (NetconfException e) {
                        log.error("Exception thrown", e);
                    }
                });
    }

    public Element addEditConfigEnvelope(Document doc) {

        // netconf rpc boilerplate part without message-id
        // rpc
        //  +- edit-config
        //      +- target
        //      |   +- running
        //      +- config
        Element rpc = doc.createElementNS(NAMESPACE, ENAME_ENVELOPE_RPC);
        doc.appendChild(rpc);
        Element editConfig = doc.createElement(ENAME_ENVELOPE_EDITCONFIG);
        rpc.appendChild(editConfig);
        Element target = doc.createElement(ENAME_ENVELOPE_TARGET);
        editConfig.appendChild(target);
        target.appendChild(doc.createElement(ENAME_ENVELOPE_RUNNING));

        Element config = doc.createElement(ENAME_ENVELOPE_CONFIG);
        config.setAttributeNS(CONFIG_NAMESPACE_NS, CONFIG_NS_PRIFIX, NAMESPACE);
        editConfig.appendChild(config);

        return config;
    }
}
