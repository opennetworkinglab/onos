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

import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.optical.OmsPort;

import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Lambda query implementation for OFOpticalSwitch13.
 *
 * Note: Standard (ONF TS-022, March 15, 2015) does not support negative values for spacingMultiplier in exp_OCH_sigid.
 * Thus, Lambda values cannot be calculated using center and spacingMultiplier of +/- values.
 * Therefore, Lambda values are calculated with positive values only: starting from min-frequency of 191.7 THz.
 *
 * TODO: When standard is fixed - modify queryLambdas accordingly.
 *
 * OFOpticalSwitch13 exposes OchSignal resources: 'lambdaCount' lambdas with 50GHz width (fixed grid)
 * starting from min-frequency of 191.7 THz.
 */

public class OFOpticalSwitch13LambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        DeviceService deviceService = opticalView(this.handler().get(DeviceService.class));
        Port p = deviceService.getPort(this.data().deviceId(), port);

        // Only OMS ports expose lambda resources
        if (p == null || !p.type().equals(Port.Type.OMS)) {
            return Collections.emptySet();
        }

        short lambdaCount = ((OmsPort) p).totalChannels();
        // OMS ports expose 'lambdaCount' fixed grid lambdas of 50GHz width, starting from min-frequency 191.7 THz.
        return IntStream.rangeClosed(1, lambdaCount)
                .mapToObj(x -> OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, x))
                .collect(Collectors.toSet());
    }
}
