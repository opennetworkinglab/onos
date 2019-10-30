/*
 * Copyright 2019-present Open Networking Foundation
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
 *
 * This work was done in Nokia Bell Labs Paris
 *
 */
package org.onosproject.roadm.cli;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.Frequency;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.optical.util.OpticalChannelUtility;
import org.onosproject.roadm.RoadmService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This is the command for adding and dropping cross-connections on the ROADMs.
 *
 */
@Service
@Command(scope = "onos", name = "roadm-xc",
        description = "Creates/Removes cross-connection on/from the ROADM")
public class RoadmCrossConnectCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(RoadmCrossConnectCommand.class);

    @Argument(index = 0, name = "operation",
            description = "Specify Create or Remove action",
            required = true, multiValued = false)
    @Completion(RoadmCrossConnectCommandCompleter.class)
    private String operation = null;

    @Argument(index = 1, name = "deviceId",
            description = "ROADM's device ID (from ONOS)",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private String deviceId = null;

    @Argument(index = 2, name = "srcPort",
            description = "XC's source port {PortNumber}",
            required = true, multiValued = false)
    private String srcPort = null;

    @Argument(index = 3, name = "dstPort",
            description = "XC's destination port {PortNumber}",
            required = true, multiValued = false)
    private String dstPort = null;

    @Argument(index = 4, name = "freq",
            description = "XC's central frequency in [GHz], like 193100 or any other frequency",
            required = true, multiValued = false)
    private String freq = null;

    @Argument(index = 5, name = "sw",
            description = "Frequency Slot Width in [GHz], like 50 GHz or 62.5 GHz",
            required = true, multiValued = false)
    private String sw = null;

    @Argument(index = 6, name = "gridType",
            description = "Frequency grid type. Could be FLEX, CWDM, DWDM or UNKNOWN.",
            required = false, multiValued = false)
    private String gridType = null;

    @Argument(index = 7, name = "channelSpacing",
            description = "Channel spacing in [GHz]. " +
                    "Could be CHL_0GHZ, CHL_6P25GHZ, CHL_12P5GHZ, " +
                    "CHL_25GHZ, CHL_50GHZ, CHL_100GHZ",
            required = false, multiValued = false)
    private String channelSpacing = null;

    static final String REMOVE = "remove";
    static final String CREATE = "create";

    @Override
    protected void doExecute() throws Exception {

        DeviceService deviceService = AbstractShellCommand.get(DeviceService.class);

        if (deviceService.isAvailable(DeviceId.deviceId(deviceId))) {
            if (deviceService.getPort(DeviceId.deviceId(deviceId), PortNumber.portNumber(srcPort)).isEnabled()) {
                if (deviceService.getPort(DeviceId.deviceId(deviceId), PortNumber.portNumber(dstPort)).isEnabled()) {
                    if (operation.equals(CREATE)) {

                        FlowId check = addRule();
                        if (check != null) {
                            print("Rule %s was successfully added", check.toString());
                            log.info("Rule {} was successfully added", check.toString());
                        } else {
                            print("Your rule wasn't added. Something went wrong during the process " +
                                          "(issue on the driver side).");
                            log.error("Your rule wasn't added. " +
                                              "Something went wrong during the process (issue on the driver side).");
                        }

                    } else if (operation.equals(REMOVE)) {

                        FlowId check = dropRule();
                        if (check != null) {
                            print("Rule %s was successfully dropped", check.toString());
                            log.info("Rule {} was successfully dropped", check.toString());
                        } else {
                            print("Your rule wasn't dropped. No match found.");
                            log.error("Your rule wasn't dropped. No match found.");
                        }

                    } else {

                        print("\n Unspecified operation -- %s -- :( \n Try again! \n", operation);
                        log.debug("\n Unspecified operation -- {} -- :( \n Try again! \n", operation);

                    }
                } else {
                    log.error("Destination port {} is not enabled", dstPort);
                }
            } else {
                log.error("Source port {} is not enabled", srcPort);
            }
        } else {
            log.error("Device {} is not available", deviceId);
        }

    }


    /**
     * This method creates a XC on the device based on the parameters passed by the user.
     * Takes as an input "global" parameters (passed by user through the console).
     * @return - return the FlowId of the installed rule.
     */
    protected FlowId addRule() {

        // Preparing parameters
        DeviceId device = DeviceId.deviceId(deviceId);
        PortNumber inPort = PortNumber.portNumber(srcPort);
        PortNumber outPort = PortNumber.portNumber(dstPort);
        OchSignal signal = createOchSignal(freq, sw, gridType, channelSpacing);

        if (inPort == null) {
            print("[addRule] Not able to find srcPort in the ONOS database");
            log.debug("[addRule] Not able to find srcPort in the ONOS database");
            return null;
        }
        if (outPort == null) {
            print("[addRule] Not able to find dstPort in the ONOS database");
            log.debug("[addRule] Not able to find dstPort in the ONOS database");
            return null;
        }

        if (signal == null) {
            print("[addRule] Not able to compose an OchSignal with passed parameters. Double check them");
            log.debug("[addRule] Not able to compose an OchSignal with passed parameters. Double check them");
            return null;
        }

        RoadmService manager = AbstractShellCommand.get(RoadmService.class);
        print("Adding XC for the device %s between port %s and port %s on frequency %s with bandwidth %s",
              deviceId, srcPort, dstPort, freq, sw);
        log.info("[addRule] Adding XC for the device {} between port {} and port {} " +
                         "on frequency {} with bandwidth {}", deviceId, srcPort, dstPort, freq, sw);
        FlowId flow = manager.createConnection(device, 100, true, -1, inPort, outPort, signal);

        if (flow != null) {
            return flow;
        } else {
            return null;
        }
    }


    /**
     * This function drops XC installed on the device, which is matching parsed criteria.
     * Takes as an input "global" parameters (passed by user through the console).
     * @return - returns number of the rules that were dropped.
     */
    protected FlowId dropRule() {

        // Preparing parameters
        DeviceId device = DeviceId.deviceId(deviceId);
        PortNumber inPort = PortNumber.portNumber(srcPort);
        PortNumber outPort = PortNumber.portNumber(dstPort);

        // Creating some variables
        OchSignal ochSignal = null;
        PortNumber inputPortNumber = null;
        PortNumber outputPortNumber = null;

        // Main idea: Go over all flow rules (read out from the storage) of current device and
        // filter them based on input and output port with respect to OchSignal
        FlowRuleService fr = AbstractShellCommand.get(FlowRuleService.class);
        Iterable<FlowEntry> flowRules = fr.getFlowEntries(device);
        FlowId flowId = null;
        OchSignal referenceSignal = createOchSignal(freq, sw, gridType, channelSpacing);


        for (FlowEntry flowRule : flowRules) {

            // Taken from FlowRuleParser
            for (Criterion c : flowRule.selector().criteria()) {
                if (c instanceof OchSignalCriterion) {
                    ochSignal = ((OchSignalCriterion) c).lambda();
                }
                if (c instanceof PortCriterion) {
                    inputPortNumber = ((PortCriterion) c).port(); // obtain input port
                }
            }
            for (Instruction i : flowRule.treatment().immediate()) {
                if (i instanceof
                        L0ModificationInstruction.ModOchSignalInstruction) {
                    ochSignal =
                            ((L0ModificationInstruction.ModOchSignalInstruction) i)
                                    .lambda();
                }
                if (i instanceof Instructions.OutputInstruction) {
                    outputPortNumber = ((Instructions.OutputInstruction) i).port(); // obtain output port
                }
            }

            // If we found match, then let's delete this rule
            if ((ochSignal.centralFrequency().equals(referenceSignal.centralFrequency()))
                    & (ochSignal.slotWidth().equals(referenceSignal.slotWidth()))
                    & (inputPortNumber.equals(inPort)) & (outputPortNumber.equals(outPort))) {
                flowId = flowRule.id();

                RoadmService manager = AbstractShellCommand.get(RoadmService.class);
                manager.removeConnection(device, flowId);
                print("Dropping existing XC from the device %s", deviceId);
                return flowId;
            }
        }

        return null;
    }


    /**
     * This method forms parameters and creates and OchSignal instance from
     * central frequency and the slot width of the channel.
     * @param frequency - central frequency of the connection.
     * @param sw - slot width of the optical channel.
     * @param grid - frequency grid type.
     * @param spacing - channel spacing.
     * @return - returns created instance of OchSignal.
     */
    protected OchSignal createOchSignal(String frequency, String sw, String grid, String spacing) {

        Frequency centralFreq = Frequency.ofGHz(Double.parseDouble(frequency));
        Frequency slotWidth = Frequency.ofGHz(Double.parseDouble(sw));

        GridType gridType = null;
        try {
            gridType = GridType.valueOf(grid.toUpperCase());
        } catch (Exception e) {
            gridType = GridType.DWDM;
        }

        ChannelSpacing channelSpacing = null;
        // It requires passing channelSpacing in the following format CHL_6P25GHZ or similar
        try {
            channelSpacing = ChannelSpacing.valueOf(spacing);
        } catch (Exception e) {
            channelSpacing = ChannelSpacing.CHL_50GHZ;
        }

        return OpticalChannelUtility.createOchSignal(centralFreq, slotWidth, gridType, channelSpacing);
    }

}
