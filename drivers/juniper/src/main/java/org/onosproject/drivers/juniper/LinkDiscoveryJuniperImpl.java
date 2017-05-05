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

package org.onosproject.drivers.juniper;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.LinkDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.juniper.JuniperUtils.LinkAbstraction;
import static org.onosproject.drivers.juniper.JuniperUtils.parseJuniperLldp;
import static org.onosproject.drivers.juniper.JuniperUtils.requestBuilder;
import static org.onosproject.drivers.utilities.XmlConfigParser.loadXmlString;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.drivers.juniper.JuniperUtils.REQ_LLDP_NBR_INFO;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Retrieve Links discovered by the device LLDP.
 * Tested with MX240 junos 14.2
 */
@Beta
public class LinkDiscoveryJuniperImpl extends AbstractHandlerBehaviour
        implements LinkDiscovery {

    private final Logger log = getLogger(getClass());

    @Override
    public Set<LinkDescription> getLinks() {
        DeviceId localDeviceId = this.handler().data().deviceId();
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        NetconfSession session =
                controller.getDevicesMap().get(localDeviceId).getSession();

        String reply;
        try {
            reply = session.get(requestBuilder(REQ_LLDP_NBR_INFO));
        } catch (IOException e) {
            log.warn("Failed to retrieve ports for device {}", localDeviceId);
            return ImmutableSet.of();
        }
        log.debug("Reply from device {} : {}", localDeviceId, reply);
        Set<LinkAbstraction> linkAbstractions = parseJuniperLldp(loadXmlString(reply));
        log.debug("Set of LinkAbstraction discovered {}", linkAbstractions);

        DeviceService deviceService = this.handler().get(DeviceService.class);
        Set<LinkDescription> descriptions = new HashSet<>();

        //for each lldp neighbor create two LinkDescription
        for (LinkAbstraction linkAbs : linkAbstractions) {

            //find source port by local port name
            Optional<Port> localPort = deviceService.getPorts(localDeviceId).stream()
                    .filter(port -> {
                        if (linkAbs.localPortName.equals(
                                port.annotations().value(PORT_NAME))) {
                            return true;
                        }
                        return false;
                    }).findAny();
            if (!localPort.isPresent()) {
                log.warn("Port name {} does not exist in device {}",
                         linkAbs.localPortName, localDeviceId);
                continue;
            }
            //find destination device by remote chassis id
            com.google.common.base.Optional<Device> dev = Iterables.tryFind(
                    deviceService.getAvailableDevices(),
                    input -> input.chassisId().equals(linkAbs.remoteChassisId));

            if (!dev.isPresent()) {
                log.warn("Device with chassis ID {} does not exist",
                         linkAbs.remoteChassisId);
                continue;
            }
            Device remoteDevice = dev.get();

            //find destination port by interface index
            Optional<Port> remotePort = deviceService.getPorts(remoteDevice.id())
                    .stream().filter(port -> {
                if (port.number().toLong()
                                == linkAbs.remotePortIndex) {
                    return true;
                }
                return false;
            }).findAny();
            if (!remotePort.isPresent()) {
                log.warn("Port number {} does not exist in device {}",
                         linkAbs.remotePortIndex, remoteDevice.id());
                continue;
            }

            JuniperUtils.createBiDirLinkDescription(localDeviceId,
                                                    localPort.get(),
                                                    remoteDevice.id(),
                                                    remotePort.get(),
                                                    descriptions);

        }
        return descriptions;
    }
}
