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
package org.onosproject.drivers.czechlight;

import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
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
 * Implementation of lambda query interface for CzechLight ROADMs.
 *
 * These devices are actually fully flexgrid-capable. Same as the other devices supported by ONOS,
 * we're just returning a dummy list of 50 GHz channels.
 */
public class CzechLightLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {

    private static final Frequency START_CENTER_FREQ_50 = Frequency.ofGHz(191_350);
    private static final Frequency END_CENTER_FREQ_50 = Frequency.ofGHz(196_100);

    @Override
    public Set<OchSignal> queryLambdas(PortNumber portNumber) {
        DeviceService deviceService = this.handler().get(DeviceService.class);
        Port port = deviceService.getPort(data().deviceId(), portNumber);

        if ((port.type() == Port.Type.FIBER) || (port.type() == Port.Type.OMS)) {
            final int startMultiplier50 = (int) (START_CENTER_FREQ_50.subtract(Spectrum.CENTER_FREQUENCY).asHz()
                    / Frequency.ofGHz(50).asHz());
            final int endMultiplier50 = (int) (END_CENTER_FREQ_50.subtract(Spectrum.CENTER_FREQUENCY).asHz()
                    / Frequency.ofGHz(50).asHz());
            return IntStream.range(startMultiplier50, endMultiplier50 + 1)
                    .mapToObj(x -> OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, x))
                    .collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }
}


