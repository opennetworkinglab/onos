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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.utils.YangToolUtil.toCharSequence;
import static org.onosproject.odtn.utils.YangToolUtil.toDocument;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
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
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.behaviour.ConfigurableTransceiver;
import org.onosproject.odtn.behaviour.FujitsuTransceiver;
import org.onosproject.odtn.cli.impl.OdtnManualTestCommand.Mode;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.io.CharSource;


@Command(scope = "onos", name = "odtn-fujitsu-manual-test",
        description = "ODTN Fujtisu manual test command")
public class OdtnFujitsuManualTestCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(OdtnFujitsuManualTestCommand.class);
    private final String rpcElement = "rpc";
    private final String netconfNamespaceUri = "urn:ietf:params:xml:ns:netconf:base:1.0";
    private final String xmlNamespaceUri = "http://www.w3.org/2000/xmlns/";
    private final String xmlNamespaceQualifiedName = "xmlns:xc";
    private final String xmlNamespaceValue = "urn:ietf:params:xml:ns:netconf:base:1.0";
    private final String editConfigTag = "edit-config";
    private final String targetTag = "target";
    private final String candidateTag = "candidate";
    private final String configTag = "config";
    private final String attributeName = "xc:operation";
    private final String attributeValue = "merge";
    private final String netconfOkTag = "<ok/>";
    private final String commitTag = "<commit/>";


    ModeCompleter fujitsuModeCompleter;
    @Argument(index = 0, name = "mode", description = "one of Mode see source",
            required = true)
    String modeStr = Mode.ENABLE_TRANSCEIVER.name();
    Mode mode;

    // injecting dependency for OSGi package import generation purpose
    DeviceIdCompleter uriCompleter;
    @Option(name = "--deviceId", description = "Device ID URI to send configuration to",
            required = false)
    String uri = null;

    // injecting dependency for OSGi package import generation purpose
    PortNumberCompleter portNoCompleter;
    // Note: this will required Port information in device subystem
    @Option(name = "--cltPortNo", description = "Client-side PortNumber to send configuration to",
            required = false)
    String cltPortNo = null;

    @Option(name = "--linePortNo", description = "Line-side PortNumber to send configuration to",
            required = false)
    String linePortNo = null;

    @Option(name = "--component",
            description = "Component name",
            required = false, multiValued = false)
    private String componentName = "";

    private DeviceService deviceService;

    /**
     * Print to console.
     * @param format format of the string
     * @param objs object
     */
    void printlog(String format, Object... objs) {
        print(format.replaceAll(Pattern.quote("{}"), "%s"), objs);
        log.info(format, objs);
    }

    @Override
    protected void execute() {
        // OSGi Services to be filled in at the beginning.
        DynamicConfigService dcs = get(DynamicConfigService.class);
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
                        .orElseGet(() -> new FujitsuTransceiver());

        switch (mode) {

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

            default:
                printlog("Mode {} not supported yet", mode);
                break;
        }

        // Do something about it.
        createReqRpc(nodes);
    }

    void createReqRpc(List<CharSequence> nodes) {

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            printlog("Unexpected error", e);
            throw new IllegalStateException(e);
        }

        // netconf rpc boilerplate part without message-id
        Element rpc = doc.createElementNS(netconfNamespaceUri, rpcElement);
        doc.appendChild(rpc);
        Element editConfig = doc.createElement(editConfigTag);
        rpc.appendChild(editConfig);
        Element target = doc.createElement(targetTag);
        editConfig.appendChild(target);
        target.appendChild(doc.createElement(candidateTag));

        Element config = doc.createElement(configTag);
        config.setAttributeNS(xmlNamespaceUri,
                xmlNamespaceQualifiedName,
                xmlNamespaceValue);
        editConfig.appendChild(config);


        for (CharSequence node : nodes) {
            Document ldoc = toDocument(CharSource.wrap(node));
            Element cfgRoot = ldoc.getDocumentElement();

            // is everything as merge, ok?
            cfgRoot.setAttribute(attributeName, attributeValue);

            // move (or copy) node to another Document
            config.appendChild(Optional.ofNullable(doc.adoptNode(cfgRoot))
                    .orElseGet(() -> doc.importNode(cfgRoot, true)));
        }

        /* Capture the RPC request in a variable to send */
        String rpcReq = String.valueOf(XmlString.prettifyXml(toCharSequence(doc)));
        String rpcReply = null;

        /* send the RPC request along with a commit message */
        if (uri != null) {
            NetconfSession netconfSession = getNetconfSession(DeviceId.deviceId(uri));
            try {
                printlog("Sending an RPC request\n{}", XmlString.prettifyXml(rpcReq));
                rpcReply = netconfSession.rpc(toCharSequence(doc, false).toString()).join();
                if (!rpcReply.contains(netconfOkTag)) {
                    log.error("Got back an error RPC response");
                } else {
                    printlog("{}", "Got back a successful RPC response. Committing the state.");
                    rpcReply = netconfSession.doWrappedRpc(commitTag);
                }
            } catch (NetconfException netconfException) {
                log.error("Exception occurred while sending the RPC Request: {}", netconfException);
            }
            printlog("{}", rpcReply);
        }
    }

    private NetconfSession getNetconfSession(DeviceId deviceId) {
        NetconfController netconfController = checkNotNull(get(NetconfController.class));
        NetconfDevice netconfDevice = netconfController.getDevicesMap().get(deviceId);
        return checkNotNull(netconfDevice.getSession());
    }
}
