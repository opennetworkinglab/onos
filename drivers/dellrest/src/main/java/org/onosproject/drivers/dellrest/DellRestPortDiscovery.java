/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.drivers.dellrest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import java.util.List;

import javax.ws.rs.core.MediaType;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the ports from a Dell Rest device.
 */
@Deprecated
public class DellRestPortDiscovery extends AbstractHandlerBehaviour {

    private final Logger log = getLogger(getClass());
    private static final String NAME = "name";
    private static final String INTERFACES = "interface";
    private static final String INTERFACES_REQUEST = "/running/dell/interfaces";
    private static final String TENGINTERFACENAME = "Te ";


    @Deprecated
    public List<PortDescription> getPorts() {
        List<PortDescription> ports = Lists.newArrayList();
        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();
        String remotePortName = "";

        // read configuration from REST API
        HierarchicalConfiguration config = XmlConfigParser.
                loadXml(controller.get(deviceId, INTERFACES_REQUEST, MediaType.valueOf("*/*")));

        // get the interfaces part
        List<HierarchicalConfiguration> portsConfig = parseDellPorts(config);

        portsConfig.stream().forEach(sub -> {
            String portName = sub.getString(NAME);
            DefaultAnnotations.Builder annotations = DefaultAnnotations.builder()
                    .set(AnnotationKeys.PORT_NAME, portName);
            // TODO: obtain proper port speed and type
            long portSpeed = 10_000;
            Port.Type portType = Port.Type.COPPER;
            PortNumber portNumber = PortNumber.fromString(remotePortName.replaceAll(TENGINTERFACENAME, ""));

            ports.add(DefaultPortDescription.builder()
                              .withPortNumber(portNumber)
                              .isEnabled(true)
                              .type(portType)
                              .portSpeed(portSpeed)
                              .annotations(annotations.build())
                              .build());
        });
        return ImmutableList.copyOf(ports);
    }

    public static List<HierarchicalConfiguration> parseDellPorts(HierarchicalConfiguration cfg) {
        return cfg.configurationsAt(INTERFACES);
    }
}

