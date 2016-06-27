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
package org.onosproject.driver.optical.query;

import org.onlab.util.GuavaCollectors;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import com.google.common.collect.ImmutableSet;

import java.util.Set;
import java.util.stream.IntStream;

/**
 * Lambda query implementation for LINC-OE Optical Emulator switch.
 *
 * The LINC ROADM emulator exposes two types of ports: OCh ports connect to ports in the packet layer,
 * while OMS ports connect to an OMS port on a neighbouring ROADM.
 *
 * LINC exposes OchSignal resources: 80 lambdas of 50 GHz (fixed grid) around ITU-T G.694.1 center frequency 193.1 GHz.
 */

public class LincOELambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {

    private static final int LAMBDA_COUNT = 80;

    /**
     * OMS ports expose 80 fixed grid lambdas of 50GHz width,
     * centered around the ITU-T center frequency 193.1 THz.
     */
    private static final Set<OchSignal> OMS_LAMDAS = IntStream.range(0, LAMBDA_COUNT)
            .mapToObj(x -> new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, x - (LAMBDA_COUNT / 2), 4))
            .collect(GuavaCollectors.toImmutableSet());

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        DeviceService deviceService = this.handler().get(DeviceService.class);

        Port p = deviceService.getPort(this.data().deviceId(), port);

        // OCh ports don't expose lambda resources
        if (p == null ||
            !p.type().equals(Port.Type.OMS)) {
            return ImmutableSet.of();
        }

        return OMS_LAMDAS;
    }
}
