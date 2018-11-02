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

package org.onosproject.drivers.gnmi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import gnmi.Gnmi;
import gnmi.Gnmi.GetRequest;
import gnmi.Gnmi.GetResponse;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static gnmi.Gnmi.Path;
import static gnmi.Gnmi.PathElem;
import static gnmi.Gnmi.Update;

/**
 * Class that discovers the device description and ports of a device that
 * supports the gNMI protocol and Openconfig models.
 */
public class OpenConfigGnmiDeviceDescriptionDiscovery
        extends AbstractGnmiHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private static final int REQUEST_TIMEOUT_SECONDS = 5;

    private static final Logger log = LoggerFactory
            .getLogger(OpenConfigGnmiDeviceDescriptionDiscovery.class);

    @Override
    public DeviceDescription discoverDeviceDetails() {
        return null;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        if (!setupBehaviour()) {
            return Collections.emptyList();
        }
        log.debug("Discovering port details on device {}", handler().data().deviceId());

        GetResponse response;
        try {
            response = client.get(buildPortStateRequest())
                    .get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.warn("Unable to discover ports from {}: {}", deviceId, e.getMessage());
            log.debug("{}", e);
            return Collections.emptyList();
        }

        Map<String, DefaultPortDescription.Builder> ports = Maps.newHashMap();
        Map<String, DefaultAnnotations.Builder> annotations = Maps.newHashMap();

        // Creates port descriptions with port name and port number
        response.getNotificationList()
                .stream()
                .flatMap(notification -> notification.getUpdateList().stream())
                .forEach(update -> {
                    // /interfaces/interface[name=ifName]/state/...
                    String ifName = update.getPath().getElem(1).getKeyMap().get("name");
                    if (!ports.containsKey(ifName)) {
                        ports.put(ifName, DefaultPortDescription.builder());
                        annotations.put(ifName, DefaultAnnotations.builder());
                    }


                    DefaultPortDescription.Builder builder = ports.get(ifName);
                    DefaultAnnotations.Builder annotationsBuilder = annotations.get(ifName);
                    parseInterfaceInfo(update, ifName, builder, annotationsBuilder);
                });

        List<PortDescription> portDescriptionList = Lists.newArrayList();
        ports.forEach((key, value) -> {
            DefaultAnnotations annotation = annotations.get(key).build();
            portDescriptionList.add(value.annotations(annotation).build());
        });
        return portDescriptionList;
    }

    private GetRequest buildPortStateRequest() {
        Path path = Path.newBuilder()
                .addElem(PathElem.newBuilder().setName("interfaces").build())
                .addElem(PathElem.newBuilder().setName("interface").putKey("name", "...").build())
                .addElem(PathElem.newBuilder().setName("state").build())
                .build();
        return GetRequest.newBuilder()
                .addPath(path)
                .setType(GetRequest.DataType.ALL)
                .setEncoding(Gnmi.Encoding.PROTO)
                .build();
    }

    /**
     * Parses the interface information.
     *
     * @param update           the update received
     */
    private void parseInterfaceInfo(Update update,
                                    String ifName,
                                    DefaultPortDescription.Builder builder,
                                    DefaultAnnotations.Builder annotationsBuilder) {


        Path path = update.getPath();
        List<PathElem> elems = path.getElemList();
        Gnmi.TypedValue val = update.getVal();
        if (elems.size() == 4) {
            // /interfaces/interface/state/ifindex
            // /interfaces/interface/state/oper-status
            String pathElemName = elems.get(3).getName();
            switch (pathElemName) {
                case "ifindex": // port number
                    builder.withPortNumber(PortNumber.portNumber(val.getUintVal(), ifName));
                    break;
                case "oper-status":
                    builder.isEnabled(parseOperStatus(val.getStringVal()));
                    break;
                default:
                    String valueString = val.toByteString().toString(Charset.defaultCharset()).trim();
                    if (!valueString.isEmpty()) {
                        annotationsBuilder.set(pathElemName, valueString);
                    }
                    log.debug("Unknown path: {}", path);
                    break;
            }
        }
        if (elems.size() == 5) {
            // /interfaces/interface/ethernet/config/port-speed
            String pathElemName = elems.get(4).getName();
            switch (pathElemName) {
                case "port-speed":
                    builder.portSpeed(parsePortSpeed(val.getStringVal()));
                    break;
                default:
                    String valueString = val.toByteString().toString(Charset.defaultCharset()).trim();
                    if (!valueString.isEmpty()) {
                        annotationsBuilder.set(pathElemName, valueString);
                    }
                    log.debug("Unknown path: {}", path);
                    break;
            }
        }
    }

    private boolean parseOperStatus(String operStatus) {
        switch (operStatus) {
            case "UP":
                return true;
            case "DOWN":
            default:
                return false;
        }
    }

    private long parsePortSpeed(String speed) {
        log.debug("Speed from config {}", speed);
        switch (speed) {
            case "SPEED_10MB":
                return 10;
            case "SPEED_100MB":
                return 100;
            case "SPEED_1GB":
                return 1000;
            case "SPEED_10GB":
                return 10000;
            case "SPEED_25GB":
                return 25000;
            case "SPEED_40GB":
                return 40000;
            case "SPEED_50GB":
                return 50000;
            case "SPEED_100GB":
                return 100000;
            default:
                return 1000;
        }
    }
}
