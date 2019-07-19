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
package org.onosproject.drivers.lumentum;

import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.net.GridType;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.Collections;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

/**
 * Implementation of lambda query interface for Lumentum ROADMs.
 *
 * Tested on both Lumentum SDN ROADM and Lumentum ROADM-A Whitebox.
 *
 * 96 OchSignals of 50 GHz, center frequencies range 191.350 - 196.100 THz.
 * 48 OchSignals of 100 GHz, center frequencies 191.375 - 196.075 THz
 */
public class LumentumRoadmLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {


    public static final GridType GRID_TYPE = GridType.DWDM;

    public static final ChannelSpacing CHANNEL_SPACING_50 = ChannelSpacing.CHL_50GHZ;
    public static final Frequency START_CENTER_FREQ_50 = Frequency.ofGHz(191_350);
    public static final Frequency END_CENTER_FREQ_50 = Frequency.ofGHz(196_100);
    private static final int LAMBDA_COUNT_50 = 96;

    public static final ChannelSpacing CHANNEL_SPACING_100 = ChannelSpacing.CHL_100GHZ;
    public static final Frequency START_CENTER_FREQ_100 = Frequency.ofGHz(191_375);
    public static final Frequency END_CENTER_FREQ_100 = Frequency.ofGHz(196_075);
    private static final int LAMBDA_COUNT_100 = 48;

    @Override
    public Set<OchSignal> queryLambdas(PortNumber portNumber) {
        DeviceService deviceService = this.handler().get(DeviceService.class);
        Port port = deviceService.getPort(data().deviceId(), portNumber);

        if ((port.type() == Port.Type.FIBER) || (port.type() == Port.Type.OMS)) {

            //Complete set of 50GHz OchSignal
            int startMultiplier50 = (int) (START_CENTER_FREQ_50.subtract(Spectrum.CENTER_FREQUENCY).asHz()
                    / Frequency.ofGHz(50).asHz());

            Set<OchSignal> channels50 = IntStream.range(0, LAMBDA_COUNT_50)
                    .mapToObj(x -> new OchSignal(GRID_TYPE, CHANNEL_SPACING_50,
                            startMultiplier50 + x,
                            4))
                    .collect(Collectors.toSet());

        /*//Complete set of 100GHz OchSignal
        int startMultiplier100 = (int) (START_CENTER_FREQ_100.subtract(Spectrum.CENTER_FREQUENCY).asHz()
                / Frequency.ofGHz(100).asHz());

        Set<OchSignal> channels100 = IntStream.range(0, LAMBDA_COUNT_100)
                .mapToObj(x -> new OchSignal(GRID_TYPE,
                        CHANNEL_SPACING_100, startMultiplier100 + x, 8))
                .collect(Collectors.toSet());

        Set<OchSignal> channels = Sets.union(channels50, channels100);*/

            return channels50;
        } else {
            return Collections.emptySet();
        }
    }
}


