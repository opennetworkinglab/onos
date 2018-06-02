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

package org.onosproject.odtn.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.odtn.behaviour.ConfigurableTransceiver;
import org.onosproject.odtn.behaviour.OdtnTerminalDeviceDriver;
import org.onosproject.odtn.behaviour.PlainTransceiver;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.osgi.DefaultServiceDirectory.getService;

import static org.onosproject.odtn.utils.YangToolUtil.toCharSequence;
import static org.onosproject.odtn.utils.YangToolUtil.toDocument;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.onlab.util.XmlString;
import org.onosproject.netconf.NetconfController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.io.CharSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Device driver implementation for ODTN Phase1.0.
 * <p>
 * NETCONF SB should be provided by DCS, but currently DCS SB driver have
 * some critical problem to configure actual devices and netconf servers,
 * as a workaround this posts netconf edit-config directly.
 */
public final class DefaultOdtnTerminalDeviceDriver implements OdtnTerminalDeviceDriver {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private DeviceService deviceService;

    private DefaultOdtnTerminalDeviceDriver() {
    }

    public static DefaultOdtnTerminalDeviceDriver create() {
        DefaultOdtnTerminalDeviceDriver self = new DefaultOdtnTerminalDeviceDriver();
        self.deviceService = getService(DeviceService.class);
        return self;
    }

    @Override
    public void apply(DeviceId did, PortNumber client, PortNumber line, boolean enable) {

        checkNotNull(did);
        checkNotNull(client);
        checkNotNull(line);

        List<CharSequence> nodes = new ArrayList<>();

        ConfigurableTransceiver transceiver =
                Optional.ofNullable(did)
                        .map(deviceService::getDevice)
                        .filter(device -> device.is(ConfigurableTransceiver.class))
                        .map(device -> device.as(ConfigurableTransceiver.class))
                        .orElseGet(() -> new PlainTransceiver());

        nodes.addAll(transceiver.enable(client, line, enable));
        if (nodes.size() == 0) {
            log.warn("Nothing to be configured.");
            return;
        }

        Document doc = buildEditConfigBody(nodes);
        configureDevice(did, doc);
    }

    private Document buildEditConfigBody(List<CharSequence> nodes) {

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            log.error("Unexpected error", e);
            throw new IllegalStateException(e);
        }

        Element config = addEditConfigEnvelope(doc);

        for (CharSequence node : nodes) {
            Document ldoc = toDocument(CharSource.wrap(node));
            Element cfgRoot = ldoc.getDocumentElement();

            cfgRoot.setAttribute("xc:operation", Operation.MERGE.value());

            // move (or copy) node to another Document
            config.appendChild(Optional.ofNullable(doc.adoptNode(cfgRoot))
                    .orElseGet(() -> doc.importNode(cfgRoot, true)));

        }

        log.info("XML:\n{}", XmlString.prettifyXml(toCharSequence(doc)));
        return doc;
    }

    private Element addEditConfigEnvelope(Document doc) {

        // netconf rpc boilerplate part without message-id
        Element rpc = doc.createElementNS("urn:ietf:params:xml:ns:netconf:base:1.0", "rpc");
        doc.appendChild(rpc);
        Element editConfig = doc.createElement("edit-config");
        rpc.appendChild(editConfig);
        Element target = doc.createElement("target");
        editConfig.appendChild(target);
        target.appendChild(doc.createElement("running"));

        Element config = doc.createElement("config");
        config.setAttributeNS("http://www.w3.org/2000/xmlns/",
                "xmlns:xc",
                "urn:ietf:params:xml:ns:netconf:base:1.0");
        editConfig.appendChild(config);

        return config;
    }

    private void configureDevice(DeviceId did, Document doc) {

        NetconfController ctr = getService(NetconfController.class);
        Optional.ofNullable(ctr.getNetconfDevice(did))
                .map(NetconfDevice::getSession)
                .ifPresent(session -> {
                    try {
                        session.rpc(toCharSequence(doc, false).toString()).join();
                    } catch (NetconfException e) {
                        log.error("Exception thrown", e);
                    }
                });
    }

}
