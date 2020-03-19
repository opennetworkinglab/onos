/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.odtn.openconfig;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import gnmi.Gnmi;
import org.onosproject.drivers.gnmi.OpenConfigGnmiDeviceDescriptionDiscovery;
import org.onosproject.gnmi.api.GnmiUtils.GnmiPathBuilder;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.optical.device.OchPortHelper;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.onosproject.gnmi.api.GnmiUtils.pathToString;

/**
 * A ODTN device discovery behaviour based on gNMI and OpenConfig model.
 *
 * This behavior is based on the origin gNMI OpenConfig device description discovery
 * with additional logic to discover optical ports for this device.
 *
 * To find all optical port name and info, it queries all components with path:
 *  /components/component[name=*]
 * And it uses components with type "OPTICAL_CHANNEL" to find optical ports
 *
 */
public class GnmiTerminalDeviceDiscovery
        extends OpenConfigGnmiDeviceDescriptionDiscovery
        implements OdtnDeviceDescriptionDiscovery {

    private static final Logger log = LoggerFactory.getLogger(GnmiTerminalDeviceDiscovery.class);
    private static final String COMPONENT_TYPE_PATH_TEMPLATE =
            "/components/component[name=%s]/state/type";
    private static final String LINE_PORT_PATH_TEMPLATE =
            "/components/component[name=%s]/optical-channel/config/line-port";

    @Override
    public DeviceDescription discoverDeviceDetails() {
        return new DefaultDeviceDescription(super.discoverDeviceDetails(),
                                            Device.Type.TERMINAL_DEVICE);
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        if (!setupBehaviour("discoverPortDetails()")) {
            return Collections.emptyList();
        }

        // Get all components
        Gnmi.Path path = GnmiPathBuilder.newBuilder()
                .addElem("components")
                .addElem("component").withKeyValue("name", "*")
                .build();

        Gnmi.GetRequest req = Gnmi.GetRequest.newBuilder()
                .addPath(path)
                .setEncoding(Gnmi.Encoding.PROTO)
                .build();
        Gnmi.GetResponse resp;
        try {
            resp = client.get(req).get();
        } catch (ExecutionException | InterruptedException e) {
            log.warn("unable to get components via gNMI: {}", e.getMessage());
            return Collections.emptyList();
        }

        Multimap<String, Gnmi.Update> componentUpdates = HashMultimap.create();
        resp.getNotificationList().stream()
                .map(Gnmi.Notification::getUpdateList)
                .flatMap(List::stream)
                .forEach(u -> {
                    // Get component name
                    // /components/component[name=?]
                    Gnmi.Path p = u.getPath();
                    if (p.getElemCount() < 2) {
                        // Invalid path
                        return;
                    }
                    String name = p.getElem(1)
                            .getKeyOrDefault("name", null);

                    // Collect gNMI updates for the component.
                    // name -> a set of gNMI updates
                    if (name != null) {
                        componentUpdates.put(name, u);
                    }
                });

        Stream<PortDescription> normalPorts = super.discoverPortDetails().stream();
        Stream<PortDescription> opticalPorts = componentUpdates.keySet().stream()
                .map(name -> convertComponentToOdtnPortDesc(name, componentUpdates.get(name)))
                .filter(Objects::nonNull);
        return Streams.concat(normalPorts, opticalPorts)
                .collect(Collectors.toList());
    }

    /**
     * Converts gNMI updates to ODTN port description.
     *
     * Paths we expected per optical port component:
     * /components/component/state/type
     * /components/component/optical-channel/config/line-port
     *
     * @param name component name
     * @param updates gNMI updates
     * @return port description, null if it is not a valid component config/state
     */
    private PortDescription
        convertComponentToOdtnPortDesc(String name, Collection<Gnmi.Update> updates) {
        Map<String, Gnmi.TypedValue> pathValue = Maps.newHashMap();
        updates.forEach(u -> pathValue.put(pathToString(u.getPath()), u.getVal()));

        String componentTypePathStr =
                String.format(COMPONENT_TYPE_PATH_TEMPLATE, name);
        Gnmi.TypedValue componentType =
                pathValue.get(componentTypePathStr);

        if (componentType == null ||
                !componentType.getStringVal().equals("OPTICAL_CHANNEL")) {
            // Ignore the component which is not a optical channel type.
            return null;
        }

        Map<String, String> annotations = Maps.newHashMap();
        annotations.put(OC_NAME, name);
        annotations.put(OC_TYPE, componentType.getStringVal());

        String linePortPathStr =
                String.format(LINE_PORT_PATH_TEMPLATE, name);
        Gnmi.TypedValue linePort = pathValue.get(linePortPathStr);

        // Invalid optical port
        if (linePort == null) {
            return null;
        }

        // According to CassiniTerminalDevice class, we expected to received a string with
        // this format: port-[port id].
        // And we use "port id" from the string as the port number.
        // However, if we can't get port id from line port value, we will use
        // hash number of the port name. (According to TerminalDeviceDiscovery class)
        String linePortString = linePort.getStringVal();
        long portId = name.hashCode();
        if (linePortString.contains("-") && !linePortString.endsWith("-")) {
            try {
                portId = Long.parseUnsignedLong(linePortString.split("-")[1]);
            } catch (NumberFormatException e) {
                log.warn("Invalid line port string: {}, use {}", linePortString, portId);
            }
        }

        annotations.put(AnnotationKeys.PORT_NAME, linePortString);
        annotations.putIfAbsent(PORT_TYPE,
                OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.value());
        annotations.putIfAbsent(ONOS_PORT_INDEX, Long.toString(portId));
        annotations.putIfAbsent(CONNECTION_ID, "connection-" + portId);

        OchSignal signalId = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1);
        return OchPortHelper.ochPortDescription(
                PortNumber.portNumber(portId, name),
                true,
                OduSignalType.ODU4, // TODO: discover type via gNMI if possible
                true,
                signalId,
                DefaultAnnotations.builder().putAll(annotations).build());
    }
}
