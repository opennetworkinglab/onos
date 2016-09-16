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

import com.google.common.collect.Collections2;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterService;

import java.util.Collection;

/**
 * Lists all meters.
 */
@Command(scope = "onos", name = "meters",
        description = "Shows meters")
public class MetersListCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    String uri = null;


    @Override
    protected void execute() {
        MeterService service = get(MeterService.class);

        Collection<Meter> meters = service.getAllMeters();
        if (uri != null) {
            DeviceId deviceId = DeviceId.deviceId(uri);
            Collection<Meter> devMeters = Collections2.filter(meters,
                                                              m -> m.deviceId().equals(deviceId));
            printMeters(devMeters);
        } else {
            printMeters(meters);
        }
    }

    private void printMeters(Collection<Meter> meters) {
        meters.forEach(m -> print(" %s", m));
    }
}
