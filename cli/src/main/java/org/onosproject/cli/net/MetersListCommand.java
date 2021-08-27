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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterCellId;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterScope;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.runtime.PiMeterCellId;

import java.util.Collection;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Lists all meters.
 */
@Service
@Command(scope = "onos", name = "meters",
        description = "Shows meters")
public class MetersListCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Argument(index = 1, name = "index", description = "Index",
            required = false, multiValued = false)
    private String indexString = null;

    @Option(name = "-sc", aliases = "--scope", description = "Scope",
            required = false, multiValued = false)
    private String scopeString = null;

    MeterScope meterScope;
    Long index;
    MeterCellId meterCellId;

    @Override
    protected void doExecute() {

        if (!isNullOrEmpty(scopeString)) {
            meterScope = MeterScope.of(scopeString);
        }

        if (!isNullOrEmpty(indexString)) {
            index = Long.parseLong(indexString);
            if (meterScope == null) {
                meterScope = MeterScope.globalScope();
            }
        }

        if (index != null) {
            if (meterScope != null && meterScope.equals(MeterScope.globalScope())) {
                meterCellId = MeterId.meterId(index);
            } else if (meterScope != null) {
                meterCellId = PiMeterCellId.ofIndirect(PiMeterId.of(meterScope.id()), index);
            }
        }

        MeterService service = get(MeterService.class);

        Collection<Meter> meters;
        if (isNullOrEmpty(uri)) {
            meters = service.getAllMeters();
            printMeters(meters);
        } else {
            if (meterCellId != null) {
                Meter m = service.getMeter(DeviceId.deviceId(uri), meterCellId);
                if (m != null) {
                    print("%s", m);
                } else {
                    error("Meter %s not found for device %s", meterCellId, uri);
                }
            } else if (meterScope != null) {
                meters = service.getMeters(DeviceId.deviceId(uri), meterScope);
                printMeters(meters);
            } else {
                meters = service.getMeters(DeviceId.deviceId(uri));
                printMeters(meters);
            }
        }
    }

    private void printMeters(Collection<Meter> meters) {
        meters.forEach(m -> print(" %s", m));
    }
}
