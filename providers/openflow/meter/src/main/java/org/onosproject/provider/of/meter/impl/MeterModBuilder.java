/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.of.meter.impl;

import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMeterFlags;
import org.projectfloodlight.openflow.protocol.OFMeterMod;
import org.projectfloodlight.openflow.protocol.OFMeterModCommand;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBand;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBandDrop;
import org.projectfloodlight.openflow.protocol.meterband.OFMeterBandDscpRemark;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Builder for a meter modification.
 */
public final class MeterModBuilder {

    private final Logger log = getLogger(getClass());

    private final long xid;
    private final OFFactory factory;
    private Meter.Unit unit = Meter.Unit.KB_PER_SEC;
    private boolean burst = false;
    private Long id;
    private Collection<Band> bands;

    public MeterModBuilder(long xid, OFFactory factory) {
        this.xid = xid;
        this.factory = factory;
    }

    public static MeterModBuilder builder(long xid, OFFactory factory) {
        return new MeterModBuilder(xid, factory);
    }

    public MeterModBuilder withRateUnit(Meter.Unit unit) {
        this.unit = unit;
        return this;
    }

    public MeterModBuilder burst() {
        this.burst = true;
        return this;
    }

    public MeterModBuilder withId(MeterId meterId) {
        this.id = meterId.id();
        return this;
    }

    public MeterModBuilder withBands(Collection<Band> bands) {
        this.bands = bands;
        return this;
    }

    public OFMeterMod add() {
        validate();
        OFMeterMod.Builder builder = builderMeterMod();
        builder.setCommand(OFMeterModCommand.ADD);
        return builder.build();
    }

    public OFMeterMod remove() {
        validate();
        OFMeterMod.Builder builder = builderMeterMod();
        builder.setCommand(OFMeterModCommand.DELETE);
        return builder.build();
    }

    public OFMeterMod modify() {
        validate();
        OFMeterMod.Builder builder = builderMeterMod();
        builder.setCommand(OFMeterModCommand.MODIFY);
        return builder.build();
    }

    private OFMeterMod.Builder builderMeterMod() {
        OFMeterMod.Builder builder = factory.buildMeterMod();
        Set<OFMeterFlags> flags = EnumSet.noneOf(OFMeterFlags.class);
        if (burst) {
            flags.add(OFMeterFlags.BURST);
        }
        switch (unit) {
            case PKTS_PER_SEC:
                flags.add(OFMeterFlags.PKTPS);
                break;
            case KB_PER_SEC:
                flags.add(OFMeterFlags.KBPS);
                break;
            default:
                log.warn("Unknown unit type {}", unit);
        }
        //FIXME: THIS WILL CHANGE IN OF1.4 to setBands.
        builder.setMeters(buildBands());
        builder.setFlags(flags)
                .setMeterId(id)
                .setXid(xid);
        return builder;
    }

    private List<OFMeterBand> buildBands() {
        return bands.stream().map(b -> {
            switch (b.type()) {
                case DROP:
                    OFMeterBandDrop.Builder dropBuilder =
                            factory.meterBands().buildDrop();
                    if (burst) {
                        dropBuilder.setBurstSize(b.burst());
                    }
                    dropBuilder.setRate(b.rate());
                    return dropBuilder.build();
                case REMARK:
                    OFMeterBandDscpRemark.Builder remarkBand =
                            factory.meterBands().buildDscpRemark();
                    if (burst) {
                        remarkBand.setBurstSize(b.burst());
                    }
                    remarkBand.setRate(b.rate());
                    remarkBand.setPrecLevel(b.dropPrecedence());
                    return remarkBand.build();
                default:
                    log.warn("Unknown band type {}", b.type());
                    return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void validate() {
        checkNotNull(id, "id cannot be null");
        checkNotNull(bands, "Must have bands");
        checkArgument(!bands.isEmpty(), "Must have at lease one band");
    }
}
