/*
 * Copyright 2014-2016 Open Networking Laboratory
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

package org.onosproject.drivers.bmv2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.bmv2.api.runtime.Bmv2Client;
import org.onosproject.bmv2.api.runtime.Bmv2RuntimeException;
import org.onosproject.bmv2.ctl.Bmv2ThriftClient;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class Bmv2PortDiscovery extends AbstractHandlerBehaviour
        implements PortDiscovery {

    private final Logger log =
            LoggerFactory.getLogger(this.getClass());

    @Override
    public List<PortDescription> getPorts() {
        Bmv2Client deviceClient;
        try {
            deviceClient = Bmv2ThriftClient.of(handler().data().deviceId());
        } catch (Bmv2RuntimeException e) {
            log.error("Failed to connect to Bmv2 device", e);
            return Collections.emptyList();
        }

        List<PortDescription> portDescriptions = Lists.newArrayList();

        try {

            deviceClient.getPortsInfo().forEach(
                    p -> {
                        DefaultAnnotations.Builder builder =
                                DefaultAnnotations.builder();
                        p.getExtraProperties().forEach(builder::set);
                        SparseAnnotations annotations = builder.build();

                        portDescriptions.add(new DefaultPortDescription(
                                PortNumber.portNumber(
                                        (long) p.portNumber(),
                                        p.ifaceName()),
                                p.isUp(),
                                annotations
                        ));
                    });
        } catch (Bmv2RuntimeException e) {
            log.error("Unable to get port description from Bmv2 device", e);
        }

        return ImmutableList.copyOf(portDescriptions);
    }
}
