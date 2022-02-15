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
import com.google.common.util.concurrent.Futures;
import gnmi.Gnmi;
import gnmi.Gnmi.GetRequest;
import gnmi.Gnmi.GetResponse;
import org.onlab.packet.ChassisId;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.gnmi.ctl.GnmiControllerImpl;
import org.onosproject.grpc.utils.AbstractGrpcHandlerBehaviour;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gnmi.Gnmi.Path;
import static gnmi.Gnmi.PathElem;
import static gnmi.Gnmi.Update;

/**
 * Class that discovers the device description and ports of a device that
 * supports the gNMI protocol and Openconfig models.
 */
public class OpenConfigGnmiDeviceDescriptionDiscovery
        extends AbstractGrpcHandlerBehaviour<GnmiClient, GnmiController>
        implements DeviceDescriptionDiscovery {

    private static final Logger log = LoggerFactory
            .getLogger(OpenConfigGnmiDeviceDescriptionDiscovery.class);

    private static final String LAST_CHANGE = "last-change";
    private static final String SDK_PORT = "sdk-port";

    private static final String UNKNOWN = "unknown";

    private GnmiController gnmiController;

    public OpenConfigGnmiDeviceDescriptionDiscovery() {
        super(GnmiController.class);
    }

    @Override
    protected boolean setupBehaviour(String opName) {
        if (!super.setupBehaviour(opName)) {
            return false;
        }

        gnmiController = handler().get(GnmiController.class);
        return true;
    }

    @Override
    public DeviceDescription discoverDeviceDetails() {
        return new DefaultDeviceDescription(
                data().deviceId().uri(),
                Device.Type.SWITCH,
                data().driver().manufacturer(),
                data().driver().hwVersion(),
                data().driver().swVersion(),
                UNKNOWN,
                new ChassisId(),
                true,
                DefaultAnnotations.builder()
                        .set(AnnotationKeys.PROTOCOL, "gNMI")
                        .build());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        if (!setupBehaviour("discoverPortDetails()")) {
            return Collections.emptyList();
        }
        log.debug("Discovering port details on device {}", handler().data().deviceId());

        final GetResponse response = Futures.getUnchecked(client.get(buildPortStateRequest()));

        final Map<String, DefaultPortDescription.Builder> ports = Maps.newHashMap();
        final Map<String, DefaultAnnotations.Builder> annotations = Maps.newHashMap();
        final Map<String, PortNumber> portIds = Maps.newHashMap();

        // Creates port descriptions with port name and port number
        response.getNotificationList()
                .forEach(notification -> {
                    notification.getUpdateList().forEach(update -> {
                        // /interfaces/interface[name=ifName]/state/...
                        final String ifName = update.getPath().getElem(1)
                                .getKeyMap().get("name");
                        if (!ports.containsKey(ifName)) {
                            ports.put(ifName, DefaultPortDescription.builder());
                            annotations.put(ifName, DefaultAnnotations.builder());
                        }
                        final DefaultPortDescription.Builder builder = ports.get(ifName);
                        final DefaultAnnotations.Builder annotationsBuilder = annotations.get(ifName);
                        parseInterfaceInfo(update, ifName, builder, annotationsBuilder, portIds);
                    });
                });

        final List<PortDescription> portDescriptionList = Lists.newArrayList();
        ports.forEach((key, value) -> {
            // For devices not providing last-change, we set it to 0
            final DefaultAnnotations.Builder annotationsBuilder = annotations.get(key);
            if (!annotationsBuilder.build().keys().contains(LAST_CHANGE)) {
                annotationsBuilder.set(LAST_CHANGE, String.valueOf(0));
            }
            /* Override port number if read port-id is enabled
               and /interfaces/interface/state/id is available */
            if (readPortId() && portIds.containsKey(key)) {
                value.withPortNumber(portIds.get(key));
            }
            DefaultAnnotations annotation = annotations.get(key).build();
            portDescriptionList.add(value.annotations(annotation).build());
        });

        return portDescriptionList;
    }

    private boolean readPortId() {
        // FIXME temporary solution will be substituted by
        //  an XML driver property when the transition to
        //  p4rt translation is completed
        return ((GnmiControllerImpl) gnmiController).readPortId();
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
     * @param update the update received
     */
    private void parseInterfaceInfo(Update update,
                                    String ifName,
                                    DefaultPortDescription.Builder builder,
                                    DefaultAnnotations.Builder annotationsBuilder,
                                    Map<String, PortNumber> portIds) {

        final Path path = update.getPath();
        final List<PathElem> elems = path.getElemList();
        final Gnmi.TypedValue val = update.getVal();
        if (elems.size() == 4) {
            /* /interfaces/interface/state/ifindex
               /interfaces/interface/state/oper-status
               /interfaces/interface/state/last-change
               /interfaces/interface/state/id */
            final String pathElemName = elems.get(3).getName();
            switch (pathElemName) {
                case "ifindex": // port number
                    builder.withPortNumber(PortNumber.portNumber(val.getUintVal(), ifName));
                    annotationsBuilder.set(SDK_PORT, String.valueOf(val.getUintVal()));
                    return;
                case "oper-status":
                    builder.isEnabled(parseOperStatus(val.getStringVal()));
                    return;
                case "last-change":
                    annotationsBuilder.set(LAST_CHANGE, String.valueOf(val.getUintVal()));
                    return;
                case "id":
                    /* Temporary stored in portIds and eventually substituted
                       when all updates have been processed. This is done because
                       there is no guarantee about the order of the updates delivery */
                    portIds.put(ifName, PortNumber.portNumber(val.getUintVal(), ifName));
                    return;
                default:
                    break;
            }
        } else if (elems.size() == 5) {
            // /interfaces/interface/ethernet/config/port-speed
            final String pathElemName = elems.get(4).getName();
            if (pathElemName.equals("port-speed")) {
                builder.portSpeed(parsePortSpeed(val.getStringVal()));
                return;
            }
        }
        log.debug("Unknown path when parsing interface info: {}", path);
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
                log.warn("Unrecognized port speed string '{}'", speed);
                return 1000;
        }
    }
}
