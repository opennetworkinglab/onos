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
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.PortNumber;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    protected static final int LUMENTUM_ROADM20_MAX_CONNECTIONS = 130;

    /**Get the flow entries that are present on the Lumentum device, called by FlowRuleDriverProvider.
     *
     * The flow entries must match exactly the FlowRule entries in the ONOS store. If they are not an
     * exact match the device will be requested to remove those flows.
     *
     * @return A collection of Flow Entries
     */
    @Override
    public Collection<FlowEntry> getFlowEntries() {
        Collection<FlowEntry> fetched = fetchConnectionsFromDevice().stream()
                .map(conn -> buildFlowrule(conn))
                .map(fr -> new DefaultFlowEntry(fr, FlowEntry.FlowEntryState.ADDED, 0, 0, 0))
                .collect(Collectors.toList());

        //Print out number of rules actually found on the device that are also included in the cache
        log.debug("Device {} getFlowEntries fetched connections {}", did(), fetched.size());

        return fetched;
    }

    /**Apply the flow entries specified in the collection rules.
     *
     * @param rules A collection of Flow Rules to be applied to the Lumentum device
     * @return The collection of added Flow Entries
     */
    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        //Check NETCONF session
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Device {} null session", did());
            return ImmutableList.of();
        }

        // Apply the  rules on the device and add to cache if success
        Collection<FlowRule> added = new ArrayList<>();
        for (FlowRule flowRule : rules) {
            LumentumFlowRule lumFlowRule = new LumentumFlowRule(flowRule, getLinePorts());

            if (rpcAddConnection(lumFlowRule)) {
                added.add(lumFlowRule);
                getConnectionCache().add(did(), lumFlowRule.getConnectionName(), lumFlowRule);
                log.debug("Adding connection with selector {}", lumFlowRule.selector());
            }
        }

        //Print out number of rules sent to the device (without receiving errors)
        log.debug("Device {} applyFlowRules added {}", did(), added.size());

        return added;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Device {} null session", did());
            return ImmutableList.of();
        }

        // Remove the rules from the device and from the cache
        List<FlowRule> removed = new ArrayList<>();
        for (FlowRule r : rules) {
            try {
                LumentumFlowRule flowRule = new LumentumFlowRule(r, getLinePorts());
                rpcDeleteConnection(flowRule);
                getConnectionCache().remove(did(), r);
                removed.add(r);
            } catch (Exception e) {
                log.error("Device {} Error {}", did(), e);
                continue;
            }
        }

        //Print out number of removed rules from the device (without receiving errors)
        log.debug("Device {} removeFlowRules removed {}", did(), removed.size());

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
            log.debug("Lumentum NETCONF - fetchConnectionsFromDevice reply {}", reply);
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
        short connId = pair.getRight();
        short moduleId = pair.getLeft();

        if (pair == null) {
            log.error("Lumentum NETCONF - device {} error in retrieving DN field", did());
            return null;
        }

        log.debug("Lumentum NETCONF - retrieved FlowRule module {} connection {}", moduleId, connId);

        HierarchicalConfiguration config = connection.configurationAt(CONFIG);
        double startFreq = config.getDouble(START_FREQ);
        double endFreq = config.getDouble(END_FREQ);
        String inputPortReference = config.getString(INPUT_PORT_REFERENCE);
        String outputPortReference = config.getString(OUTPUT_PORT_REFERENCE);

        HierarchicalConfiguration state = connection.configurationAt(STATE);
        double attenuation = state.getDouble(CHANNEL_ATTENUATION);
        double inputPower = state.getDouble(CHANNEL_INPUT_POWER);
        double outputPower = state.getDouble(CHANNEL_OUTPUT_POWER);

        PortNumber portNumber = getPortNumber(moduleId, inputPortReference, outputPortReference);

        //If rule is on module 1 it means input port in the Flow rule is contained in portNumber.
        //Otherwise the input port in the Flow rule must is the line port.
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(moduleId == 1 ? portNumber : LINE_PORT_NUMBER)
                .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
                .add(Criteria.matchLambda(toOchSignal(startFreq, endFreq)))
                .build();

        log.debug("Lumentum NETCONF - retrieved FlowRule startFreq {} endFreq {}", startFreq, endFreq);
        log.debug("Lumentum NETCONF - retrieved FlowRule selector {}", selector);

        //Lookup of connection
        //Retrieved rules, cached rules are considered equal if the selector is equal
        FlowRule cacheRule = null;
        if (getConnectionCache().size(did()) != 0) {
            cacheRule = getConnectionCache().get(did()).stream()
                    .filter(r -> (r.selector().equals(selector)))
                    .findFirst()
                    .orElse(null);
        }


        if (cacheRule == null) {
            //TODO consider a way to keep "external" FlowRules
            log.error("Lumentum NETCONF connection {} not in the cache", pair.getRight());
            rpcDeleteExternalConnection(moduleId, connId);
            return null;
        } else {
            //Update monitored values
            log.debug("Attenuation retrieved {} dB for connection {}",
                    attenuation, ((LumentumFlowRule) cacheRule).getConnectionId());
            ((LumentumFlowRule) cacheRule).setAttenuation(attenuation);
            ((LumentumFlowRule) cacheRule).setInputPower(inputPower);
            ((LumentumFlowRule) cacheRule).setOutputPower(outputPower);

            return cacheRule;
        }
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
    private Pair<Short, Short> setModuleIdConnection(LumentumFlowRule xc) {
        int moduleId, connectionId;

        if (xc.isAddRule()) {
            moduleId = 1;
        } else {
            moduleId = 2;
        }

        xc.setConnectionModule(moduleId);

        connectionId = generateConnectionId(xc);

        if (connectionId == 0) {
            log.error("Max connections configured on device module {}", moduleId);
            return null;
        }

        xc.setConnectionId(connectionId);

        return Pair.of((short) moduleId, (short) connectionId);
    }

    //Following Lumentum documentation rpc operation to configure a new connection
    private boolean rpcAddConnection(LumentumFlowRule xc) {

        Pair<Short, Short> pair = setModuleIdConnection(xc);
        String module = pair.getLeft().toString();
        String connectionId = pair.getRight().toString();

        log.debug("Lumentum driver new connection sent moduleId {} connId {}",
                xc.getConnectionModule(),
                xc.getConnectionId());

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

        log.info("Lumentum ROADM20 - RPC add-connection sent to device {}", did());
        log.debug("Lumentum ROADM20 - RPC add-connection sent to device {} {}", did(), stringBuilder);

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

        log.info("Lumentum ROADM20 - edit-connection sent to device {}", did());
        log.debug("Lumentum ROADM20 - edit-connection sent to device {} {}", did(), stringBuilder);

        return editCrossConnect(stringBuilder.toString());
    }

    //Following Lumentum documentation rpc operation to delete a new connection
    private boolean rpcDeleteConnection(LumentumFlowRule xc) {

        //Look for corresponding rule into the cache
        FlowRule cacheRule = getConnectionCache().get(did()).stream()
                .filter(r -> (r.selector().equals(xc.selector()) && r.treatment().equals(xc.treatment())))
                .findFirst()
                .orElse(null);

        if (cacheRule == null) {
            log.error("Lumentum RPC delete-connection, connection not found on the local cache");
            throw new IllegalStateException("Lumentum RPC delete-connection, connection not found on the local cache");
        }


        int moduleId = ((LumentumFlowRule) cacheRule).getConnectionModule();
        int connId = ((LumentumFlowRule) cacheRule).getConnectionId();


        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "\n");
        stringBuilder.append("<delete-connection xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">" + "\n");
        stringBuilder.append(
                "<dn>ne=1;chassis=1;card=1;module=" + moduleId + ";connection=" + connId + "</dn>" + "\n");
        stringBuilder.append("</delete-connection>" + "\n");
        stringBuilder.append("</rpc>" + " \n");

        log.info("Lumentum ROADM20 - RPC delete-connection sent to device {}", did());
        log.debug("Lumentum ROADM20 - - RPC delete-connection sent to device {} {}", did(), stringBuilder);

        return editCrossConnect(stringBuilder.toString());
    }

    //Following Lumentum documentation rpc operation to delete a new connection
    //Executed if for some reason a connection not in the cache is detected
    private boolean rpcDeleteExternalConnection(short moduleId, short connectionId) {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "\n");
        stringBuilder.append("<delete-connection xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">" + "\n");
        stringBuilder.append("<dn>ne=1;chassis=1;card=1;module="
                + moduleId + ";connection=" + connectionId + "</dn>" + "\n");
        stringBuilder.append("</delete-connection>" + "\n");
        stringBuilder.append("</rpc>" + "\n");

        log.info("Lumentum ROADM20 - RPC delete-external-connection sent to device {}", did());
        log.debug("Lumentum ROADM20 - - RPC delete-external-connection sent to device {} {}", did(), stringBuilder);

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
            log.error("Failed to edit the CrossConnect edit-cfg for device {}",
                      handler().data().deviceId(), e);
            log.debug("Failed configuration {}", xcString);
            return false;
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
     * Device only supports connection id < 130
     */
    private int generateConnectionId(LumentumFlowRule xc) {

       int moduleNew = xc.getConnectionModule();

        //LUMENTUM_ROADM20_MAX_CONNECTIONS =  100, device only supports connection id < 130
        for (int i = 1; i < LUMENTUM_ROADM20_MAX_CONNECTIONS; i++) {
            Set<FlowRule> rulesForDevice = getConnectionCache().get(did());

            if (rulesForDevice == null) {
                return 1;
            } else {
                Set<Integer> connIds = rulesForDevice.stream()
                        .filter(flow -> ((LumentumFlowRule) flow).getConnectionModule() == moduleNew)
                        .map(flow -> ((LumentumFlowRule) flow).getConnectionId())
                        .collect(Collectors.toSet());

                if (!connIds.contains(i)) {
                    return i;
                }
            }
        }
        return 0;
    }

    private DeviceConnectionCache getConnectionCache() {
        return DeviceConnectionCache.init();
    }

    /**
     * Helper method to get the device id.
     */
    private DeviceId did() {
        return data().deviceId();
    }

    /**
     * Helper method to get the Netconf session.
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(did()).getSession();
    }
}
