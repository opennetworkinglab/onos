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
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterScope;
import org.onosproject.net.meter.MeterService;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.HashSet;
import java.util.Set;

/**
 * Add a meter to a device.
 */
@Service
@Command(scope = "onos", name = "meter-add",
        description = "Adds a meter to a device (currently for testing)")
public class MeterAddCommand extends AbstractShellCommand {

    private Meter.Unit unit;
    private Set<Band> bands = new HashSet<>();
    private MeterScope scope;
    private Long index;

    @Option(name = "-bd", aliases = "--bandDrop",
            description = "Assign band DROP to this meter",
            required = false, multiValued = false)
    private boolean hasBandDrop = false;

    @Option(name = "-br", aliases = "--bandRemark",
            description = "Assign band REMARK to this meter",
            required = false, multiValued = false)
    private boolean hasBandRemark = false;

    @Option(name = "-by", aliases = "--bandYel",
            description = "Assign band MARK_YELLOW to this meter",
            required = false, multiValued = false)
    private boolean hasBandYel = false;

    @Option(name = "-bre", aliases = "--bandRed",
            description = "Assign band MARK_RED to this meter",
            required = false, multiValued = false)
    private boolean hasBandRed = false;

    @Option(name = "-up", aliases = "--unitPkts",
            description = "Assign unit Packets per Second to this meter",
            required = false, multiValued = false)
    private boolean hasPkts = false;

    @Option(name = "-uk", aliases = "--unitKbps",
            description = "Assign unit Kilobits per Second to this meter",
            required = false, multiValued = false)
    private boolean hasKbps = false;

    @Option(name = "-ub", aliases = "--unitBytes",
            description = "Assign unit Bytes per Second to this meter",
            required = false, multiValued = false)
    private boolean hasBytes = false;

    @Option(name = "-ib", aliases = "--isBurst",
            description = "Set meter applicable only to burst",
            required = false, multiValued = false)
    private boolean isBurst = false;

    @Option(name = "-b", aliases = "--bandwidth", description = "Bandwidth",
            required = false, multiValued = true)
    private String[] bandwidthString = null;

    @Option(name = "-bs", aliases = "--burstSize", description = "Burst size",
            required = false, multiValued = true)
    private String[] burstSizeString = null;

    @Option(name = "-sc", aliases = "--scope", description = "Scope",
            required = false, multiValued = false)
    private String scopeString = null;

    @Option(name = "-id", aliases = "--index", description = "Index",
            required = false, multiValued = false)
    private String indexString = null;

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private String uri = null;

    private final String appId = "org.onosproject.cli.meterCmd";

    private void checkOptions() {
        // check units
        if (hasPkts) {
            unit = Meter.Unit.PKTS_PER_SEC;
        } else if (hasKbps) {
            unit = Meter.Unit.KB_PER_SEC;
        } else if (hasBytes) {
            unit = Meter.Unit.BYTES_PER_SEC;
        }

        int numBands = 0;
        if (hasBandDrop) {
            numBands++;
        }
        if (hasBandRemark) {
            numBands++;
        }
        if (hasBandYel) {
            numBands++;
        }
        if (hasBandRed) {
            numBands++;
        }

        long[] rates = new long[numBands];
        long[] bursts = new long[numBands];
        // check rate (does not take into account if it is kbps or pkts)
        if (bandwidthString != null && bandwidthString.length == numBands &&
                burstSizeString != null && burstSizeString.length == numBands) {
            for (int i = 0; i < bandwidthString.length; i++) {
                rates[i] = 500L;
                bursts[i] = 0L;
                if (!isNullOrEmpty(bandwidthString[i])) {
                    rates[i] = Long.parseLong(bandwidthString[i]);
                }
                if (!isNullOrEmpty(burstSizeString[i])) {
                    bursts[i] = Long.parseLong(burstSizeString[i]);
                }
            }
        } else if (bandwidthString != null && bandwidthString.length < numBands &&
                burstSizeString != null && burstSizeString.length < numBands) {
            for (int i = 0; i < numBands; i++) {
                rates[i] = 500L;
                bursts[i] = 0L;
                if (i < bandwidthString.length && !isNullOrEmpty(bandwidthString[i])) {
                    rates[i] = Long.parseLong(bandwidthString[i]);
                }
                if (i < burstSizeString.length && !isNullOrEmpty(burstSizeString[i])) {
                    bursts[i] = Long.parseLong(burstSizeString[i]);
                }
            }
        }

        // Create bands
        int i = 0;
        if (hasBandDrop) {
            Band band = DefaultBand.builder()
                    .ofType(Band.Type.DROP)
                    .withRate(rates[i])
                    .burstSize(bursts[i])
                    .build();
            bands.add(band);
            i++;
        }
        if (hasBandRemark) {
            Band band = DefaultBand.builder()
                    .ofType(Band.Type.REMARK)
                    .withRate(rates[i])
                    .burstSize(bursts[i])
                    .build();
            bands.add(band);
            i++;
        }
        if (hasBandYel) {
            Band band = DefaultBand.builder()
                    .ofType(Band.Type.MARK_YELLOW)
                    .withRate(rates[i])
                    .burstSize(bursts[i])
                    .build();
            bands.add(band);
            i++;
        }
        if (hasBandRed) {
            Band band = DefaultBand.builder()
                    .ofType(Band.Type.MARK_RED)
                    .withRate(rates[i])
                    .burstSize(bursts[i])
                    .build();
            bands.add(band);
        }

        // default band is drop
        if (bands.size() == 0) {
            Band band = DefaultBand.builder()
                    .ofType(Band.Type.DROP)
                    .withRate(500L)
                    .burstSize(0L)
                    .build();
            bands.add(band);
        }

        if (!isNullOrEmpty(scopeString)) {
            scope = MeterScope.of(scopeString);
        }

        if (!isNullOrEmpty(indexString) && scope != null) {
            index = Long.parseLong(indexString);
        }
    }

    @Override
    protected void doExecute() {
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

        // Scope is by default global but we can still provide the index
        // otherwise we can specify both scope and index or let the meter
        // service allocate the meter for us. User defined index requires
        // the user defined mode being active.
        if (scope != null) {
            builder = builder.withScope(scope);
        }

        if (index != null) {
            builder = builder.withIndex(index);
        }

        MeterRequest request = builder.add();

        Meter m = service.submit(request);
        log.info("Requested meter with cellId {}: {}", m.meterCellId().toString(), m.toString());
        print("Requested meter with cellId %s: %s", m.meterCellId().toString(), m.toString());
    }
}
