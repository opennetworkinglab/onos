/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.statistic.FlowEntryWithLoad;
import org.onosproject.net.statistic.FlowStatisticService;
import org.onosproject.net.statistic.SummaryFlowEntryWithLoad;

import java.util.List;
import java.util.Map;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Fetches flow statistics with a flow type and instruction type.
 */
@Command(scope = "onos", name = "get-flow-stats",
        description = "Fetches flow stats for a connection point with given flow type and instruction type")
public class GetFlowStatisticsCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "devicePort",
            description = "Device[/Port] connectPoint Description",
            required = true, multiValued = false)
    String devicePort = null;

    @Option(name = "-s", aliases = "--summary",
            description = "Show flow stats summary",
            required = false, multiValued = false)
    boolean showSummary = true; // default summary

    @Option(name = "-a", aliases = "--all",
            description = "Show flow stats all",
            required = false, multiValued = false)
    boolean showAll = false;

    @Option(name = "-t", aliases = "--topn",
            description = "Show flow stats topn entry",
            required = false, multiValued = false)
    String showTopn = null;

    @Option(name = "-f", aliases = "--flowType",
            description = "Flow live type, It includes IMMEDIATE, SHORT, MID, LONG, UNKNOWN"
                          + ", and is valid with -a or -t option only",
            required = false, multiValued = false)
    String flowLiveType = null;

    @Option(name = "-i", aliases = "--instructionType",
            description = "Flow instruction type, It includes DROP, OUTPUT, GROUP, L0MODIFICATION, L2MODIFICATION,"
                    + " TABLE, L3MODIFICATION, METADATA"
                    + ", and is valid with -a or -t option only",
            required = false, multiValued = false)
    String instructionType = null;

    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);
        FlowStatisticService flowStatsService = get(FlowStatisticService.class);

        String deviceUri = getDeviceId(devicePort);
        String portUri = getPortNumber(devicePort);

        DeviceId ingressDeviceId = deviceId(deviceUri);
        PortNumber ingressPortNumber;
        if (portUri.length() == 0) {
            ingressPortNumber = null;
        } else {
            ingressPortNumber = portNumber(portUri);
        }

        Device device = deviceService.getDevice(ingressDeviceId);
        if (device == null) {
            error("No such device %s", ingressDeviceId.uri());
            return;
        }

        if (ingressPortNumber != null) {
            Port port = deviceService.getPort(ingressDeviceId, ingressPortNumber);
            if (port == null) {
                error("No such port %s on device %s", portUri, ingressDeviceId.uri());
                return;
            }
        }

        if (flowLiveType != null) {
            flowLiveType = flowLiveType.toUpperCase();
        }
        if (instructionType != null) {
            instructionType = instructionType.toUpperCase();
        }

        // convert String to FlowLiveType and check validity
        FlowEntry.FlowLiveType inLiveType;
        if (flowLiveType == null) {
            inLiveType = null;
        } else {
            inLiveType = getFlowLiveType(flowLiveType);
            if (inLiveType == null) {
                error("Invalid flow live type [%s] error", flowLiveType);
                return;
            }
        }
        // convert String to InstructionType and check validity
        Instruction.Type inInstructionType;
        if (instructionType == null) {
            inInstructionType = null;
        } else {
            inInstructionType = getInstructionType(instructionType);
            if (inInstructionType == null) {
                error("Invalid instruction type [%s] error", instructionType);
                return;
            }
        }

        if (showTopn != null) {
            int topn = Integer.parseInt(showTopn);

            if (topn <= 0) {
                topn = 100; //default value
            } else if (topn > 1000) {
                topn = 1000; //max value
            }

            // print show topn head line with type
            print("deviceId=%s, show TOPN=%s flows, liveType=%s, instruction type=%s",
                    deviceUri,
                    Integer.toString(topn),
                    flowLiveType == null ? "ALL" : flowLiveType,
                    instructionType == null ? "ALL" : instructionType);
            if (ingressPortNumber == null) {
                Map<ConnectPoint, List<FlowEntryWithLoad>> typedFlowLoadMap =
                          flowStatsService.loadTopnByType(device, inLiveType, inInstructionType, topn);
                // print all ports topn flows load for a given device
                for (ConnectPoint cp : typedFlowLoadMap.keySet()) {
                    printPortFlowsLoad(cp, typedFlowLoadMap.get(cp));
                }
            } else {
                List<FlowEntryWithLoad> typedFlowLoad =
                        flowStatsService.loadTopnByType(device, ingressPortNumber, inLiveType, inInstructionType, topn);
                // print device/port topn flows load
                ConnectPoint cp = new ConnectPoint(ingressDeviceId, ingressPortNumber);
                printPortFlowsLoad(cp, typedFlowLoad);
            }
        } else if (showAll) { // is true?
            // print show all head line with type
            print("deviceId=%s, show ALL flows, liveType=%s, instruction type=%s",
                    deviceUri,
                    flowLiveType == null ? "ALL" : flowLiveType,
                    instructionType == null ? "ALL" : instructionType);
            if (ingressPortNumber == null) {
                Map<ConnectPoint, List<FlowEntryWithLoad>> typedFlowLoadMap =
                        flowStatsService.loadAllByType(device, inLiveType, inInstructionType);
                // print all ports all flows load for a given device
                for (ConnectPoint cp : typedFlowLoadMap.keySet()) {
                    printPortFlowsLoad(cp, typedFlowLoadMap.get(cp));
                }
            } else {
                List<FlowEntryWithLoad> typedFlowLoad =
                        flowStatsService.loadAllByType(device, ingressPortNumber, inLiveType, inInstructionType);
                // print device/port all flows load
                ConnectPoint cp = new ConnectPoint(ingressDeviceId, ingressPortNumber);
                printPortFlowsLoad(cp, typedFlowLoad);
            }
        } else { // if (showSummary == true) //always is true
            // print show summary head line
            print("deviceId=%s, show SUMMARY flows", deviceUri);
            if (ingressPortNumber == null) {
                Map<ConnectPoint, SummaryFlowEntryWithLoad> summaryFlowLoadMap =
                        flowStatsService.loadSummary(device);
                // print all ports flow load summary for a given device
                for (ConnectPoint cp : summaryFlowLoadMap.keySet()) {
                    printPortSummaryLoad(cp, summaryFlowLoadMap.get(cp));
                }
            } else {
                SummaryFlowEntryWithLoad summaryFlowLoad =
                        flowStatsService.loadSummary(device, ingressPortNumber);
                // print device/port flow load summary
                ConnectPoint cp = new ConnectPoint(ingressDeviceId, ingressPortNumber);
                printPortSummaryLoad(cp, summaryFlowLoad);
            }
        }
    }

    /**
     * Extracts the port number portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return port number as a string, empty string if the port is not found
     */
    private String getPortNumber(String deviceString) {
        if (deviceString == null) {
            return "";
        }

        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return ""; // return when no port number
        }
        return deviceString.substring(slash + 1, deviceString.length());
    }

    /**
     * Extracts the device ID portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return device ID string
     */
    private String getDeviceId(String deviceString) {
        if (deviceString == null) {
            return "";
        }

        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return deviceString; // return only included device ID
        }
        return deviceString.substring(0, slash);
    }

    /**
     * converts string of flow live type to FlowLiveType enum.
     *
     * @param liveType string representing the flow live type
     * @return FlowEntry.FlowLiveType
     */
    private FlowEntry.FlowLiveType getFlowLiveType(String liveType) {
        String liveTypeUC = liveType.toUpperCase();

        if ("IMMEDIATE".equals(liveTypeUC)) {
            return FlowEntry.FlowLiveType.IMMEDIATE;
        } else if ("SHORT".equals(liveTypeUC)) {
            return FlowEntry.FlowLiveType.SHORT;
        } else if ("MID".equals(liveTypeUC)) {
            return FlowEntry.FlowLiveType.MID;
        } else if ("LONG".equals(liveTypeUC)) {
            return FlowEntry.FlowLiveType.LONG;
        } else if ("UNKNOWN".equals(liveTypeUC)) {
            return FlowEntry.FlowLiveType.UNKNOWN;
        } else {
            return null; // flow live type error
        }
    }

    /**
     * converts string of instruction type to Instruction type enum.
     *
     * @param instType string representing the instruction type
     * @return Instruction.Type
     */
    private Instruction.Type getInstructionType(String instType) {
        String instTypeUC = instType.toUpperCase();

        if ("OUTPUT".equals(instTypeUC)) {
            return Instruction.Type.OUTPUT;
        } else if ("GROUP".equals(instTypeUC)) {
            return Instruction.Type.GROUP;
        } else if ("L0MODIFICATION".equals(instTypeUC)) {
            return Instruction.Type.L0MODIFICATION;
        } else if ("L2MODIFICATION".equals(instTypeUC)) {
            return Instruction.Type.L2MODIFICATION;
        } else if ("TABLE".equals(instTypeUC)) {
            return Instruction.Type.TABLE;
        } else if ("L3MODIFICATION".equals(instTypeUC)) {
            return Instruction.Type.L3MODIFICATION;
        } else if ("METADATA".equals(instTypeUC)) {
            return Instruction.Type.METADATA;
        } else {
             return null; // instruction type error
        }
    }

    private void printPortFlowsLoad(ConnectPoint cp, List<FlowEntryWithLoad> typedFlowLoad) {
       print("  deviceId/Port=%s/%s, %s flows", cp.elementId(), cp.port(), typedFlowLoad.size());
        for (FlowEntryWithLoad fel: typedFlowLoad) {
            StoredFlowEntry sfe =  fel.storedFlowEntry();
            print("    flowId=%s, state=%s, liveType=%s, life=%s -> %s",
                  Long.toHexString(sfe.id().value()),
                  sfe.state(),
                  sfe.liveType(),
                  sfe.life(),
                  fel.load().isValid() ? fel.load() : "Load{rate=0, NOT VALID}");
        }
    }

    private void printPortSummaryLoad(ConnectPoint cp, SummaryFlowEntryWithLoad summaryFlowLoad) {
        print("  deviceId/Port=%s/%s, Total=%s, Immediate=%s, Short=%s, Mid=%s, Long=%s, Unknown=%s",
                cp.elementId(),
                cp.port(),
                summaryFlowLoad.totalLoad().isValid() ? summaryFlowLoad.totalLoad() : "Load{rate=0, NOT VALID}",
                summaryFlowLoad.immediateLoad().isValid() ? summaryFlowLoad.immediateLoad() : "Load{rate=0, NOT VALID}",
                summaryFlowLoad.shortLoad().isValid() ? summaryFlowLoad.shortLoad() : "Load{rate=0, NOT VALID}",
                summaryFlowLoad.midLoad().isValid() ? summaryFlowLoad.midLoad() : "Load{rate=0, NOT VALID}",
                summaryFlowLoad.longLoad().isValid() ? summaryFlowLoad.longLoad() : "Load{rate=0, NOT VALID}",
                summaryFlowLoad.unknownLoad().isValid() ? summaryFlowLoad.unknownLoad() : "Load{rate=0, NOT VALID}");
    }
}
