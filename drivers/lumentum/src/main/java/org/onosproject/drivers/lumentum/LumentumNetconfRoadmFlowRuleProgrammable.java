/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.lumentum;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.driver.optical.flowrule.CrossConnectCache;
import org.onosproject.driver.optical.flowrule.CrossConnectFlowRule;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.HashSet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of FlowRuleProgrammable interface for Lumentum ROADM-A Whitebox devices using NETCONF.
 */
public class LumentumNetconfRoadmFlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private static final Logger log =
            LoggerFactory.getLogger(LumentumNetconfRoadmFlowRuleProgrammable.class);

    private static final String DN = "dn";
    private static final String DN_PORT = "port=";
    private static final String DN_CARD1 = "ne=1;chassis=1;card=1;port=";
    private static final String CONNECTION = "connection";
    private static final String CONNECTIONS = "data.connections.connection";
    private static final String CONFIG = "config";
    private static final String STATE = "state";
    private static final String START_FREQ = "start-freq";
    private static final String END_FREQ = "end-freq";
    private static final String MODULE = "module";
    private static final String SEMI_COLON = ";";
    private static final String EQUAL = "=";
    private static final String INPUT_PORT_REFERENCE = "input-port-reference";
    private static final String OUTPUT_PORT_REFERENCE = "output-port-reference";

    private static final String CHANNEL_ATTENUATION = "attenuation";
    private static final String CHANNEL_INPUT_POWER = "input-channel-attributes.power";
    private static final String CHANNEL_OUTPUT_POWER = "output-channel-attributes.power";

    protected static final long LINE_PORT = 3001;
    protected static final PortNumber LINE_PORT_NUMBER = PortNumber.portNumber(LINE_PORT);
    protected static final long MUX_OUT = 4201;
    protected static final long DEMUX_IN = 5101;
    protected static final long GHZ = 1_000_000_000L;
    protected static final int MAX_CONNECTIONS = 100;

    //List of LumentumConnections to associate ConnectionId and other info to the relative hash

    //This is required because CrossConnect, CrossConnect Cache do not include all parameters required by Lumentum
    //TODO: Use an external cache as CrossConnectCache to avoid problems in case of multiple devices using this driver

    protected static final Set<LumentumConnection> CONNECTION_SET = new HashSet<>();

    /**Get the flow entries that are present on the Lumentum device, called by FlowRuleDriverProvider.
     *
     * The flow entries must match exactly the FlowRule entries in the ONOS store. If they are not an
     * exact match the device will be requested to remove those flows.
     *
     * @return A collection of Flow Entries
     */
    @Override
    public Collection<FlowEntry> getFlowEntries() {
        return ImmutableList.copyOf(
                fetchConnectionsFromDevice().stream()
                        .map(conn -> buildFlowrule(conn))
                        .filter(Objects::nonNull)
                        .map(fr -> new DefaultFlowEntry(
                                fr, FlowEntry.FlowEntryState.ADDED, 0, 0, 0))
                        .collect(Collectors.toList()));
    }

    /**Apply the flow entries specified in the collection rules.
     *
     * @param rules A collection of Flow Rules to be applied to the Lumentum device
     * @return The collection of added Flow Entries
     */
    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        // Apply the  rules on the device
        Collection<FlowRule> added = rules.stream()
                .map(r -> new CrossConnectFlowRule(r, getLinePorts()))
                .filter(xc -> rpcAddConnection(xc))
                .collect(Collectors.toList());

        // Cache the cookie/priority
        CrossConnectCache cache = this.handler().get(CrossConnectCache.class);
        added.forEach(xc -> cache.set(
                Objects.hash(data().deviceId(), xc.selector(), xc.treatment()),
                xc.id(),
                xc.priority()));

        added.forEach(xc -> log.debug("Lumentum build cached FlowRule selector {} treatment {}",
                xc.selector().toString(), xc.treatment().toString()));

        return added;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        // Remove the valid rules from the device
        Collection<FlowRule> removed = rules.stream()
                .map(r -> new CrossConnectFlowRule(r, getLinePorts()))
                .filter(xc -> rpcDeleteConnection(xc))
                .collect(Collectors.toList());

        // Remove flow rule from cache
        CrossConnectCache cache = this.handler().get(CrossConnectCache.class);
        removed.forEach(xc -> cache.remove(
                Objects.hash(data().deviceId(), xc.selector(), xc.treatment())));

        removed.forEach(xc -> log.debug("Lumentum NETCONF - removed cached FlowRule selector {} treatment {}",
                xc.selector(), xc.treatment()));

        return removed;
    }

    private List<PortNumber> getLinePorts() {
        DeviceService deviceService = this.handler().get(DeviceService.class);
        return deviceService.getPorts(data().deviceId()).stream()
                .filter(p -> p.number().toLong() == LINE_PORT)
                .map(p -> p.number())
                .collect(Collectors.toList());
    }

    /**
     * Fetches list of connections from device.
     *
     * @return list of connections as XML hierarchy
     */
    private List<HierarchicalConfiguration> fetchConnectionsFromDevice() {
        String reply;

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append("<connections xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">");
        requestBuilder.append("</connections>");

        NetconfSession session = getNetconfSession();

        if (session == null) {
            log.error("Lumentum NETCONF - session not found for {}", handler().data().deviceId());
            return ImmutableList.of();
        }

        try {
            reply = session.get(requestBuilder.toString(), null);
            log.info("Lumentum NETCONF - fetchConnectionsFromDevice reply {}", reply);
        } catch (NetconfException e) {
            log.error("Failed to retrieve configuration details for device {}",
                      handler().data().deviceId(), e);
            return ImmutableList.of();
        }

        HierarchicalConfiguration cfg =
                XmlConfigParser.loadXml(new ByteArrayInputStream(reply.getBytes()));

        return cfg.configurationsAt(CONNECTIONS);
    }

    // Example input dn: ne=1;chassis=1;card=1;module=2;connection=89
    private Pair<Short, Short> parseDn(String dn) {
        Short module = null;
        Short connection = null;
        for (String entry : dn.split(SEMI_COLON)) {
            String[] keyVal = entry.split(EQUAL);
            if (keyVal.length != 2) {
                continue;
            }
            if (keyVal[0].equals(MODULE)) {
                module = Short.valueOf(keyVal[1]);
            }
            if (keyVal[0].equals(CONNECTION)) {
                connection = Short.valueOf(keyVal[1]);
            }
            if (module != null && connection != null) {
                return Pair.of(module, connection);
            }
        }

        return null;
    }

    /**
     * Builds a flow rule from a connection hierarchy.
     *
     * @param connection the connection hierarchy
     * @return the flow rule
     */
    private FlowRule buildFlowrule(HierarchicalConfiguration connection) {

        String dn = connection.getString(DN);
        Pair<Short, Short> pair = parseDn(dn);

        HierarchicalConfiguration config = connection.configurationAt(CONFIG);
        double startFreq = config.getDouble(START_FREQ);
        double endFreq = config.getDouble(END_FREQ);
        String inputPortReference = config.getString(INPUT_PORT_REFERENCE);
        String outputPortReference = config.getString(OUTPUT_PORT_REFERENCE);

        HierarchicalConfiguration state = connection.configurationAt(STATE);
        double attenuation = state.getDouble(CHANNEL_ATTENUATION);
        double inputPower = state.getDouble(CHANNEL_INPUT_POWER);
        double outputPower = state.getDouble(CHANNEL_OUTPUT_POWER);

        if (pair == null) {
            return null;
        }

        PortNumber portNumber = getPortNumber(pair.getLeft(), inputPortReference, outputPortReference);

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(pair.getLeft() == 1 ? portNumber : LINE_PORT_NUMBER)
                .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
                .add(Criteria.matchLambda(toOchSignal(startFreq, endFreq)))
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(pair.getLeft() == 1 ? LINE_PORT_NUMBER : portNumber)
                .build();

        log.debug("Lumentum NETCONF - retrieved FlowRule startFreq {} endFreq {}", startFreq, endFreq);

        // Lookup flow ID and priority
        int hash = Objects.hash(data().deviceId(), selector, treatment);
        CrossConnectCache cache = this.handler().get(CrossConnectCache.class);
        Pair<FlowId, Integer> lookup = cache.get(hash);

        LumentumConnection conn = CONNECTION_SET.stream()
                .filter(c -> hash == c.getHash())
                .findFirst()
                .orElse(null);

        //If the flow entry is not in the cache: return null/publish the flow rule
        if ((lookup == null) || (conn == null)) {
           log.error("Lumentum NETCONF connection not in connectionSet {}", pair.getRight());
           rpcDeleteUnwantedConnection(pair.getRight().toString());
           return null;
        } else {
            log.debug("Lumentum NETCONF attenuation and parameters set {} for connection id {}",
                    attenuation,
                    conn.getConnectionId());

            conn.setAttenuation(attenuation);
            conn.setInputPower(inputPower);
            conn.setOutputPower(outputPower);
        }

        return DefaultFlowRule.builder()
                .forDevice(data().deviceId())
                .makePermanent()
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(lookup.getRight())
                .withCookie(lookup.getLeft().value())
                .build();
    }

    /**
     * Get the port number.
     * If this is a MUX connection return input-port. Outport is always MUX_OUT = 4201.
     * If this is a DEMUX connection return output-port. Inport is always DEMUX_IN= 5101.
     *
     * @param module the module (1 for MUX/ADD, 2 for DEMUX/DROP)
     * @return the add/drop port number
     */
    private PortNumber getPortNumber(short module, String inputPort, String outputPort) {
        checkArgument(module == 1 || module == 2, "Module must be 1 (MUX/ADD) or 2 (DEMUX/DROP)");

        if (module == 1) {
            return PortNumber.portNumber(inputPort.split(DN_PORT)[1]);
        } else {
            return PortNumber.portNumber(outputPort.split(DN_PORT)[1]);
        }
    }

    /**
     * Converts cross connect flow rule to module and connection.
     *
     * Connection number is incremental within the class and associated to the rule hash.
     *
     * @param xc the cross connect flow rule
     * @return pair of module (1 for MUX/ADD, 2 for DEMUX/DROP) and connection number
     */
    private Pair<Short, Short> setModuleConnection(CrossConnectFlowRule xc, Integer id) {
        if (xc.isAddRule()) {
            return Pair.of((short) 1, id.shortValue());
        } else {
            return Pair.of((short) 2, id.shortValue());
        }
    }

    /**
     * Retrieve module and connection from the cache.
     *
     * Connection number is incremental within the class and associated to the rule hash.
     *
     * @param xc the cross connect flow rule
     * @return pair of module (1 for MUX/ADD, 2 for DEMUX/DROP) and connection number
     */
    private Pair<Short, Short> retrieveModuleConnection(CrossConnectFlowRule xc) {

        int hash = Objects.hash(data().deviceId(), xc.selector(), xc.treatment());

        LumentumConnection retrievedConnection = CONNECTION_SET.stream()
                .filter(conn -> conn.getHash() == hash)
                .findFirst()
                .orElse(null);

        if (retrievedConnection == null) {
            log.error("Lumentum connection not found");
            return null;
        }

        //Remove connection id from the local cache
        CONNECTION_SET.remove(retrievedConnection);

        log.debug("Lumentum NETCONF - retrieveModuleConnection {} retrievedConnectionId {} port {}",
                xc.isAddRule(), retrievedConnection.getConnectionId(), xc.addDrop());

        if (xc.isAddRule()) {
            return Pair.of((short) 1, retrievedConnection.getConnectionId().shortValue());
        } else {
            return Pair.of((short) 2, retrievedConnection.getConnectionId().shortValue());
        }
    }

    //Following Lumentum documentation rpc operation to configure a new connection
    private boolean rpcAddConnection(CrossConnectFlowRule xc) {

        int currentConnectionId = generateConnectionId();

        if (currentConnectionId == 0) {
            log.error("Lumentum driver - 100 connections are already configured on the device");
            return false;
        }

        LumentumConnection connection = new LumentumConnection(currentConnectionId,
                Objects.hash(data().deviceId(), xc.selector(), xc.treatment()), xc);

        CONNECTION_SET.add(connection);

        Pair<Short, Short> pair = setModuleConnection(xc, currentConnectionId);
        String module = pair.getLeft().toString();
        String connectionId = pair.getRight().toString();

        //Conversion of ochSignal format (center frequency + diameter) to Lumentum frequency slot format (start - end)
        Frequency freqRadius = Frequency.ofHz(xc.ochSignal().channelSpacing().frequency().asHz() / 2);
        Frequency center = xc.ochSignal().centralFrequency();
        String startFreq = String.valueOf(center.subtract(freqRadius).asHz() / GHZ);
        String endFreq = String.valueOf(center.add(freqRadius).asHz() / GHZ);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "\n");
        stringBuilder.append("<add-connection xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">" + "\n");
        stringBuilder.append(
                "<dn>ne=1;chassis=1;card=1;module=" + module + ";connection=" + connectionId + "</dn>" + "\n");
        stringBuilder.append("<start-freq>" + startFreq + "</start-freq>" + "\n");
        stringBuilder.append("<end-freq>" + endFreq + "</end-freq>" + "\n");
        stringBuilder.append("<attenuation>" + "0.0" + "</attenuation>" + "\n");
        stringBuilder.append("<blocked>" + "false" + "</blocked>" + "\n");
        stringBuilder.append("<maintenance-state>" + "in-service" + "</maintenance-state>" + "\n");

        if (xc.isAddRule()) {
            stringBuilder.append(
                    "<input-port-reference>"  + DN_CARD1 + xc.addDrop().toString() + "</input-port-reference>" + "\n");
            stringBuilder.append(
                    "<output-port-reference>" + DN_CARD1 + MUX_OUT + "</output-port-reference>" + "\n");
        } else  {
            stringBuilder.append(
                    "<input-port-reference>"  + DN_CARD1 + DEMUX_IN + "</input-port-reference>" + "\n");
            stringBuilder.append(
                    "<output-port-reference>" + DN_CARD1 + xc.addDrop().toString() + "</output-port-reference>" + "\n");
        }
        stringBuilder.append("<custom-name>" + "onos-connection" + "</custom-name>" + "\n");
        stringBuilder.append("</add-connection>" + "\n");
        stringBuilder.append("</rpc>" + "\n");

        log.info("Lumentum NETCONF - RPC add-connection {}", stringBuilder);

        return editCrossConnect(stringBuilder.toString());
    }

    //Following Lumentum documentation <edit-config> operation to edit connection parameter
    //Currently only edit the "attenuation" parameter
    private boolean editConnection(String moduleId, String connectionId, int attenuation) {

        double attenuationDouble = ((double) attenuation) / 100;

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "\n");
        stringBuilder.append("<edit-config>" + "\n");
        stringBuilder.append("<target>" + "\n");
        stringBuilder.append("<running/>" + "\n");
        stringBuilder.append("</target>" + "\n");
        stringBuilder.append("<config>" + "\n");
        stringBuilder.append("<connections xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">" + "\n");
        stringBuilder.append("<connection>" + "\n");
        stringBuilder.append("" +
                "<dn>ne=1;chassis=1;card=1;module=" + moduleId + ";connection=" + connectionId + "</dn>" + "\n");
        //Other configurable parameters
        //stringBuilder.append("<custom-name/>" + "\n");
        //stringBuilder.append("<maintenance-state>" + "in-service" + "</maintenance-state>" + "\n");
        //stringBuilder.append("<start-freq>" + startFreq + "</start-freq>" + "\n");
        //stringBuilder.append("<end-freq>" + endFreq + "</end-freq>" + "\n");
        stringBuilder.append("<config>" + "\n");
        stringBuilder.append("<attenuation>" + attenuationDouble + "</attenuation>" + "\n");
        stringBuilder.append("</config>" + "\n");
        stringBuilder.append("</connection>" + "\n");
        stringBuilder.append("</connections>" + "\n");
        stringBuilder.append("</config>" + "\n");
        stringBuilder.append("</edit-config>" + "\n");
        stringBuilder.append("</rpc>" + "\n");

        log.info("Lumentum {} - edit-connection {}", data().deviceId(), stringBuilder);

        return editCrossConnect(stringBuilder.toString());
    }

    //Following Lumentum documentation rpc operation to delete a new connection
    private boolean rpcDeleteConnection(CrossConnectFlowRule xc) {
        Pair<Short, Short> pair = retrieveModuleConnection(xc);

        if (pair == null) {
            log.error("Lumentum RPC delete-connection, connection not found on the local cache");
            throw new IllegalStateException("Lumentum RPC delete-connection, connection not found on the local cache");
        }

        String module = pair.getLeft().toString();
        String connection = pair.getRight().toString();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "\n");
        stringBuilder.append("<delete-connection xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">" + "\n");
        stringBuilder.append(
                "<dn>ne=1;chassis=1;card=1;module=" + module + ";connection=" + connection + "</dn>" + "\n");
        stringBuilder.append("</delete-connection>" + "\n");
        stringBuilder.append("</rpc>" + " \n");

        log.info("Lumentum RPC delete-connection {}", stringBuilder);

        return editCrossConnect(stringBuilder.toString());
    }

    //Following Lumentum documentation rpc operation to delete a new connection
    //Executed if for some reason a connection not in the cache is detected
    private boolean rpcDeleteUnwantedConnection(String connectionId) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "\n");
        stringBuilder.append("<delete-connection xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">" + "\n");
        stringBuilder.append("<dn>ne=1;chassis=1;card=1;module=1;connection=" + connectionId + "</dn>" + "\n");
        stringBuilder.append("</delete-connection>" + "\n");
        stringBuilder.append("</rpc>" + "\n");

        log.info("Lumentum {} - RPC delete-connection unwanted {}", data().deviceId(), stringBuilder);

        return editCrossConnect(stringBuilder.toString());
    }


    private boolean editCrossConnect(String xcString) {
        NetconfSession session = getNetconfSession();

        if (session == null) {
            log.error("Lumentum NETCONF - session not found for device {}", handler().data().deviceId());
            return false;
        }

        try {
            return session.editConfig(xcString);
        } catch (NetconfException e) {
            log.error("Failed to edit the CrossConnect edid-cfg for device {}",
                      handler().data().deviceId(), e);
            log.debug("Failed configuration {}", xcString);
            return false;
        }
    }

    private NetconfSession getNetconfSession() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));

        try {
            NetconfSession session = checkNotNull(
                    controller.getNetconfDevice(handler().data().deviceId()).getSession());
            return session;
        } catch (NullPointerException e) {
            log.error("Lumentum NETCONF - session not found for {}", handler().data().deviceId());
            return null;
        }
    }

     /**
     * Convert start and end frequencies to OCh signal.
     *
     * FIXME: assumes slots of 12.5 GHz while devices allows granularity 6.25 GHz
     * FIXME: supports channel spacing 50 and 100
     *
     * @param start starting frequency as double in GHz
     * @param end end frequency as double in GHz
     * @return OCh signal
     */
    public static OchSignal toOchSignal(double start, double end) {
        int slots = (int) ((end - start) / ChannelSpacing.CHL_12P5GHZ.frequency().asGHz());
        int multiplier = 0;

        //Conversion for 50 GHz slots
        if (end - start == 50) {
            multiplier = (int) (((end - start) / 2 + start - Spectrum.CENTER_FREQUENCY.asGHz())
                    / ChannelSpacing.CHL_50GHZ.frequency().asGHz());

            return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, multiplier, slots);
        }

        //Conversion for 100 GHz slots
        if (end - start == 100) {
            multiplier = (int) (((end - start) / 2 + start - Spectrum.CENTER_FREQUENCY.asGHz())
                    / ChannelSpacing.CHL_100GHZ.frequency().asGHz());

            return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, multiplier, slots);
        }

        return null;
    }

    /**
     * Generate a valid connectionId, the connectionId is a field required by the device every time
     * a connection is created/edited/removed.
     *
     *
     * Device only supports connection id < 100
     */
    private static Integer generateConnectionId() {

        //Device only supports connection id < 100
        for (int i = 1; i < MAX_CONNECTIONS; i++) {
            Set<Integer> connIds = CONNECTION_SET.stream()
                    .map(conn -> conn.getConnectionId())
                    .collect(Collectors.toSet());

            if (!connIds.contains(i)) {
                return i;
            }
        }
        return 0;
    }
}
