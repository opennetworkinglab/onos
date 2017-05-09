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
import org.onosproject.net.OduSignalType;
import org.onosproject.net.OtuSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OtuPort;
import org.onosproject.net.behaviour.TributarySlotQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

import java.util.Collections;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * TributarySlotQuery implementation which responds that all slots of ODU2 or ODU4 are available for the port.
 */
public class DefaultTributarySlotQuery extends AbstractHandlerBehaviour implements TributarySlotQuery {

    private static final Logger log = LoggerFactory.getLogger(DefaultTributarySlotQuery.class);

    private static final int TOTAL_ODU2_TRIBUTARY_SLOTS = 8;
    private static final int TOTAL_ODU4_TRIBUTARY_SLOTS = 80;

    private static Set<TributarySlot> getEntireOdu2TributarySlots() {
        return IntStream.rangeClosed(1, TOTAL_ODU2_TRIBUTARY_SLOTS)
                .mapToObj(TributarySlot::of)
                .collect(GuavaCollectors.toImmutableSet());
    }

    private static Set<TributarySlot> getEntireOdu4TributarySlots() {
        return IntStream.rangeClosed(1, TOTAL_ODU4_TRIBUTARY_SLOTS)
                .mapToObj(TributarySlot::of)
                .collect(GuavaCollectors.toImmutableSet());
    }

    private static final Set<TributarySlot> ENTIRE_ODU2_TRIBUTARY_SLOTS = getEntireOdu2TributarySlots();
    private static final Set<TributarySlot> ENTIRE_ODU4_TRIBUTARY_SLOTS = getEntireOdu4TributarySlots();

    @Override
    public Set<TributarySlot> queryTributarySlots(PortNumber port) {
        // currently return all slots by default.
        DeviceService deviceService = opticalView(this.handler().get(DeviceService.class));
        Port p = deviceService.getPort(this.data().deviceId(), port);

        if (p == null) {
            return Collections.emptySet();
        }

        switch (p.type()) {
            case OCH:
                return queryOchTributarySlots(p);
            case OTU:
                return queryOtuTributarySlots(p);
            default:
                return Collections.emptySet();
        }
    }

    private Set<TributarySlot> queryOchTributarySlots(Port ochPort) {
        OduSignalType signalType = null;
        if (ochPort instanceof OchPort) {
            signalType = ((OchPort) ochPort).signalType();
        } else {
            log.warn("{} was not an OchPort", ochPort);
            return Collections.emptySet();
        }

        switch (signalType) {
            case ODU2:
                return ENTIRE_ODU2_TRIBUTARY_SLOTS;
            case ODU4:
                return ENTIRE_ODU4_TRIBUTARY_SLOTS;
            default:
                log.error("Unsupported signal type {} for {}", signalType, ochPort);
                return Collections.emptySet();
        }
    }

    private Set<TributarySlot> queryOtuTributarySlots(Port otuPort) {
        OtuSignalType signalType = null;
        if (otuPort instanceof OtuPort) {
            signalType = ((OtuPort) otuPort).signalType();
        } else {
            log.warn("{} was not an OtuPort", otuPort);
            return Collections.emptySet();
        }

        switch (signalType) {
            case OTU2:
                return ENTIRE_ODU2_TRIBUTARY_SLOTS;
            case OTU4:
                return ENTIRE_ODU4_TRIBUTARY_SLOTS;
            default:
                log.error("Unsupported signal type {} for {}", signalType, otuPort);
                return Collections.emptySet();
        }
    }
}