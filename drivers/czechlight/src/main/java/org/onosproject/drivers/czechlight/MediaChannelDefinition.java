/*
 * Copyright 2019-2020 Jan Kundrát, CESNET, <jan.kundrat@cesnet.cz> and Open Networking Foundation
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

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.net.OchSignal;

import java.util.Map;
import java.util.stream.Collectors;

/** Media Channel definition specifies a frequency range, i.e., a channel in a flexgrid DWDM system as retrieved from
 * the ROADM device. We cannot use something like an OchSignal because this represents raw data on the device.
 */
class MediaChannelDefinition {
    public int lowMHz;
    public int highMHz;

    public MediaChannelDefinition(final int lowMHz, final int highMHz) {
        this.lowMHz = lowMHz;
        this.highMHz = highMHz;
    }

    public String toString() {
        return "Channel{" + String.valueOf(lowMHz / 1_000_000.0) + " - " + String.valueOf(highMHz / 1_000_000.0) + "}";
    }

    public static Map<String, MediaChannelDefinition> parseChannelDefinitions(final HierarchicalConfiguration xml) {
        return xml.configurationsAt("data.channel-plan.channel").stream()
                .collect(Collectors.toMap(x -> x.getString("name"),
                        x -> new MediaChannelDefinition(x.getInt("lower-frequency"),
                                x.getInt("upper-frequency"))));
    }

    public static boolean mcMatches(final Map.Entry<String, MediaChannelDefinition> entry, final OchSignal och) {
        return entry.getValue().lowMHz == och.centralFrequency().asMHz() - och.slotWidth().asMHz() / 2
                && entry.getValue().highMHz == och.centralFrequency().asMHz() + och.slotWidth().asMHz() / 2;
    }

};

