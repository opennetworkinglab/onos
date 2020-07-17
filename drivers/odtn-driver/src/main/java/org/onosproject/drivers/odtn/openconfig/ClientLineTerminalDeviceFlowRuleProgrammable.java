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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn.openconfig;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.drivers.odtn.impl.FlowRuleParser;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.PortNumber;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of FlowRuleProgrammable interface for
 * OpenConfig terminal devices.
 */
public class ClientLineTerminalDeviceFlowRuleProgrammable
        extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private static final Logger log =
            LoggerFactory.getLogger(ClientLineTerminalDeviceFlowRuleProgrammable.class);

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final String PREFIX_PORT = "port-";
    private static final String PREFIX_CHANNEL = "channel-";
    private static final String DEFAULT_OPERATIONAL_MODE = "0";
    private static final String DEFAULT_TARGET_POWER = "0";
    private static final String DEFAULT_ASSIGNMENT_INDEX = "1";
    private static final String DEFAULT_ALLOCATION_INDEX = "10";
    private static final int DEFAULT_RULE_PRIORITY = 10;
    private static final long DEFAULT_RULE_COOKIE = 1234L;
    private static final String OPERATION_DISABLE = "DISABLED";
    private static final String OPERATION_ENABLE = "ENABLED";
    private static final String OC_TYPE_PROT_OTN = "oc-opt-types:PROT_OTN";
    private static final String OC_TYPE_PROT_ETH = "oc-opt-types:PROT_ETHERNET";


    /**
     * Apply the flow entries specified in the collection rules.
     *
     * @param rules A collection of Flow Rules to be applied
     * @return The collection of added Flow Entries
     */
    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            openConfigError("null session");
            return ImmutableList.of();
        }

        // Apply the  rules on the device
        Collection<FlowRule> added = rules.stream()
                .map(r -> new TerminalDeviceFlowRule(r, getLinePorts()))
                .filter(xc -> applyFlowRule(session, xc))
                .collect(Collectors.toList());

        for (FlowRule flowRule : added) {
            log.info("OpenConfig added flowrule {}", flowRule);
            getConnectionCache().add(did(), ((TerminalDeviceFlowRule) flowRule).connectionName(), flowRule);
        }

        //Print out number of rules sent to the device (without receiving errors)
        openConfigLog("applyFlowRules added {}", added.size());
        return added;
    }

    /**
     * Get the flow entries that are present on the device.
     *
     * @return A collection of Flow Entries
     */
    @Override
    public Collection<FlowEntry> getFlowEntries() {
        log.debug("getFlowEntries device {} cache size {}", did(), getConnectionCache().size(did()));

        Collection<FlowEntry> fetched = fetchConnectionsFromDevice().stream()
                .map(fr -> new DefaultFlowEntry(fr, FlowEntry.FlowEntryState.ADDED, 0, 0, 0))
                .collect(Collectors.toList());

        //Print out number of rules actually found on the device that are also included in the cache
        openConfigLog("getFlowEntries fetched connections {}", fetched.size());

        return fetched;
    }

    /**
     * Remove the specified flow rules.
     *
     * @param rules A collection of Flow Rules to be removed
     * @return The collection of removed Flow Entries
     */
    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            openConfigError("null session");
            return ImmutableList.of();
        }
        List<FlowRule> removed = new ArrayList<>();
        for (FlowRule r : rules) {
            try {
                TerminalDeviceFlowRule termFlowRule = new TerminalDeviceFlowRule(r, getLinePorts());
                removeFlowRule(session, termFlowRule);
                getConnectionCache().remove(did(), termFlowRule.connectionName());
                removed.add(r);
            } catch (Exception e) {
                openConfigError("Error {}", e);
                continue;
            }
        }

        //Print out number of removed rules from the device (without receiving errors)
        openConfigLog("removeFlowRules removed {}", removed.size());

        return removed;
    }

    private DeviceConnectionCache getConnectionCache() {
        return DeviceConnectionCache.init();
    }

    // Context so XPath expressions are aware of XML namespaces
    private static final NamespaceContext NS_CONTEXT = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix.equals("oc-platform-types")) {
                return "http://openconfig.net/yang/platform-types";
            }
            if (prefix.equals("oc-opt-term")) {
                return "http://openconfig.net/yang/terminal-device";
            }
            return null;
        }

        @Override
        public Iterator getPrefixes(String val) {
            return null;
        }

        @Override
        public String getPrefix(String uri) {
            return null;
        }
    };


    /**
     * Helper method to get the device id.
     */
    private DeviceId did() {
        return data().deviceId();
    }

    /**
     * Helper method to log from this class adding DeviceId.
     */
    private void openConfigLog(String format, Object... arguments) {
        log.info("OPENCONFIG {}: " + format, did(), arguments);
    }

    /**
     * Helper method to log an error from this class adding DeviceId.
     */
    private void openConfigError(String format, Object... arguments) {
        log.error("OPENCONFIG {}: " + format, did(), arguments);
    }


    /**
     * Helper method to get the Netconf Session.
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(did()).getSession();
    }


    /**
     * Construct a String with a Netconf filtered get RPC Message.
     *
     * @param filter A valid XML tree with the filter to apply in the get
     * @return a String containing the RPC XML Document
     */
    private String filteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

    /**
     * Construct a get request to retrieve Components and their
     * properties (for the ONOS port, index).
     *
     * @return The filt content to send to the device.
     */
    private String getComponents() {
        StringBuilder filt = new StringBuilder();
        filt.append("<components xmlns='http://openconfig.net/yang/platform'>");
        filt.append(" <component>");
        filt.append("  <name/>");
        filt.append("  <properties/>");
        filt.append(" </component>");
        filt.append("</components>");
        return filteredGetBuilder(filt.toString());
    }

    /**
     * Construct a get request to retrieve Optical Channels and
     * the line port they are using.
     * <p>
     * This method is used to query the device so we can find the
     * OpticalChannel component name that used a given line port.
     *
     * @return The filt content to send to the device.
     */
    private String getOpticalChannels() {
        StringBuilder filt = new StringBuilder();
        filt.append("<components xmlns='http://openconfig.net/yang/platform'>");
        filt.append(" <component>");
        filt.append("  <name/>");
        filt.append("  <state/>");
        filt.append("  <oc-opt-term:optical-channel xmlns:oc-opt-term"
                + " = 'http://openconfig.net/yang/terminal-device'>");
        filt.append("    <oc-opt-term:config>");
        filt.append("     <oc-opt-term:line-port/>");
        filt.append("    </oc-opt-term:config>");
        filt.append("  </oc-opt-term:optical-channel>");
        filt.append(" </component>");
        filt.append("</components>");
        return filteredGetBuilder(filt.toString());
    }

    /**
     * Get the OpenConfig component name for the OpticalChannel component
     * associated to the passed port number (typically a line side port, already
     * mapped to ONOS port).
     *
     * @param session    The netconf session to the device.
     * @param portNumber ONOS port number of the Line port ().
     * @return the channel component name or null
     */
    private String getOpticalChannel(NetconfSession session,
                                     PortNumber portNumber) {
        try {
            checkNotNull(session);
            checkNotNull(portNumber);
            XPath xp = XPathFactory.newInstance().newXPath();
            xp.setNamespaceContext(NS_CONTEXT);

            // Get the port name for a given port number
            // We could iterate the port annotations too, no need to
            // interact with device.
            String xpGetPortName =
                    "/rpc-reply/data/components/"
                            +
                            "component[./properties/property[name='onos-index']/config/value ='" +
                            portNumber.toLong() + "']/"
                            + "name/text()";

            // Get all the components and their properties
            String compReply = session.rpc(getComponents()).get();
            DocumentBuilderFactory builderFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document document =
                    builder.parse(new InputSource(new StringReader(compReply)));
            String portName = xp.evaluate(xpGetPortName, document);
            String xpGetOptChannelName =
                    "/rpc-reply/data/components/"
                            + "component[./optical-channel/config/line-port='" + portName +
                            "']/name/text()";

            String optChannelReply = session.rpc(getOpticalChannels()).get();
            document =
                    builder.parse(new InputSource(new StringReader(optChannelReply)));
            return xp.evaluate(xpGetOptChannelName, document);
        } catch (Exception e) {
            openConfigError("Exception {}", e);
            return null;
        }
    }

    private void setLogicalChannel(NetconfSession session, String operation, String logChannel)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'>");
        sb.append("<logical-channels>");
        sb.append("<channel>");
        sb.append("<index>" + logChannel + "</index>");
        sb.append("<config>");
        sb.append("<admin-state>" + operation + "</admin-state>");
        sb.append("</config>");
        sb.append("</channel>");
        sb.append("</logical-channels>");
        sb.append("</terminal-device>");

        boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing the logical channel");
        }
    }

    private void setOpticalChannelFrequency(NetconfSession session, String optChannel, Frequency freq)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        sb.append("<components xmlns='http://openconfig.net/yang/platform'>");
        sb.append("<component>");
        sb.append("<name>" + PREFIX_CHANNEL + optChannel + "</name>");
        sb.append("<oc-opt-term:optical-channel xmlns:oc-opt-term='http://openconfig.net/yang/terminal-device'>");
        sb.append("<oc-opt-term:config>");
        sb.append("<oc-opt-term:frequency>" + (long) freq.asMHz() + "</oc-opt-term:frequency>");
        sb.append("<oc-opt-term:target-output-power>" + DEFAULT_TARGET_POWER + "</oc-opt-term:target-output-power>");
        sb.append("<oc-opt-term:operational-mode>" + DEFAULT_OPERATIONAL_MODE + "</oc-opt-term:operational-mode>");
        sb.append("<oc-opt-term:line-port>" + PREFIX_PORT + optChannel + "</oc-opt-term:line-port>");
        sb.append("</oc-opt-term:config>");
        sb.append("</oc-opt-term:optical-channel>");
        sb.append("</component>");
        sb.append("</components>");

        boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing channel frequency");
        }
    }

    private void setLogicalChannelAssignment(NetconfSession session, String operation, String client, String line,
                                             String assignmentIndex, String allocationIndex)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        sb.append("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'>");
        sb.append("<logical-channels>");
        sb.append("<channel>");
        sb.append("<index>" + client + "</index>");
        sb.append("<config>");
        sb.append("<admin-state>" + operation + "</admin-state>");
        sb.append("</config>");
        sb.append("<logical-channel-assignments>");
        sb.append("<assignment>");
        sb.append("<index>" + assignmentIndex + "</index>");
        sb.append("<config>");
        sb.append("<logical-channel>" + line + "</logical-channel>");
        sb.append("<allocation>" + allocationIndex + "</allocation>");
        sb.append("</config>");
        sb.append("</assignment>");
        sb.append("</logical-channel-assignments>");
        sb.append("</channel>");
        sb.append("</logical-channels>");
        sb.append("</terminal-device>");

        boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing logical channel assignment");
        }
    }

    /**
     * Apply a single flowrule to the device.
     *
     * --- Directionality details:
     * Driver supports ADD (INGRESS) and DROP (EGRESS) rules generated by OpticalCircuit/OpticalConnectivity intents
     * the format of the rules are checked in class TerminalDeviceFlowRule
     *
     * However, the physical transponder is always bidirectional as specified in OpenConfig YANG models
     * therefore ADD and DROP rules are mapped in the same xml that ENABLE (and tune) a transponder port.
     *
     * If the intent is generated as bidirectional both ADD and DROP flowrules are generated for each device, thus
     * the same xml is sent twice to the device.
     *
     * @param session   The Netconf session.
     * @param rule      Flow Rules to be applied.
     * @return true if no Netconf errors are received from the device when xml is sent
     * @throws NetconfException if exchange goes wrong
     */
    protected boolean applyFlowRule(NetconfSession session, TerminalDeviceFlowRule rule) {

        //Configuration of LINE side, used for OpticalConnectivity intents
        //--- configure central frequency
        //--- enable the line port
        if (rule.type == TerminalDeviceFlowRule.Type.LINE_INGRESS ||
                rule.type == TerminalDeviceFlowRule.Type.LINE_EGRESS) {

            FlowRuleParser frp = new FlowRuleParser(rule);
            String componentName = frp.getPortNumber().toString();
            Frequency centralFrequency = frp.getCentralFrequency();

            StringBuilder componentConf = new StringBuilder();

            log.info("Sending LINE FlowRule to device {} LINE port {}, frequency {}",
                    did(), componentName, centralFrequency);

            try {
                setOpticalChannelFrequency(session, componentName, centralFrequency);
            } catch (NetconfException e) {
                log.error("Error writing central frequency in the component");
                return false;
            }

            try {
                setLogicalChannel(session, OPERATION_ENABLE, componentName);
            } catch (NetconfException e) {
                log.error("Error enabling the logical channel");
                return false;
            }
        }

        //Configuration of CLIENT side, used for OpticalCircuit intents
        //--- associate the client port to the line port
        //--- enable the client port
        //
        //Assumes only one "assignment" per logical-channel with index 1
        //TODO check the OTN mapping of client ports into the line port frame specified by parameter "<allocation>"
        if (rule.type == TerminalDeviceFlowRule.Type.CLIENT_INGRESS ||
                rule.type == TerminalDeviceFlowRule.Type.CLIENT_EGRESS) {

            String clientPortName;
            String linePortName;
            if (rule.type == TerminalDeviceFlowRule.Type.CLIENT_INGRESS) {
                clientPortName = rule.inPort().toString();
                linePortName = rule.outPort().toString();
            } else {
                clientPortName = rule.outPort().toString();
                linePortName = rule.inPort().toString();
            }

            log.info("Sending CLIENT FlowRule to device {} CLIENT port: {}, LINE port {}",
                    did(), clientPortName, linePortName);

            try {
                setLogicalChannelAssignment(session, OPERATION_ENABLE, clientPortName, linePortName,
                        DEFAULT_ASSIGNMENT_INDEX, DEFAULT_ALLOCATION_INDEX);
            } catch (NetconfException e) {
                log.error("Error setting the logical channel assignment");
                return false;
            }
        }

        return true;
    }

    protected boolean removeFlowRule(NetconfSession session, TerminalDeviceFlowRule rule)
            throws NetconfException {

        //Configuration of LINE side, used for OpticalConnectivity intents
        //--- configure central frequency to ZERO
        //--- disable the line port
        if (rule.type == TerminalDeviceFlowRule.Type.LINE_INGRESS ||
                rule.type == TerminalDeviceFlowRule.Type.LINE_EGRESS) {

            FlowRuleParser frp = new FlowRuleParser(rule);
            String componentName = frp.getPortNumber().toString();

            log.info("Removing LINE FlowRule device {} line port {}", did(), componentName);

            try {
                setLogicalChannel(session, OPERATION_DISABLE, componentName);
            } catch (NetconfException e) {
                log.error("Error disabling the logical channel line side");
                return false;
            }
        }

        //Configuration of CLIENT side, used for OpticalCircuit intents
        //--- configure central frequency to ZERO
        //--- disable the line port
        if (rule.type == TerminalDeviceFlowRule.Type.CLIENT_INGRESS ||
                rule.type == TerminalDeviceFlowRule.Type.CLIENT_EGRESS) {

            String clientPortName;
            String linePortName;
            if (rule.type == TerminalDeviceFlowRule.Type.CLIENT_INGRESS) {
                clientPortName = rule.inPort().toString();
                linePortName = rule.outPort().toString();
            } else {
                clientPortName = rule.outPort().toString();
                linePortName = rule.inPort().toString();
            }

            log.debug("Removing CLIENT FlowRule device {} client port: {}, line port {}",
                    did(), clientPortName, linePortName);

            try {
                setLogicalChannelAssignment(session, OPERATION_DISABLE, clientPortName, linePortName,
                        DEFAULT_ASSIGNMENT_INDEX, DEFAULT_ALLOCATION_INDEX);
            } catch (NetconfException e) {
                log.error("Error disabling the logical channel assignment");
                return false;
            }
        }

        return true;
    }

    private List<FlowRule> fetchLineConnectionFromDevice(String channel, Frequency centralFreq) {
        List<FlowRule> confirmedRules = new ArrayList<>();
        FlowRule cacheAddRule;
        FlowRule cacheDropRule;
        NetconfSession session = getNetconfSession();

        log.debug("fetchOpticalConnectionsFromDevice {} frequency {}", did(), centralFreq);

        //Build the corresponding flow rule as expected
        //Selector including port and ochSignal
        //Treatment including port
        PortNumber inputPortNumber = PortNumber.portNumber(channel);
        PortNumber outputPortNumber = PortNumber.portNumber(channel);

        log.debug("fetchOpticalConnectionsFromDevice {} port {}-{}", did(), inputPortNumber, outputPortNumber);

        TrafficSelector selectorDrop = DefaultTrafficSelector.builder()
                .matchInPort(inputPortNumber)
                .add(Criteria.matchLambda(toOchSignal(centralFreq, 50.0)))
                .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
                .build();

        TrafficTreatment treatmentDrop = DefaultTrafficTreatment.builder()
                .setOutput(outputPortNumber)
                .build();

        TrafficSelector selectorAdd = DefaultTrafficSelector.builder()
                .matchInPort(inputPortNumber)
                .build();

        TrafficTreatment treatmentAdd = DefaultTrafficTreatment.builder()
                .add(Instructions.modL0Lambda(toOchSignal(centralFreq, 50.0)))
                .setOutput(outputPortNumber)
                .build();

        //Retrieved rules and cached rules are considered equal if both selector and treatment are equal
        cacheAddRule = null;
        cacheDropRule = null;
        if (getConnectionCache().size(did()) != 0) {
            cacheDropRule = getConnectionCache().get(did()).stream()
                    .filter(r -> (r.selector().equals(selectorDrop) && r.treatment().equals(treatmentDrop)))
                    .findFirst()
                    .orElse(null);

            cacheAddRule = getConnectionCache().get(did()).stream()
                    .filter(r -> (r.selector().equals(selectorAdd) && r.treatment().equals(treatmentAdd)))
                    .findFirst()
                    .orElse(null);
        }

        //Include the DROP rule to the retrieved rules if found in cache
        if ((cacheDropRule != null)) {
            confirmedRules.add(cacheDropRule);
            log.debug("fetchOpticalConnectionsFromDevice {} DROP LINE rule included in the cache {}",
                    did(), cacheDropRule);
        } else {
            log.warn("fetchOpticalConnectionsFromDevice {} DROP LINE rule not included in cache", did());
        }

        //Include the ADD rule to the retrieved rules if found in cache
        if ((cacheAddRule != null)) {
            confirmedRules.add(cacheAddRule);
            log.debug("fetchOpticalConnectionsFromDevice {} ADD LINE rule included in the cache {}",
                    did(), cacheAddRule.selector());
        } else {
            log.warn("fetchOpticalConnectionsFromDevice {} ADD LINE rule not included in cache", did());
        }

        //If neither Add or Drop rules are present in the cache, remove configuration from the device
        if ((cacheDropRule == null) && (cacheAddRule == null)) {
            log.warn("fetchOpticalConnectionsFromDevice {} ADD and DROP rule not included in the cache", did());

            FlowRule deviceDropRule = DefaultFlowRule.builder()
                    .forDevice(data().deviceId())
                    .makePermanent()
                    .withSelector(selectorDrop)
                    .withTreatment(treatmentDrop)
                    .withCookie(DEFAULT_RULE_COOKIE)
                    .withPriority(DEFAULT_RULE_PRIORITY)
                    .build();

            FlowRule deviceAddRule = DefaultFlowRule.builder()
                    .forDevice(data().deviceId())
                    .makePermanent()
                    .withSelector(selectorAdd)
                    .withTreatment(treatmentAdd)
                    .withCookie(DEFAULT_RULE_COOKIE)
                    .withPriority(DEFAULT_RULE_PRIORITY)
                    .build();

            try {
                //TODO this is not required if allowExternalFlowRules
                TerminalDeviceFlowRule addRule = new TerminalDeviceFlowRule(deviceAddRule, getLinePorts());
                removeFlowRule(session, addRule);

                TerminalDeviceFlowRule dropRule = new TerminalDeviceFlowRule(deviceDropRule, getLinePorts());
                removeFlowRule(session, dropRule);
            } catch (NetconfException e) {
                openConfigError("Error removing LINE rule from device", e);
            }
        }
        return confirmedRules;
    }

    private List<FlowRule> fetchClientConnectionFromDevice(PortNumber clientPortNumber, PortNumber linePortNumber) {
        List<FlowRule> confirmedRules = new ArrayList<>();
        FlowRule cacheAddRule;
        FlowRule cacheDropRule;
        NetconfSession session = getNetconfSession();

        //Build the corresponding flow rule as expected
        //Selector including port
        //Treatment including port

        log.debug("fetchClientConnectionsFromDevice {} client {} line {}", did(), clientPortNumber, linePortNumber);

        TrafficSelector selectorDrop = DefaultTrafficSelector.builder()
                .matchInPort(linePortNumber)
                .build();

        TrafficTreatment treatmentDrop = DefaultTrafficTreatment.builder()
                .setOutput(clientPortNumber)
                .build();

        TrafficSelector selectorAdd = DefaultTrafficSelector.builder()
                .matchInPort(clientPortNumber)
                .build();

        TrafficTreatment treatmentAdd = DefaultTrafficTreatment.builder()
                .setOutput(linePortNumber)
                .build();

        //Retrieved rules and cached rules are considered equal if both selector and treatment are equal
        cacheAddRule = null;
        cacheDropRule = null;
        if (getConnectionCache().size(did()) != 0) {
            cacheDropRule = getConnectionCache().get(did()).stream()
                    .filter(r -> (r.selector().equals(selectorDrop) && r.treatment().equals(treatmentDrop)))
                    .findFirst()
                    .orElse(null);

            cacheAddRule = getConnectionCache().get(did()).stream()
                    .filter(r -> (r.selector().equals(selectorAdd) && r.treatment().equals(treatmentAdd)))
                    .findFirst()
                    .orElse(null);
        }

        //Include the DROP rule to the retrieved rules if found in cache
        if ((cacheDropRule != null)) {
            confirmedRules.add(cacheDropRule);
            log.debug("fetchClientConnectionsFromDevice {} DROP CLIENT rule in the cache {}",
                    did(), cacheDropRule);
        } else {
            log.warn("fetchClientConnectionsFromDevice {} DROP CLIENT rule not found in cache", did());
        }

        //Include the ADD rule to the retrieved rules if found in cache
        if ((cacheAddRule != null)) {
            confirmedRules.add(cacheAddRule);
            log.debug("fetchClientConnectionsFromDevice {} ADD CLIENT rule in the cache {}",
                    did(), cacheAddRule);
        } else {
            log.warn("fetchClientConnectionsFromDevice {} ADD CLIENT rule not found in cache", did());
        }

        if ((cacheDropRule == null) && (cacheAddRule == null)) {
            log.warn("fetchClientConnectionsFromDevice {} ADD and DROP rule not included in the cache", did());

            FlowRule deviceDropRule = DefaultFlowRule.builder()
                    .forDevice(data().deviceId())
                    .makePermanent()
                    .withSelector(selectorDrop)
                    .withTreatment(treatmentDrop)
                    .withCookie(DEFAULT_RULE_COOKIE)
                    .withPriority(DEFAULT_RULE_PRIORITY)
                    .build();

            FlowRule deviceAddRule = DefaultFlowRule.builder()
                    .forDevice(data().deviceId())
                    .makePermanent()
                    .withSelector(selectorAdd)
                    .withTreatment(treatmentAdd)
                    .withCookie(DEFAULT_RULE_COOKIE)
                    .withPriority(DEFAULT_RULE_PRIORITY)
                    .build();

            try {
                //TODO this is not required if allowExternalFlowRules
                TerminalDeviceFlowRule addRule = new TerminalDeviceFlowRule(deviceAddRule, getLinePorts());
                removeFlowRule(session, addRule);

                TerminalDeviceFlowRule dropRule = new TerminalDeviceFlowRule(deviceDropRule, getLinePorts());
                removeFlowRule(session, dropRule);
            } catch (NetconfException e) {
                openConfigError("Error removing CLIENT rule from device", e);
            }
        }
        return confirmedRules;
    }

    /**
     * Fetches list of connections from device.
     *
     * TODO manage allow external flow rules (allowExternalFlowRules)
     * Currently removes from the device all connections that are not currently present in the DeviceConnectionCache.
     *
     * @return connections that are present on the device and in the DeviceConnectionCache.
     */
    private List<FlowRule> fetchConnectionsFromDevice() {
        List<FlowRule> confirmedRules = new ArrayList<>();
        String reply;
        FlowRule cacheAddRule;
        FlowRule cacheDropRule;
        NetconfSession session = getNetconfSession();

        //Get relevant information from the device
        StringBuilder requestFilter = new StringBuilder();
        requestFilter.append("<components xmlns='http://openconfig.net/yang/platform'>");
        requestFilter.append("  <component>");
        requestFilter.append("    <name/>");
        requestFilter.append("    <oc-opt-term:optical-channel " +
                "xmlns:oc-opt-term='http://openconfig.net/yang/terminal-device'>");
        requestFilter.append("      <oc-opt-term:config/>");
        requestFilter.append("    </oc-opt-term:optical-channel>");
        requestFilter.append("  </component>");
        requestFilter.append("</components>");
        requestFilter.append("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'>");
        requestFilter.append("  <logical-channels>");
        requestFilter.append("    <channel>");
        requestFilter.append("      <index/>");
        requestFilter.append("      <config>");
        requestFilter.append("        <admin-state/>");
        requestFilter.append("        <logical-channel-type/>");
        requestFilter.append("      </config>");
        requestFilter.append("      <logical-channel-assignments>");
        requestFilter.append("        <assignment>");
        requestFilter.append("          <config>");
        requestFilter.append("            <logical-channel/>");
        requestFilter.append("          </config>");
        requestFilter.append("        </assignment>");
        requestFilter.append("      </logical-channel-assignments>");
        requestFilter.append("    </channel>");
        requestFilter.append("  </logical-channels>");
        requestFilter.append("</terminal-device>");

        try {
            reply = session.get(requestFilter.toString(), null);
            //log.debug("TRANSPONDER CONNECTIONS - fetchConnectionsFromDevice {} reply {}", did(), reply);
        } catch (NetconfException e) {
            log.error("Failed to retrieve configuration details for device {}", handler().data().deviceId(), e);
            return ImmutableList.of();
        }

        HierarchicalConfiguration cfg = XmlConfigParser.loadXml(new ByteArrayInputStream(reply.getBytes()));

        List<HierarchicalConfiguration> logicalChannels =
                cfg.configurationsAt("data.terminal-device.logical-channels.channel");

        List<HierarchicalConfiguration> components =
                cfg.configurationsAt("data.components.component");

        //Retrieve the ENABLED line ports
        List<String> enabledOpticalChannels = logicalChannels.stream()
                .filter(r -> r.getString("config.logical-channel-type").equals(OC_TYPE_PROT_OTN))
                .filter(r -> r.getString("config.admin-state").equals(OPERATION_ENABLE))
                .map(r -> r.getString("index"))
                .collect(Collectors.toList());

        log.debug("fetchConnectionsFromDevice {} enabledOpticalChannelsIndex {}", did(), enabledOpticalChannels);

        if (enabledOpticalChannels.size() != 0) {
            for (String channel : enabledOpticalChannels) {
                log.debug("fetchOpticalConnectionsFromDevice {} channel {}", did(), channel);

                //Retrieve the corresponding central frequency from the associated component
                //TODO correlate the components instead of relying on naming
                Frequency centralFreq = components.stream()
                        .filter(c -> c.getString("name").equals(PREFIX_CHANNEL + channel))
                        .map(c -> c.getDouble("optical-channel.config.frequency"))
                        .map(c -> Frequency.ofMHz(c))
                        .findFirst()
                        .orElse(null);

                confirmedRules.addAll(fetchLineConnectionFromDevice(channel, centralFreq));
            }
        }

        //Retrieve the ENABLED client ports
        List<String> enabledClientChannels = logicalChannels.stream()
                .filter(r -> r.getString("config.logical-channel-type").equals(OC_TYPE_PROT_ETH))
                .filter(r -> r.getString("config.admin-state").equals(OPERATION_ENABLE))
                .map(r -> r.getString("index"))
                .collect(Collectors.toList());

        log.debug("fetchClientConnectionsFromDevice {} enabledClientChannelsIndex {}", did(), enabledClientChannels);

        if (enabledClientChannels.size() != 0) {
            for (String clientPort : enabledClientChannels) {

                log.debug("fetchClientConnectionsFromDevice {} channel {}", did(), clientPort);

                String linePort = logicalChannels.stream()
                    .filter(r -> r.getString("config.logical-channel-type").equals(OC_TYPE_PROT_ETH))
                    .filter(r -> r.getString("config.admin-state").equals(OPERATION_ENABLE))
                    .filter(r -> r.getString("index").equals(clientPort))
                    .map(r -> r.getString("logical-channel-assignments.assignment.config.logical-channel"))
                    .findFirst()
                    .orElse(null);

                //Build the corresponding flow rule as expected
                //Selector including port
                //Treatment including port
                PortNumber clientPortNumber = PortNumber.portNumber(clientPort);
                PortNumber linePortNumber = PortNumber.portNumber(linePort);

                confirmedRules.addAll(fetchClientConnectionFromDevice(clientPortNumber, linePortNumber));
            }
        }

        //Returns rules that are both on the device and on the cache
        if (confirmedRules.size() != 0) {
            log.info("fetchConnectionsFromDevice {} number of confirmed rules {}", did(), confirmedRules.size());
            return confirmedRules;
        } else {
            return ImmutableList.of();
        }
    }

    /**
     * Convert start and end frequencies to OCh signal.
     *
     * FIXME: supports channel spacing 50 and 100
     *
     * @param central central frequency as double in THz
     * @param width width of the channel arounf the central frequency as double in GHz
     * @return OCh signal
     */
    public static OchSignal toOchSignal(Frequency central, double width) {
        int slots = (int) (width / ChannelSpacing.CHL_12P5GHZ.frequency().asGHz());
        int multiplier = 0;

        double centralAsGHz = central.asGHz();

        if (width == 50) {
            multiplier = (int) ((centralAsGHz - Spectrum.CENTER_FREQUENCY.asGHz())
                    / ChannelSpacing.CHL_50GHZ.frequency().asGHz());

            return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, multiplier, slots);
        }

        if (width == 100) {
            multiplier = (int) ((centralAsGHz - Spectrum.CENTER_FREQUENCY.asGHz())
                    / ChannelSpacing.CHL_100GHZ.frequency().asGHz());

            return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, multiplier, slots);
        }

        return null;
    }

    private List<PortNumber> getLinePorts() {
        List<PortNumber> linePorts;

        DeviceService deviceService = this.handler().get(DeviceService.class);
        linePorts = deviceService.getPorts(data().deviceId()).stream()
                .filter(p -> p.annotations().value(OdtnDeviceDescriptionDiscovery.PORT_TYPE)
                        .equals(OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.value()))
                .map(p -> p.number())
                .collect(Collectors.toList());

        return linePorts;

    }
}
