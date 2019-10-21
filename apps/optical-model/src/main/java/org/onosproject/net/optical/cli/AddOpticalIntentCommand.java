/*
 * Copyright 2016-present Open Networking Foundation
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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.net.ConnectPointCompleter;
import org.onosproject.cli.net.ConnectivityIntentCommand;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;

import java.util.Map;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.util.OpticalIntentUtility.createOpticalIntent;

/**
 * Installs optical connectivity or circuit intents, depending on given port types.
 */
@Service
@Command(scope = "onos", name = "add-optical-intent",
        description = "Installs optical connectivity intent")
public class AddOpticalIntentCommand extends ConnectivityIntentCommand {
    private static final String SIGNAL_FORMAT = "slotGranularity/channelSpacing(in GHz e.g 6.25,12.5,25,50,100)/" +
            "spaceMultiplier/gridType(cwdm, flex, dwdm) " + "e.g 1/6.25/1/flex";

    private static final String CH_6P25 = "6.25";
    private static final String CH_12P5 = "12.5";
    private static final String CH_25 = "25";
    private static final String CH_50 = "50";
    private static final String CH_100 = "100";

    private static final Map<String, ChannelSpacing> CHANNEL_SPACING_MAP = ImmutableMap
            .<String, ChannelSpacing>builder()
            .put(CH_6P25, ChannelSpacing.CHL_6P25GHZ)
            .put(CH_12P5, ChannelSpacing.CHL_12P5GHZ)
            .put(CH_25, ChannelSpacing.CHL_25GHZ)
            .put(CH_50, ChannelSpacing.CHL_50GHZ)
            .put(CH_100, ChannelSpacing.CHL_100GHZ)
            .build();

    @Argument(index = 0, name = "ingress",
            description = "Ingress Device/Port Description",
            required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    String ingressString = "";

    @Argument(index = 1, name = "egress",
            description = "Egress Device/Port Description",
            required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    String egressString = "";

    @Option(name = "-b", aliases = "--bidirectional",
            description = "If this argument is passed the optical link created will be bidirectional, " +
                    "else the link will be unidirectional.",
            required = false, multiValued = false)
    private boolean bidirectional = false;

    @Option(name = "-s", aliases = "--signal",
            description = "Optical Signal. Format = " + SIGNAL_FORMAT,
            required = false, multiValued = false)
    private String signal;

    private ConnectPoint createConnectPoint(String devicePortString) {
        String[] splitted = devicePortString.split("/");

        checkArgument(splitted.length == 2,
                "Connect point must be in \"deviceUri/portNumber\" format");

        DeviceId deviceId = DeviceId.deviceId(splitted[0]);
        DeviceService deviceService = get(DeviceService.class);

        List<Port> ports = deviceService.getPorts(deviceId);

        for (Port port : ports) {
            if (splitted[1].equals(port.number().name())) {
                return new ConnectPoint(deviceId, port.number());
            }
        }

        return null;
    }

    private OchSignal createOchSignal() throws IllegalArgumentException {
        if (signal == null) {
            return null;
        }
        try {
            String[] splitted = signal.split("/");
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
                    signal, SIGNAL_FORMAT);
            print(msg);
            throw new IllegalArgumentException(msg, e);
        }
    }


    @Override
    protected void doExecute() {
        IntentService service = get(IntentService.class);
        DeviceService deviceService = get(DeviceService.class);
        ConnectPoint ingress = createConnectPoint(ingressString);
        ConnectPoint egress = createConnectPoint(egressString);

        Intent intent = createOpticalIntent(ingress, egress, deviceService,
                key(), appId(), bidirectional, createOchSignal(), null);

        service.submit(intent);
        print("Optical intent submitted:\n%s", intent.toString());
    }
}
