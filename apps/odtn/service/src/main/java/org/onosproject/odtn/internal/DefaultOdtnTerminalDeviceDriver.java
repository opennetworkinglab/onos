/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.odtn.behaviour.ConfigurableTransceiver;
import org.onosproject.odtn.behaviour.OdtnTerminalDeviceDriver;
import org.onosproject.odtn.behaviour.AbstractOdtnTerminalDeviceDriver;
import org.onosproject.odtn.behaviour.PlainTransceiver;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.osgi.DefaultServiceDirectory.getService;

import org.w3c.dom.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Device driver implementation for ODTN Phase1.0.
 * <p>
 * NETCONF SB should be provided by DCS, but currently DCS SB driver have
 * some critical problem to configure actual devices and netconf servers,
 * as a workaround this posts netconf edit-config directly.
 */
public final class DefaultOdtnTerminalDeviceDriver
        extends AbstractOdtnTerminalDeviceDriver implements OdtnTerminalDeviceDriver {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private DeviceService deviceService;

    private DefaultOdtnTerminalDeviceDriver() {
    }

    public static DefaultOdtnTerminalDeviceDriver create() {
        DefaultOdtnTerminalDeviceDriver self = new DefaultOdtnTerminalDeviceDriver();
        self.deviceService = getService(DeviceService.class);
        return self;
    }

    @Override
    public void apply(DeviceId did, PortNumber client, PortNumber line, boolean enable) {

        checkNotNull(did);
        checkNotNull(client);
        checkNotNull(line);

        List<CharSequence> nodes = new ArrayList<>();

        ConfigurableTransceiver transceiver =
                Optional.ofNullable(did)
                        .map(deviceService::getDevice)
                        .filter(device -> device.is(ConfigurableTransceiver.class))
                        .map(device -> device.as(ConfigurableTransceiver.class))
                        .orElseGet(() -> new PlainTransceiver());

        nodes.addAll(transceiver.enable(client, line, enable));
        if (nodes.size() == 0) {
            log.warn("Nothing to be configured.");
            return;
        }

        Document doc = buildEditConfigBody(nodes);
        configureDevice(did, doc);
    }
}
