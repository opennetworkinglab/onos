/*
 * Copyright 2018 Open Networking Foundation
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
package org.onosproject.drivers.polatis.openflow;

import org.onlab.util.GuavaCollectors;
import org.onlab.util.Spectrum;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowSwitch;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescProp;
import org.projectfloodlight.openflow.protocol.OFPortDescPropOptical;

import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.onosproject.openflow.controller.Dpid.dpid;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Lambda query implementation for OpenFlow Optical Circuit Switch.
 */
public class OpenFlowLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {

    private static final Logger log = getLogger(OpenFlowLambdaQuery.class);

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        Set<OchSignal> signals = new LinkedHashSet<>();
        for (OFPortDesc pd : getPortDescs()) {
            if (pd.getPortNo().getPortNumber() == port.toLong()) {
                for (OFPortDescProp prop : pd.getProperties()) {
                    if (prop instanceof OFPortDescPropOptical) {
                        OFPortDescPropOptical oprop = (OFPortDescPropOptical) prop;
                        long txMin = oprop.getTxMinFreqLmda();
                        long txMax = oprop.getTxMaxFreqLmda();
                        long txGrid = oprop.getTxGridFreqLmda();
                        signals.addAll(signals(txMin, txMax, txGrid));
                        long rxMin = oprop.getRxMinFreqLmda();
                        long rxMax = oprop.getRxMaxFreqLmda();
                        long rxGrid = oprop.getRxGridFreqLmda();
                        signals.addAll(signals(rxMin, rxMax, rxGrid));
                    }
                }
            }
        }
        return signals;

    }

    private Set<OchSignal> signals(long min, long max, long grid) {
        if (Spectrum.O_BAND_MIN.asMHz() > min) {
            log.warn("Out of range frequency (below the O-band minimum)");
        }
        if (Spectrum.U_BAND_MAX.asMHz() < max) {
            log.warn("Out of range frequency (above the U-band maximum)");
        }

        double centerFrequencyMHz = Spectrum.CENTER_FREQUENCY.asMHz();
        long startSpacingMultiplier = (long) (min - centerFrequencyMHz) / grid;
        long stopSpacingMultiplier = (long) (max - centerFrequencyMHz) / grid;

        Set<OchSignal> signals = new LinkedHashSet<>();

        if (grid == ChannelSpacing.CHL_100GHZ.frequency().asMHz()) {
            signals = IntStream.rangeClosed((int) startSpacingMultiplier,
                        (int) stopSpacingMultiplier)
                    .mapToObj(i -> new OchSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, i, 8))
                    .collect(GuavaCollectors.toImmutableSet());
        } else if (grid == ChannelSpacing.CHL_50GHZ.frequency().asMHz()) {
            signals = IntStream.rangeClosed((int) startSpacingMultiplier,
                        (int) stopSpacingMultiplier)
                    .mapToObj(i -> new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, i, 4))
                    .collect(GuavaCollectors.toImmutableSet());
        } else if (grid == ChannelSpacing.CHL_25GHZ.frequency().asMHz()) {
            signals = IntStream.rangeClosed((int) startSpacingMultiplier,
                        (int) stopSpacingMultiplier)
                    .mapToObj(i -> new OchSignal(GridType.DWDM, ChannelSpacing.CHL_25GHZ, i, 2))
                    .collect(GuavaCollectors.toImmutableSet());
        } else if (grid == ChannelSpacing.CHL_12P5GHZ.frequency().asMHz()) {
            signals = IntStream.rangeClosed((int) startSpacingMultiplier,
                        (int) stopSpacingMultiplier)
                    .mapToObj(i -> new OchSignal(GridType.DWDM, ChannelSpacing.CHL_6P25GHZ, i, 1))
                    .collect(GuavaCollectors.toImmutableSet());
        } else if (grid == ChannelSpacing.CHL_6P25GHZ.frequency().asMHz()) {
            // Only consider odd values for the multiplier (for easy mapping to fixed grid)
            signals = IntStream.rangeClosed((int) startSpacingMultiplier,
                        (int) stopSpacingMultiplier)
                    .filter(i -> i % 2 == 1)
                    .mapToObj(i -> new OchSignal(GridType.FLEX, ChannelSpacing.CHL_6P25GHZ, i, 1))
                    .collect(GuavaCollectors.toImmutableSet());
        } else {
            log.warn("Unsupported channel spacing");
        }

        return signals;
    }

   private List<OFPortDesc> getPortDescs() {
        final Dpid dpid = dpid(handler().data().deviceId().uri());
        OpenFlowSwitch sw = handler().get(OpenFlowController.class).getSwitch(dpid);
        return sw.getPorts();
    }
}
