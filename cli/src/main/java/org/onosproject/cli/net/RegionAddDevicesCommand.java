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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.net.region.RegionId;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Add a set of devices into existing region.
 */
@Service
@Command(scope = "onos", name = "region-add-devices",
        description = "Adds a set of devices into the region.")
public class RegionAddDevicesCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "id", description = "Region ID",
            required = true, multiValued = false)
    @Completion(RegionIdCompleter.class)
    String id = null;

    @Argument(index = 1, name = "devIds", description = "Device IDs",
            required = true, multiValued = true)
    @Completion(DeviceIdCompleter.class)
    List<String> devIds = null;

    @Override
    protected void doExecute() {
        RegionAdminService service = get(RegionAdminService.class);
        RegionId regionId = RegionId.regionId(id);

        List<DeviceId> dids = devIds.stream().map(s ->
                DeviceId.deviceId(s)).collect(Collectors.toList());

        service.addDevices(regionId, dids);
    }
}
