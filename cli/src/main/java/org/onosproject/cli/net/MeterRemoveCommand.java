/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterService;

import java.util.Collections;

/**
 * Remove existing meter from device.
 */
@Command(scope = "onos", name = "meter-remove",
        description = "Removes a meter from a device (currently for testing)")
public class MeterRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    private String uri = null;

    @Argument(index = 1, name = "meterId", description = "Meter ID hexadecimal value",
            required = true, multiValued = false)
    private String meterIdstr = null;

    private final String appId = "org.onosproject.cli.meterCmd";

    @Override
    protected void execute() {
        MeterService service = get(MeterService.class);
        CoreService coreService = get(CoreService.class);

        DeviceId deviceId = DeviceId.deviceId(uri);
        MeterId meterId = MeterId.meterId(Long.parseLong(meterIdstr, 16));

        Band b = new DefaultBand(Band.Type.DROP, 0L, 0L, (short) 0);

        MeterRequest.Builder builder = DefaultMeterRequest.builder()
                .forDevice(deviceId)
                .withBands(Collections.singleton(b))
                .withUnit(Meter.Unit.PKTS_PER_SEC)
                .fromApp(coreService.registerApplication(appId));
        MeterRequest meterRequest = builder.remove();
        service.withdraw(meterRequest, meterId);
        log.info("Requested meter removal: {}", meterRequest.toString());

        print("Requested meter removal: %s", meterRequest.toString());
    }
}
