/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterService;

import java.util.Collections;

/**
 * Add a meter.
 */
@Command(scope = "onos", name = "add-meter",
        description = "Adds a meter to a device (currently for testing)")
public class AddMeter extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    String uri = null;

    private final String appId = "org.onosproject.cli.addMeter";

    @Override
    protected void execute() {
        MeterService service = get(MeterService.class);
        CoreService coreService = get(CoreService.class);

        DeviceId deviceId = DeviceId.deviceId(uri);

        MeterId meterId = service.allocateMeterId();

        Band band = DefaultBand.builder()
                        .ofType(Band.Type.DROP)
                        .withRate(500)
                        .build();


        Meter meter = DefaultMeter.builder()
                .forDevice(deviceId)
                .fromApp(coreService.registerApplication(appId))
                .withId(meterId)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(Collections.singleton(band))
                .build();

        MeterOperation op = new MeterOperation(meter, MeterOperation.Type.ADD, null);

        service.addMeter(op);

    }
}
