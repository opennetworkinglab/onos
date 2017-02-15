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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterService;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.HashSet;
import java.util.Set;

/**
 * Add a meter to a device.
 */
@Command(scope = "onos", name = "meter-add",
        description = "Adds a meter to a device (currently for testing)")
public class MeterAddCommand extends AbstractShellCommand {

    private Meter.Unit unit;
    private Set<Band> bands = new HashSet<>();
    private Long rate;
    private Long burstSize;


    @Option(name = "-bd", aliases = "--bandDrop",
            description = "Assign band DROP to this meter",
            required = false, multiValued = false)
    private boolean hasBandDrop = false;

    @Option(name = "-br", aliases = "--bandRemark",
            description = "Assign band REMARK to this meter",
            required = false, multiValued = false)
    private boolean hasBandRemark = false;

    @Option(name = "-up", aliases = "--unitPkts",
            description = "Assign unit Packets per Second to this meter",
            required = false, multiValued = false)
    private boolean hasPkts = false;

    @Option(name = "-uk", aliases = "--unitKbps",
            description = "Assign unit Kilobits per Second to this meter",
            required = false, multiValued = false)
    private boolean hasKbps = false;

    @Option(name = "-ib", aliases = "--isBurst",
            description = "Set meter applicable only to burst",
            required = false, multiValued = false)
    private boolean isBurst = false;

    @Option(name = "-b", aliases = "--bandwidth", description = "Bandwidth",
            required = false, multiValued = false)
    private String bandwidthString = null;

    @Option(name = "-bs", aliases = "--burstSize", description = "Burst size",
            required = false, multiValued = false)
    private String burstSizeString = null;

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    private String uri = null;

    private final String appId = "org.onosproject.cli.meterCmd";

    private void checkOptions() {
        // check units
        if (hasPkts) {
            unit = Meter.Unit.PKTS_PER_SEC;
        } else {
            unit = Meter.Unit.KB_PER_SEC;
        }

        // check rate (does not take into account if it is kbps or pkts)
        if (!isNullOrEmpty(bandwidthString)) {
            rate = Long.parseLong(bandwidthString);
        } else {
            rate = 500L;
        }

        // burst size
        if (!isNullOrEmpty(burstSizeString)) {
            burstSize = Long.parseLong(burstSizeString);
        } else {
            burstSize = 0L;
        }

        // Create bands
        if (hasBandDrop) {
            Band band = DefaultBand.builder()
                    .ofType(Band.Type.DROP)
                    .withRate(rate)
                    .burstSize(burstSize)
                    .build();
            bands.add(band);
        }
        if (hasBandRemark) {
            Band band = DefaultBand.builder()
                    .ofType(Band.Type.REMARK)
                    .withRate(rate)
                    .burstSize(burstSize)
                    .build();
            bands.add(band);
        }
        // default band is drop
        if (bands.size() == 0) {
            Band band = DefaultBand.builder()
                    .ofType(Band.Type.DROP)
                    .withRate(rate)
                    .burstSize(burstSize)
                    .build();
            bands.add(band);
        }



    }

    @Override
    protected void execute() {
        MeterService service = get(MeterService.class);
        CoreService coreService = get(CoreService.class);

        DeviceId deviceId = DeviceId.deviceId(uri);

        checkOptions();


        MeterRequest.Builder builder = DefaultMeterRequest.builder()
                .forDevice(deviceId)
                .fromApp(coreService.registerApplication(appId))
                .withUnit(unit)
                .withBands(bands);


        if (isBurst) {
            builder = builder.burst();
        }

        MeterRequest request = builder.add();

        Meter m = service.submit(request);
        log.info("Requested meter with id {}: {}", m.id().toString(), m.toString());
        print("Requested meter with id %s: %s", m.id().toString(), m.toString());
    }
}
