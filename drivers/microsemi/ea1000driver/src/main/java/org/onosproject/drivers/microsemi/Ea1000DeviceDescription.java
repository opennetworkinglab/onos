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
package org.onosproject.drivers.microsemi;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.onlab.packet.ChassisId;
import org.onosproject.drivers.microsemi.yang.IetfSystemNetconfService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.system.AugmentedSysSystem;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.system.rev20160505.ietfsystemmicrosemi.systemstate.platform.AugmentedSysPlatform;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.system.rev20140806.IetfSystem;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev20130715.ietfyangtypes.DateAndTime;
import org.slf4j.Logger;

public class Ea1000DeviceDescription extends AbstractHandlerBehaviour implements DeviceDescriptionDiscovery {

    private String serialNumber = "unavailable";
    private String swVersion = "unavailable";
    private String longitudeStr = null;
    private String latitudeStr = null;
    private final Logger log = getLogger(getClass());

    public Ea1000DeviceDescription() {
        log.info("Loaded handler behaviour Ea1000DeviceDescription.");
    }

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.info("Adding description for EA1000 device");

        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        IetfSystemNetconfService ietfSystemService =
                (IetfSystemNetconfService) checkNotNull(handler().get(IetfSystemNetconfService.class));

        try {
            IetfSystem system = ietfSystemService.getIetfSystemInit(session);
            if (system != null && system.systemState() != null) {
                swVersion = system.systemState().platform().osRelease();
                AugmentedSysPlatform augmentedSysPlatform =
                        (AugmentedSysPlatform) system.systemState()
                        .platform().yangAugmentedInfo(AugmentedSysPlatform.class);
                serialNumber = augmentedSysPlatform.deviceIdentification().serialNumber();
                DateAndTime deviceDateAndTime = system.systemState().clock().currentDatetime();
                OffsetDateTime odt =
                        OffsetDateTime.parse(deviceDateAndTime.string(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                if (odt.getYear() < OffsetDateTime.now(ZoneId.of("UTC")).getYear()) {
                    OffsetDateTime nowUtc = OffsetDateTime.now(ZoneId.of("UTC"));
                    log.warn("Date on device is in the past: {}. Setting it to {}", odt.toString(), nowUtc);
                    ietfSystemService.setCurrentDatetime(nowUtc, session);
                }
            }

            if (system != null && system.system() != null) {
                AugmentedSysSystem augmentedSystem =
                        (AugmentedSysSystem) system.system().yangAugmentedInfo(AugmentedSysSystem.class);
                longitudeStr = augmentedSystem.longitude().toPlainString();
                latitudeStr = augmentedSystem.latitude().toPlainString();
            }
        } catch (NetconfException e) {
            log.error("Unable to retrieve init data from device: " + handler().data().deviceId().toString()
                    + " Error: " + e.getMessage());
            e.printStackTrace();
        }

        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = handler().data().deviceId();
        Device device = deviceService.getDevice(deviceId);
        DefaultAnnotations.Builder annotationsBuilder = DefaultAnnotations.builder();
        if (longitudeStr != null && latitudeStr != null) {
            annotationsBuilder.set(AnnotationKeys.LONGITUDE, longitudeStr)
                    .set(AnnotationKeys.LATITUDE, latitudeStr).build();
        } else {
            log.warn("Longitude and latitude could not be retrieved from device " + deviceId);
        }

        return new DefaultDeviceDescription(device.id().uri(), Device.Type.OTHER, "Microsemi", "EA1000", swVersion,
                serialNumber, new ChassisId(), annotationsBuilder.build());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {

        List<PortDescription> ports = new ArrayList<PortDescription>();

        DefaultAnnotations annotationOptics = DefaultAnnotations.builder().set(AnnotationKeys.PORT_NAME, "Optics")
                .build();
        PortDescription optics = new DefaultPortDescription(PortNumber.portNumber(0), true, Port.Type.FIBER, 1000,
                annotationOptics);
        ports.add(optics);

        DefaultAnnotations annotationHost = DefaultAnnotations.builder().set(AnnotationKeys.PORT_NAME, "Host").build();
        PortDescription host = new DefaultPortDescription(PortNumber.portNumber(1), true, Port.Type.COPPER, 1000,
                annotationHost);
        ports.add(host);

        return ports;
    }
}
