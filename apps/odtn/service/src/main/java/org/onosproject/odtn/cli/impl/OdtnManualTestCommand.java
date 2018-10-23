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
package org.onosproject.odtn.cli.impl;

import static org.onosproject.odtn.utils.YangToolUtil.toCharSequence;
import static org.onosproject.odtn.utils.YangToolUtil.toCompositeData;
import static org.onosproject.odtn.utils.YangToolUtil.toDocument;
import static org.onosproject.odtn.utils.YangToolUtil.toResourceData;
import static org.onosproject.odtn.utils.YangToolUtil.toXmlCompositeStream;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.XmlString;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.cli.net.PortNumberCompleter;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.odtn.behaviour.ConfigurableTransceiver;
import org.onosproject.odtn.behaviour.PlainTransceiver;
import org.onosproject.odtn.utils.openconfig.OpticalChannel;
import org.onosproject.odtn.utils.openconfig.Transceiver;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Lists;
import com.google.common.io.CharSource;

@Service
@Command(scope = "onos", name = "odtn-manual-test",
         description = "ODTN manual test command")
public class OdtnManualTestCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(OdtnManualTestCommand.class);

    public static enum Mode {
        ENABLE_TRANSCEIVER,
        DISABLE_TRANSCEIVER,
        PRECONF_TRANSCEIVER,
        PRECONF_OPTICAL_CHANNEL,
    }

    ModeCompleter modeCompleter;
    @Argument(index = 0, name = "mode", description = "one of Mode see source",
              required = true)
    @Completion(ModeCompleter.class)
    String modeStr = Mode.ENABLE_TRANSCEIVER.name();
    Mode mode;

    @Option(name = "--deviceId", description = "Device ID URI to send configuration to",
              required = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    // Note: this will required Port information in device subystem
    @Option(name = "--cltPortNo", description = "Client-side PortNumber to send configuration to",
            required = false)
    @Completion(PortNumberCompleter.class)
    String cltPortNo = null;

    @Option(name = "--linePortNo", description = "Line-side PortNumber to send configuration to",
            required = false)
    @Completion(PortNumberCompleter.class)
    String linePortNo = null;


    // TODO add completer for this?
    @Option(name = "--component",
            description = "Component name",
            required = false, multiValued = false)
    private String componentName = "TRANSCEIVER_1_1_4_1";


    // OSGi Services to be filled in at the beginning.
    private DynamicConfigService dcs;
    private DeviceService deviceService;


    void printlog(String format, Object... objs) {
        print(format.replaceAll(Pattern.quote("{}"), "%s"), objs);
        log.info(format, objs);
    }

    List<CharSequence> transform(List<DataNode> input) {
        ResourceId empty = ResourceId.builder().build();
        return Lists.transform(input,
                   node -> toCharSequence(toXmlCompositeStream(toCompositeData(toResourceData(empty, node)))));
    }


    @Override
    protected void doExecute() {
        dcs = get(DynamicConfigService.class);
        deviceService = get(DeviceService.class);

        try {
            mode = Mode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            printlog("{} is not a valid Mode, pick one of {}",
                     modeStr,
                     Arrays.asList(Mode.values()),
                     e);
            return;
        }

        // effectively configuration context
        List<CharSequence> nodes = new ArrayList<>();

        // driver selection with fallback to plain OpenConfig
        DeviceId did = Optional.ofNullable(uri)
                        .map(DeviceId::deviceId)
                        .orElse(null);
        ConfigurableTransceiver transceiver =
            Optional.ofNullable(did)
            .map(deviceService::getDevice)
            .filter(device -> device.is(ConfigurableTransceiver.class))
            .map(device -> device.as(ConfigurableTransceiver.class))
            .orElseGet(() -> new PlainTransceiver());

        switch (mode) {
        case PRECONF_TRANSCEIVER:
            // note: these doesn't support driver
            nodes.addAll(transform(Transceiver.preconf(componentName)));
            break;

        case ENABLE_TRANSCEIVER:
            if (cltPortNo != null && linePortNo != null) {
                nodes.addAll(transceiver.enable(PortNumber.portNumber(cltPortNo),
                                                PortNumber.portNumber(linePortNo),
                                                true));
            } else {
                nodes.addAll(transceiver.enable(componentName, true));
            }
            break;

        case DISABLE_TRANSCEIVER:
            if (cltPortNo != null && linePortNo != null) {
                nodes.addAll(transceiver.enable(PortNumber.portNumber(cltPortNo),
                                                PortNumber.portNumber(linePortNo),
                                                false));
            } else {
                nodes.addAll(transceiver.enable(componentName, false));
            }
            break;

        case PRECONF_OPTICAL_CHANNEL:
            // note: these doesn't support driver
            nodes.addAll(transform(OpticalChannel.preconf(componentName)));
            break;

        default:
            printlog("Mode {} not supported yet", mode);
            break;
        }

        // Do something about it.
        doTheMagic(nodes);
    }

    void doTheMagic(List<CharSequence> nodes) {

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            printlog("Unexpected error", e);
            throw new IllegalStateException(e);
        }

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


        for (CharSequence node : nodes) {
            Document ldoc = toDocument(CharSource.wrap(node));
            if (ldoc != null) {
                Element cfgRoot = ldoc.getDocumentElement();

                // is everything as merge, ok?
                cfgRoot.setAttribute("xc:operation", "merge");

                // move (or copy) node to another Document
                config.appendChild(Optional.ofNullable(doc.adoptNode(cfgRoot))
                                           .orElseGet(() -> doc.importNode(cfgRoot, true)));

                // don't have good use for JSON for now
                //JsonNode json = toJsonNode(toJsonCompositeStream(toCompositeData(toResourceData(resourceId, node))));
                //printlog("JSON:\n{}", toCharSequence(json));
            }
        }

        printlog("XML:\n{}", XmlString.prettifyXml(toCharSequence(doc)));

        // TODO if deviceId is given send it out to the device
        if (uri != null) {
            DeviceId deviceId = DeviceId.deviceId(uri);
            NetconfController ctr = get(NetconfController.class);
            Optional.ofNullable(ctr.getNetconfDevice(deviceId))
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
}
