/*
 * Copyright 2017-present Open Networking Foundation
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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.MeterCellId;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterScope;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.runtime.PiMeterCellId;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Remove existing meter from device.
 */
@Service
@Command(scope = "onos", name = "meter-remove",
        description = "Removes a meter from a device (currently for testing)")
public class MeterRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private String uri = null;

    @Argument(index = 1, name = "index", description = "Index",
            required = true, multiValued = false)
    private String indexString = null;

    @Option(name = "-sc", aliases = "--scope", description = "Scope",
            required = false, multiValued = false)
    private String scopeString = null;

    private final String appId = "org.onosproject.cli.meterCmd";

    @Override
    protected void doExecute() {
        MeterService service = get(MeterService.class);
        CoreService coreService = get(CoreService.class);

        DeviceId deviceId = DeviceId.deviceId(uri);

        MeterScope scope = MeterScope.globalScope();
        if (!isNullOrEmpty(scopeString)) {
            scope = MeterScope.of(scopeString);
        }

        MeterCellId meterCellId;
        long index = Long.parseLong(indexString);
        if (scope.equals(MeterScope.globalScope())) {
            meterCellId = MeterId.meterId(index);
        } else {
            meterCellId = PiMeterCellId.ofIndirect(PiMeterId.of(scope.id()), index);
        }

        MeterRequest.Builder builder = DefaultMeterRequest.builder()
                .forDevice(deviceId)
                .fromApp(coreService.registerApplication(appId));
        MeterRequest meterRequest = builder.remove();
        service.withdraw(builder.remove(), meterCellId);

        log.info("Requested meter {} removal: {}", meterCellId.toString(), meterRequest.toString());
        print("Requested meter %s removal: %s", meterCellId.toString(), meterRequest.toString());
    }
}
