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
 */


package org.onosproject.net.optical.cli;

import com.google.common.collect.ImmutableMap;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.NetconfOperationCompleter;
import org.onosproject.cli.net.OpticalConnectPointCompleter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.device.OchPortHelper;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Enable the optical channel and tune the wavelength via a flow rule based on a given Signal.
 */
@Service
@Command(scope = "onos", name = "wavelength-config",
        description = "Enable the optical channel and tune the wavelength via a flow rule ")
public class PortWaveLengthCommand extends AbstractShellCommand {

    private static final String SIGNAL_FORMAT = "slotGranularity/channelSpacing(in GHz e.g 6.25,12.5,25,50,100)/" +
            "spaceMultiplier/gridType(cwdm, flex, dwdm) " + "e.g 1/6.25/1/flex";

    private static final String CH_6P25 = "6.25";
    private static final String CH_12P5 = "12.5";
    private static final String CH_25 = "25";
    private static final String CH_50 = "50";
    private static final String CH_100 = "100";
    private static final long BASE_FREQUENCY = 193100000;   //Working in Mhz

    private static final Map<String, ChannelSpacing> CHANNEL_SPACING_MAP = ImmutableMap
            .<String, ChannelSpacing>builder()
            .put(CH_6P25, ChannelSpacing.CHL_6P25GHZ)
            .put(CH_12P5, ChannelSpacing.CHL_12P5GHZ)
            .put(CH_25, ChannelSpacing.CHL_25GHZ)
            .put(CH_50, ChannelSpacing.CHL_50GHZ)
            .put(CH_100, ChannelSpacing.CHL_100GHZ)
            .build();
    @Argument(index = 0, name = "operation", description = "Netconf Operation including get, edit-config, etc.",
            required = true, multiValued = false)
    @Completion(NetconfOperationCompleter.class)
    private String operation = null;

    @Argument(index = 1, name = "connectPoint",
            description = "Device/Port Description",
            required = true, multiValued = false)
    @Completion(OpticalConnectPointCompleter.class)
    String connectPointString = "";

    @Argument(index = 2, name = "value",
            description = "Optical Signal or wavelength. Provide wavelenght in MHz, while Och Format = "
                    + SIGNAL_FORMAT, required = false, multiValued = false)
    String parameter = "";


    private OchSignal createOchSignal() throws IllegalArgumentException {
        if (parameter == null) {
            return null;
        }
        try {
            String[] splitted = parameter.split("/");
            checkArgument(splitted.length == 4,
                    "signal requires 4 parameters: " + SIGNAL_FORMAT);
            int slotGranularity = Integer.parseInt(splitted[0]);
            String chSpacing = splitted[1];
            ChannelSpacing channelSpacing = checkNotNull(CHANNEL_SPACING_MAP.get(chSpacing),
                    String.format("invalid channel spacing: %s", chSpacing));
            int multiplier = Integer.parseInt(splitted[2]);
            String gdType = splitted[3].toUpperCase();
            GridType gridType = GridType.valueOf(gdType);
            return new OchSignal(gridType, channelSpacing, multiplier, slotGranularity);
        } catch (RuntimeException e) {
            /* catching RuntimeException as both NullPointerException (thrown by
             * checkNotNull) and IllegalArgumentException (thrown by checkArgument)
             * are subclasses of RuntimeException.
             */
            String msg = String.format("Invalid signal format: %s, expected format is %s.",
                    parameter, SIGNAL_FORMAT);
            print(msg);
            throw new IllegalArgumentException(msg, e);
        }
    }

    private OchSignal createOchSignalFromWavelength(DeviceService deviceService, ConnectPoint cp) {
        long wavelength = Long.parseLong(parameter);
        if (wavelength == 0L) {
            return null;
        }
        Port port = deviceService.getPort(cp);
        Optional<OchPort> ochPortOpt = OchPortHelper.asOchPort(port);

        if (ochPortOpt.isPresent()) {
            OchPort ochPort = ochPortOpt.get();
            GridType gridType = ochPort.lambda().gridType();
            ChannelSpacing channelSpacing = ochPort.lambda().channelSpacing();
            int slotGranularity = ochPort.lambda().slotGranularity();
            int multiplier = getMultplier(wavelength, gridType, channelSpacing);
            return new OchSignal(gridType, channelSpacing, multiplier, slotGranularity);
        } else {
            print("Connect point %s is not OChPort", cp);
            return null;
        }

    }

    private int getMultplier(long wavelength, GridType gridType, ChannelSpacing channelSpacing) {
        long baseFreq;
        switch (gridType) {
            case DWDM:
                baseFreq = BASE_FREQUENCY;
                break;
            case CWDM:
            case FLEX:
            case UNKNOWN:
            default:
                baseFreq = 0L;
                break;
        }
        if (wavelength > baseFreq) {
            return (int) ((wavelength - baseFreq) / (channelSpacing.frequency().asMHz()));
        } else {
            return (int) ((baseFreq - wavelength) / (channelSpacing.frequency().asMHz()));
        }
    }


    @Override
    protected void doExecute() throws Exception {
        if (operation.equals("edit-config")) {
            FlowRuleService flowService = get(FlowRuleService.class);
            DeviceService deviceService = get(DeviceService.class);
            CoreService coreService = get(CoreService.class);
            ConnectPoint cp = ConnectPoint.deviceConnectPoint(connectPointString);

            TrafficSelector.Builder trafficSelectorBuilder = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder trafficTreatmentBuilder = DefaultTrafficTreatment.builder();
            FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder();


            // an empty traffic selector
            TrafficSelector trafficSelector = trafficSelectorBuilder.matchInPort(cp.port()).build();
            OchSignal ochSignal;
            if (parameter.contains("/")) {
                ochSignal = createOchSignal();
            } else if (parameter.matches("-?\\d+(\\.\\d+)?")) {
                ochSignal = createOchSignalFromWavelength(deviceService, cp);
            } else {
                print("Signal or wavelength %s are in uncorrect format");
                return;
            }
            if (ochSignal == null) {
                print("Error in creating OchSignal");
                return;
            }
            TrafficTreatment trafficTreatment = trafficTreatmentBuilder
                    .add(Instructions.modL0Lambda(ochSignal))
                    .add(Instructions.createOutput(deviceService.getPort(cp).number()))
                    .build();

            Device device = deviceService.getDevice(cp.deviceId());
            int priority = 100;
            ApplicationId appId = coreService.registerApplication("org.onosproject.optical-model");
            log.info(appId.name());
            FlowRule addFlow = flowRuleBuilder
                    .withPriority(priority)
                    .fromApp(appId)
                    .withTreatment(trafficTreatment)
                    .withSelector(trafficSelector)
                    .forDevice(device.id())
                    .makePermanent()
                    .build();
            flowService.applyFlowRules(addFlow);
            String msg = String.format("Setting wavelength %s", ochSignal.centralFrequency().asGHz());
            print(msg);
        } else {
            print("Operation %s are not supported now.", operation);
        }

    }
}
