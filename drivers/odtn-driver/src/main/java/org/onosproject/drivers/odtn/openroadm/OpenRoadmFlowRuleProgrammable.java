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
package org.onosproject.drivers.odtn.openroadm;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of FlowRuleProgrammable interface for OpenROADM devices.
 */
public class OpenRoadmFlowRuleProgrammable
  extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private static final Logger log =
      LoggerFactory.getLogger(OpenRoadmFlowRuleProgrammable.class);

    private static final String RPC_TAG_NETCONF_BASE =
      "<rpc xmlns='urn:ietf:params:xml:ns:netconf:base:1.0'>";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final String ORG_OPENROADM_DEVICE_OPEN_TAG =
      "<org-openroadm-device xmlns='http://org/openroadm/device'  xmlns:nc='urn:ietf:params:xml:ns:netconf:base:1.0'>";
    private static final String ORG_OPENROADM_DEVICE_CLOSE_TAG = "</org-openroadm-device>";

    /**
     * Helper method to get the DeviceId.
     * <p>
     */
    private DeviceId did() {
        return data().deviceId();
    }

    private DeviceConnectionCache getConnectionCache() {
        return DeviceConnectionCache.init();
    }

    /**
     * Helper method to log from this class adding DeviceId.
     * <p>
     */
    private void openRoadmLog(String format, Object... arguments) {
        log.debug("OPENROADM {}: " + format, did(), arguments);
    }

    /**
     * Helper method to log from this class adding DeviceId.
     * <p>
     */
    private void openRoadmInfo(String format, Object... arguments) {
        log.info("OPENROADM {}: " + format, did(), arguments);
    }


    /**
     * Get a list of Port numbers that are LINE ports (degree).
     * <p>
     *  @return list of port numbers
     */
    private List<PortNumber> getLinePorts() {
        DeviceService deviceService = this.handler().get(DeviceService.class);
        return deviceService.getPorts(did())
          .stream()
          .filter(
            p -> p.annotations().value("openroadm-logical-connection-point").contains("DEG"))
          .map(p -> p.number())
          .collect(Collectors.toList());
    }

    /**
     * Helper method to get the Netconf Session.
     *  @return the netconf session, which may be null.
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller = handler().get(NetconfController.class);
        NetconfSession session = controller.getNetconfDevice(did()).getSession();
        return session;
    }

    /**
     * Fetches list of connections from device.
     *
     * @return list of connections as XML hierarchy
     */
    private List<HierarchicalConfiguration> getDeviceConnections() {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("OPENROADM {}: session not found", did());
            return ImmutableList.of();
        }
        try {
            StringBuilder rb = new StringBuilder();
            rb.append(ORG_OPENROADM_DEVICE_OPEN_TAG);
            rb.append("  <roadm-connections/>");
            rb.append(ORG_OPENROADM_DEVICE_CLOSE_TAG);
            String reply = session.getConfig(DatastoreId.RUNNING, rb.toString());
            log.debug("REPLY to getDeviceConnections {}", reply);
            HierarchicalConfiguration cfg =
              XmlConfigParser.loadXml(new ByteArrayInputStream(reply.getBytes()));
            return cfg.configurationsAt("data.org-openroadm-device.roadm-connections");
        } catch (NetconfException e) {
            return ImmutableList.of();
        }
    }


    /**
     * Get the flow entries that are present on the device, called by
     * FlowRuleDriverProvider. <p> The flow entries must match exactly the
     * FlowRule entries in the ONOS store. If they are not an exact match the
     * device will be requested to remove those flows.
     *
     * @return A collection of Flow Entries
     */
    @Override
    public Collection<FlowEntry> getFlowEntries() {
        List<HierarchicalConfiguration> conf = getDeviceConnections();
        List<FlowEntry> entries = new ArrayList<>();
        for (HierarchicalConfiguration c : conf) {
            openRoadmLog("Existing connection {}", c);
            FlowRule r = buildFlowrule(c);
            if (r != null) {
                FlowEntry e = new DefaultFlowEntry(r, FlowEntry.FlowEntryState.ADDED, 0, 0, 0);
                openRoadmLog("RULE RETRIEVED {}", r);
                entries.add(e);
            }
        }
        return entries;
    }


    /**
     * Apply the flow entries specified in the collection rules.
     *
     * @param rules A collection of Flow Rules to be applied
     * @return The collection of added Flow Entries
     */
    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        List<FlowRule> added = new ArrayList<>();
        for (FlowRule r : rules) {
            openRoadmLog("TO APPLY RULE {}", r);
            OpenRoadmFlowRule xc = new OpenRoadmFlowRule(r, getLinePorts());
            openRoadmInfo("OpenRoadmRule {}", xc);
            if (editConfigCreateConnection(xc)) {
                added.add(xc);
                openRoadmLog("RULE APPLIED {}", r);
            }
        }
        openRoadmLog("applyFlowRules added {}", added.size());
        return added;
    }


    /**
     * Remove the specified flow rules.
     *
     * @param rules A collection of Flow Rules to be removed
     * @return The collection of removed Flow Rules
     */
    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        List<FlowRule> removed = new ArrayList<>();
        for (FlowRule r : rules) {
            OpenRoadmFlowRule xc = new OpenRoadmFlowRule(r, getLinePorts());
            openRoadmLog("TO REMOVE RULE {}", xc);
            if (editConfigDeleteConnection(xc)) {
                removed.add(r);
                openRoadmLog("RULE REMOVED {}", r);
            }
        }
        openRoadmLog("removedFlowRules removed {}", removed.size());
        return removed;
    }

    /**
     * Construct a connection name for a connection between two ports.
     *
     * @param srcPort source port, can be a degree port or a SRG port
     * @param dstPort destination port, can be a degree port or a SRG port
     * @param ncFreq Nominal Center freq.
     * @return a string with the connection name.
     *
     * OpenROADM Connections are of the form <nmc-interface>-to-<nmc-interface>.
     * NMC interfaces are created before the connection, directly over the SRG
     * port or a supporting MC interface.
     *
     */
    private String openRoadmConnectionName(Port srcPort, Port dstPort, Frequency ncFreq) {
        StringBuilder sb = new StringBuilder();
        sb.append("NMC-CTP-");
        sb.append(srcPort.annotations().value("openroadm-logical-connection-point"));
        sb.append("-");
        sb.append(ncFreq.asTHz());
        sb.append("-to-NMC-CTP-");
        sb.append(dstPort.annotations().value("openroadm-logical-connection-point"));
        sb.append("-");
        sb.append(ncFreq.asTHz());
        return sb.toString();
    }

    /**
     * Construct a connection name given an OpenRoadmFlowRule.
     *
     * @param xc the flow rule or crossconnection.
     *
     */
    private String openRoadmConnectionName(OpenRoadmFlowRule xc) {
        DeviceService deviceService = this.handler().get(DeviceService.class);
        Port srcPort = deviceService.getPort(did(), xc.inPort());
        Port dstPort = deviceService.getPort(did(), xc.outPort());
        Frequency centerFreq = xc.ochSignal().centralFrequency();
        return openRoadmConnectionName(srcPort, dstPort, centerFreq);
    }

    /**
     * Builds a flow rule from a connection object (as XML object).
     *
     * @param connection the connection hierarchy
     * @return the flow rule
     */
    private FlowRule buildFlowrule(HierarchicalConfiguration connection) {
        String name = connection.getString("connection-name");
        if (name == null) {
            log.error("OPENROADM {}: connection name not correctly retrieved", did());
            return null;
        }
        // If the flow entry is not in the cache: return null
        FlowRule flowRule = getConnectionCache().get(did(), name);
        if (flowRule == null) {
            log.error("OPENROADM {}: name {} not in cache. delete editConfig", did(), name);
            editConfigDeleteConnection(name);
            return null;
        } else {
            openRoadmLog("connection retrieved {}", name);
        }
        OpenRoadmFlowRule xc = new OpenRoadmFlowRule(flowRule, getLinePorts());
        DeviceService deviceService = this.handler().get(DeviceService.class);
        OpenRoadmConnection conn = OpenRoadmConnectionFactory.create(name, xc, deviceService);
        OchSignal och = toOchSignalCenterWidth(conn.srcNmcFrequency, conn.srcNmcWidth);
        // Build the rule selector and treatment
        TrafficSelector selector =
          DefaultTrafficSelector.builder()
            .matchInPort(conn.inPortNumber)
            .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
            .add(Criteria.matchLambda(och))
            .build();
        Instruction ochInstruction = Instructions.modL0Lambda(och);
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                       .add(ochInstruction)
                                       .setOutput(conn.outPortNumber)
                                       .build();

        return DefaultFlowRule.builder()
          .forDevice(data().deviceId())
          .makePermanent()
          .withSelector(selector)
          .withTreatment(treatment)
          .withPriority(conn.priority)
          .withCookie(conn.id.value())
          .build();
    }



    /**
     * Delete a ROADM Interface given its name.
     *
     * @param interfaceName name of the interface to be removed.
     */
    private void editConfigDeleteInterfaceEntry(String interfaceName) {
        checkNotNull(interfaceName);
        StringBuilder sb = new StringBuilder();
        sb.append(ORG_OPENROADM_DEVICE_OPEN_TAG);
        sb.append("  <interface nc:operation='delete'>");
        sb.append("    <name>" + interfaceName + "</name>");
        sb.append("  </interface>");
        sb.append(ORG_OPENROADM_DEVICE_CLOSE_TAG);
        if (!editConfig(sb.toString())) {
            log.error("OPENROADM {}: failed to delete interface{}", did(), interfaceName);
        }
    }



    /**
     * Delete a ROADM Connection given its name.
     *
     * @param connectionName name of the connection to be removed.
     */
    private void editConfigDeleteConnectionEntry(String connectionName) {
        StringBuilder sb = new StringBuilder();
        sb.append(ORG_OPENROADM_DEVICE_OPEN_TAG);
        sb.append("   <roadm-connections nc:operation='delete'>");
        sb.append("     <connection-name>" + connectionName + "</connection-name>");
        sb.append("    </roadm-connections>");
        sb.append(ORG_OPENROADM_DEVICE_CLOSE_TAG);
        if (!editConfig(sb.toString())) {
            log.error("OPENROADM {}: failed to delete Connection {}", did(), connectionName);
        }
    }

    /**
     * Entry point to remove a Crossconnect.
     *
     * @param connectionName name of the connection to be removed.
     * @return true (an edit delete is assumed to not fail)
     *
     * NMC-CTP-<rx-logical-port>-<freq>-to-NMC-CTP-<tx-logical-port>-<freq>
     * NMC-CTP-DEG1-TTP-RX-192.7-to-NMC-CTP-DEG3-TTP-TX-192.7
     *
     * Local:
     * NMC-CTP-SRG1-PP1-RX-190.7-to-NMC-CTP-SRG1-PP2-TX-190.7
     *
     * Note this method is rarely used, only when it exists in the Datastore
     * but not in the connection cache.
     */
    private boolean editConfigDeleteConnection(String connectionName) {
        String[] nmcNames = connectionName.split("-to-");
        // Connection src NMC interface (RX) is NMC-CTP-DEG1-TTP-RX-192.7
        // Connection dst NMC interface (TX) is NMC-CTP-DEG3-TTP-TX-192.7
        String nmcRxName = nmcNames[0];
        String nmcTxName = nmcNames[1];
        // Connection src MC interface (RX) is MC-TTP-DEG3-TTP-RX-192.7
        // Connection dst MC interface (TX) is MC-TTP-DEG3-TTP-TX-192.7
        String mcRxName = "MC-TTP" + nmcRxName.substring(7);
        String mcTxName = "MC-TTP" + nmcTxName.substring(7);

        // Delete Connection
        editConfigDeleteConnectionEntry(connectionName);
        // Delete interfaces
        editConfigDeleteInterfaceEntry(nmcRxName);
        editConfigDeleteInterfaceEntry(nmcTxName);
        if (!nmcRxName.contains("SRG")) { // Source MC interfaces not in ADD
            editConfigDeleteInterfaceEntry(mcRxName);
        }
        if (!nmcTxName.contains("SRG")) { // Dest MC interfaces not in ADD
            editConfigDeleteInterfaceEntry(mcTxName);
        }
        return true;
    }



    /**
     * Entry point to remove a Crossconnect.
     *
     * @param xc - OpenROADM flow rule (cross-connect data)
     */
    private boolean editConfigDeleteConnection(OpenRoadmFlowRule xc) {
        String name = openRoadmConnectionName(xc);
        FlowRule flowRule = getConnectionCache().get(did(), name);
        if (flowRule == null) {
            openRoadmLog("editConfigDeleteConnection,  {} not in cache", name);
            // What to do ? it should be in the cache
            return true;
        }
        // Delete Connection
        editConfigDeleteConnectionEntry(name);
        // Remove connection from cache
        getConnectionCache().remove(did(), xc);

        DeviceService deviceService = this.handler().get(DeviceService.class);
        OpenRoadmConnection conn = OpenRoadmConnectionFactory.create(name, xc, deviceService);

        // Delete interfaces. Note, deletion of interfaces may fail if
        // they are used by other connections.
        editConfigDeleteInterfaceEntry(conn.dstNmcName);
        editConfigDeleteInterfaceEntry(conn.srcNmcName);
        if ((conn.getType() != OpenRoadmFlowRule.Type.ADD_LINK) &&
            (conn.getType() != OpenRoadmFlowRule.Type.LOCAL)) {
            editConfigDeleteInterfaceEntry(conn.srcMcName);
        }
        if ((conn.getType() != OpenRoadmFlowRule.Type.DROP_LINK) &&
            (conn.getType() != OpenRoadmFlowRule.Type.LOCAL)) {
            editConfigDeleteInterfaceEntry(conn.dstMcName);
        }


        return true;
    }

    /**
     * Create a ROADM NMC Interfaces.
     *
     * @param conn connection to create on the device.
     * @param operation netconf operation (e.g. merge)
     * @return true if Netconf operation was ok, false otherwise.
     */
    private boolean editConfigCreateMcInterfaces(OpenRoadmConnection conn, String operation) {

        openRoadmLog("Checking MC interafaces for {}", conn);
        // clang-format off
        // Creation of MC in Input
        if ((conn.getType() != OpenRoadmFlowRule.Type.ADD_LINK) &&
            (conn.getType() != OpenRoadmFlowRule.Type.LOCAL)) {
            StringBuilder sb = new StringBuilder();
            openRoadmLog("Creating MC SRC interface {}", conn.srcMcName);
            sb.append(ORG_OPENROADM_DEVICE_OPEN_TAG);
            sb.append("<interface nc:operation='" + operation + "'>");
            sb.append("  <name>" + conn.srcMcName + "</name>");
            sb.append("  <description>Media-Channel</description>");
            sb.append("  <type xmlns:openROADM-if='http://org/openroadm/interfaces'>" +
                    "openROADM-if:mediaChannelTrailTerminationPoint</type>");
            sb.append("  <administrative-state>inService</administrative-state>");
            sb.append("  <supporting-circuit-pack-name>" +
                    conn.srcMcSupportingCircuitPack +
                    "</supporting-circuit-pack-name>");
            sb.append("  <supporting-port>" + conn.srcMcSupportingPort + "</supporting-port>");
            sb.append("  <supporting-interface>" + conn.srcMcSupportingInterface + "</supporting-interface>");
            sb.append("  <mc-ttp xmlns='http://org/openroadm/media-channel-interfaces'>");
            sb.append("    <min-freq>" + conn.srcMcMinFrequency.asTHz() + "</min-freq>");
            sb.append("    <max-freq>" + conn.srcMcMaxFrequency.asTHz() + "</max-freq>");
            sb.append("  </mc-ttp>");
            sb.append("</interface>");
            sb.append(ORG_OPENROADM_DEVICE_CLOSE_TAG);
            if (!editConfig(sb.toString())) {
                log.error("OPENROADM {}: failed to create interface\n {}", did(), sb.toString());
                return false;
            }
        }
        if ((conn.getType() != OpenRoadmFlowRule.Type.DROP_LINK) &&
            (conn.getType() != OpenRoadmFlowRule.Type.LOCAL)) {
            StringBuilder sb = new StringBuilder();
            openRoadmLog("Creating MC DST interface {}", conn.dstMcName);
            sb.append(ORG_OPENROADM_DEVICE_OPEN_TAG);
            sb.append("<interface nc:operation='" + operation + "'>");
            sb.append("  <name>" + conn.dstMcName + "</name>");
            sb.append("  <description>Media-Channel</description>");
            sb.append("  <type xmlns:openROADM-if='http://org/openroadm/interfaces'>" +
                    "openROADM-if:mediaChannelTrailTerminationPoint</type>");
            sb.append("  <administrative-state>inService</administrative-state>");
            sb.append("  <supporting-circuit-pack-name>" +
                conn.dstMcSupportingCircuitPack +
                "</supporting-circuit-pack-name>");
            sb.append("  <supporting-port>" + conn.dstMcSupportingPort + "</supporting-port>");
            sb.append("  <supporting-interface>" + conn.dstMcSupportingInterface + "</supporting-interface>");
            sb.append("  <mc-ttp xmlns='http://org/openroadm/media-channel-interfaces'>");
            sb.append("    <min-freq>" + conn.dstMcMinFrequency.asTHz() + "</min-freq>");
            sb.append("    <max-freq>" + conn.dstMcMaxFrequency.asTHz() + "</max-freq>");
            sb.append("  </mc-ttp>");
            sb.append("</interface>");
            sb.append(ORG_OPENROADM_DEVICE_CLOSE_TAG);
            if (!editConfig(sb.toString())) {
                log.error("OPENROADM {}: failed to create interface\n {}", did(), sb.toString());
                return false;
            }
        }
        // clang-format on
        return true;
    }

    /**
     * Create a ROADM NMC Interfaces.
     *
     * @param conn connection to create on the device.
     * @param operation netconf operation (e.g. merge)
     * @return true if Netconf operation was ok, false otherwise.
     */
    private boolean editConfigCreateNmcInterfaces(OpenRoadmConnection conn, String operation) {
        // clang-format off
        openRoadmLog("Creating NMC interfaces SRC {}", conn.srcNmcName);
        StringBuilder sb = new StringBuilder();
        sb.append(ORG_OPENROADM_DEVICE_OPEN_TAG);
        sb.append("<interface nc:operation='" + operation + "'>");
        sb.append("  <name>" + conn.srcNmcName + "</name>");
        sb.append("  <description>Network-Media-Channel</description>");
        sb.append("  <type xmlns:openROADM-if='http://org/openroadm/interfaces'>" +
                  "openROADM-if:networkMediaChannelConnectionTerminationPoint</type>");
        sb.append("  <administrative-state>inService</administrative-state>");
        sb.append("  <supporting-circuit-pack-name>" +
                conn.srcNmcSupportingCircuitPack +
                "</supporting-circuit-pack-name>");
        sb.append("  <supporting-port>" + conn.srcNmcSupportingPort + "</supporting-port>");
        if ((conn.getType() != OpenRoadmFlowRule.Type.ADD_LINK) &&
            (conn.getType() != OpenRoadmFlowRule.Type.LOCAL)) {
            sb.append("<supporting-interface>" + conn.srcNmcSupportingInterface + "</supporting-interface>");
        }
        sb.append("  <nmc-ctp xmlns='http://org/openroadm/network-media-channel-interfaces'>");
        sb.append("    <frequency>" + conn.srcNmcFrequency.asTHz() + "</frequency>");
        sb.append("    <width>" + conn.srcNmcWidth.asGHz() + "</width>");
        sb.append("  </nmc-ctp>");
        sb.append("</interface>");
        sb.append(ORG_OPENROADM_DEVICE_CLOSE_TAG);
        if (!editConfig(sb.toString())) {
            log.error("OpenRoadm driver - failed to create interface");
            return false;
        }

        openRoadmLog("Creating NMC interfaces DST {}", conn.dstNmcName);
        sb = new StringBuilder();
        sb.append(ORG_OPENROADM_DEVICE_OPEN_TAG);
        sb.append("<interface nc:operation='" + operation + "'>");
        sb.append("  <name>" + conn.dstNmcName + "</name>");
        sb.append("  <description>Network-Media-Channel</description>");
        sb.append("  <type xmlns:openROADM-if='http://org/openroadm/interfaces'>" +
                "openROADM-if:networkMediaChannelConnectionTerminationPoint</type>");
        sb.append("  <administrative-state>inService</administrative-state>");
        sb.append("  <supporting-circuit-pack-name>" +
                    conn.dstNmcSupportingCircuitPack +
                    "</supporting-circuit-pack-name>");
        sb.append("  <supporting-port>" + conn.dstNmcSupportingPort + "</supporting-port>");
        if ((conn.getType() != OpenRoadmFlowRule.Type.DROP_LINK) &&
            (conn.getType() != OpenRoadmFlowRule.Type.LOCAL)) {
            sb.append("<supporting-interface>" + conn.dstNmcSupportingInterface + "</supporting-interface>");
        }
        sb.append("  <nmc-ctp xmlns='http://org/openroadm/network-media-channel-interfaces'>");
        sb.append("    <frequency>" + conn.dstNmcFrequency.asTHz() + "</frequency>");
        sb.append("    <width>" + conn.dstNmcWidth.asGHz() + "</width>");
        sb.append("  </nmc-ctp>");
        sb.append("</interface>");
        sb.append(ORG_OPENROADM_DEVICE_CLOSE_TAG);
        if (!editConfig(sb.toString())) {
            log.error("OpenRoadm driver - failed to create interface");
            return false;
        }
        return true;
        // clang-format on
    }

    /**
     * Create the MC and NMC interfaces supporting a connection.
     *
     * @param conn connection to create on the device.
     * @return true if Netconf operation was ok, false otherwise.
     */
    private boolean editConfigCreateInterfaces(OpenRoadmConnection conn) {
        if (!editConfigCreateMcInterfaces(conn, "merge")) {
            return false;
        }
        if (!editConfigCreateNmcInterfaces(conn, "merge")) {
            return false;
        }
        return true;
    }


    /**
     * Create a ROADM Connection given its data.
     *
     * @param conn connection to create on the device.
     * @return true if Netconf operation was ok, false otherwise.
     */
    private boolean editConfigCreateConnectionEntry(OpenRoadmConnection conn,
                                                    String operation) {
        StringBuilder sb = new StringBuilder();
        sb.append(ORG_OPENROADM_DEVICE_OPEN_TAG);
        sb.append("  <roadm-connections nc:operation='" + operation + "'>");
        sb.append("    <connection-name>" + conn.connectionName + "</connection-name>");
        sb.append("    <opticalControlMode>off</opticalControlMode>");
        sb.append("    <target-output-power>0</target-output-power>");
        sb.append("    <source>");
        sb.append("      <src-if>" + conn.srcConnInterface + "</src-if>");
        sb.append("    </source>");
        sb.append("    <destination>");
        sb.append("      <dst-if>" + conn.dstConnInterface + "</dst-if>");
        sb.append("    </destination>");
        sb.append("  </roadm-connections>");
        sb.append(ORG_OPENROADM_DEVICE_CLOSE_TAG);
        if (!editConfig(sb.toString())) {
            log.error("OPENROADM {}: failed to create Connection {}", did(),
                      conn.connectionName);
            return false;
        }
        return true;
    }

    /**
     * Request the device to setup the Connection for the rule.

     * @param xc - OpenRoadmFlowRule crossconnect
     *
     * @return true if operation was completed, false otherwise.
     */
    private boolean editConfigCreateConnection(OpenRoadmFlowRule xc) {
        checkNotNull(xc);
        String openRoadmConnectionName = openRoadmConnectionName(xc);
        DeviceService deviceService = this.handler().get(DeviceService.class);
        OpenRoadmConnection connection =
          OpenRoadmConnectionFactory.create(openRoadmConnectionName, xc, deviceService);

        if (!editConfigCreateInterfaces(connection)) {
            return false;
        }

        if (!editConfigCreateConnectionEntry(connection, "merge")) {
            return false;
        }

        // Add connection to local cache
        getConnectionCache().add(did(), openRoadmConnectionName, xc);
        openRoadmLog("Connection {} created", connection.connectionName);
        return true;
    }

    /**
     * Helper function to send an edit-config message.
     * @param config XML string to send
     * @return false on error, true otherwise
     * <p>
     * This method uses the running datastore.
     */
    private boolean editConfig(String config) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("OPENROADM {}: session not found", did());
            return false;
        }
        try {
            return session.editConfig(DatastoreId.RUNNING, null, config);
        } catch (NetconfException e) {
            log.error("OPENROADM {}: failed to editConfig device {}", did(), e);
            return false;
        }
    }

    /**
     * Convert start and end frequencies to OCh signal.
     *
     * FIXME: assumes slots of 12.5 GHz while devices allows granularity 6.25
     * GHz and only supports channel spacing 50 and 100
     *
     * @param min starting frequency as double in THz
     * @param max end frequency as double in THz
     * @return OCh signal
     */
    public static OchSignal toOchSignalMinMax(Frequency min, Frequency max) {
        double start = min.asGHz();
        double end = max.asGHz();

        int slots = (int) ((end - start) / ChannelSpacing.CHL_12P5GHZ.frequency().asGHz());
        int multiplier = 0;

        // Conversion for 50 GHz slots
        if (end - start == 50) {
            multiplier =
              (int) (((end - start) / 2 + start - Spectrum.CENTER_FREQUENCY.asGHz()) /
                     ChannelSpacing.CHL_50GHZ.frequency().asGHz());

            return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, multiplier, slots);
        }

        // Conversion for 100 GHz slots
        if (end - start == 100) {
            multiplier =
              (int) (((end - start) / 2 + start - Spectrum.CENTER_FREQUENCY.asGHz()) /
                     ChannelSpacing.CHL_100GHZ.frequency().asGHz());

            return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, multiplier, slots);
        }

        return null;
    }

    /**
     * Helper method to create an OchSignal for a frequency slot.
     *
     *  @param center the center frequency as per the ITU-grid.
     *  @param width slot width
     * @return OCh signal
     */
    public static OchSignal toOchSignalCenterWidth(Frequency center, Frequency width) {

        Frequency radius = width.floorDivision(2);

        // Frequency slot start and end frequency.
        double start = center.subtract(radius).asGHz();
        double end = center.add(radius).asGHz();

        int slots = (int) ((end - start) / ChannelSpacing.CHL_12P5GHZ.frequency().asGHz());
        int multiplier = 0;

        // Conversion for 50 GHz slots
        if (end - start == 50) {
            multiplier =
              (int) (((end - start) / 2 + start - Spectrum.CENTER_FREQUENCY.asGHz()) /
                     ChannelSpacing.CHL_50GHZ.frequency().asGHz());

            return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, multiplier, slots);
        }

        // Conversion for 100 GHz slots
        if (end - start == 100) {
            multiplier =
              (int) (((end - start) / 2 + start - Spectrum.CENTER_FREQUENCY.asGHz()) /
                     ChannelSpacing.CHL_100GHZ.frequency().asGHz());

            return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, multiplier, slots);
        }

        return null;
    }
}
