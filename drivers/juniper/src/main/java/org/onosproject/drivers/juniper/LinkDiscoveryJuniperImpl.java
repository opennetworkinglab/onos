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

package org.onosproject.drivers.juniper;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.LinkDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
public class LinkDiscoveryJuniperImpl extends JuniperAbstractHandlerBehaviour
        implements LinkDiscovery {

    private final Logger log = getLogger(getClass());

    @Override
    public Set<LinkDescription> getLinks() {
        DeviceId localDeviceId = this.handler().data().deviceId();
        NetconfSession session = lookupNetconfSession(localDeviceId);

        String reply;
        try {
            reply = session.get(requestBuilder(REQ_LLDP_NBR_INFO));
        } catch (NetconfException e) {
            log.warn("Failed to retrieve lldp-neighbors-information for device {}", localDeviceId);
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
                    .filter(port -> linkAbs.localPortName.equals(
                            port.annotations().value(PORT_NAME))).findAny();
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
                log.warn("Device with chassis ID {} does not exist. Referenced by {}/{}",
                         linkAbs.remoteChassisId, localDeviceId, linkAbs);
                continue;
            }
            Device remoteDevice = dev.get();

            //find destination port by interface index
            Optional<Port> remotePort = deviceService.getPorts(remoteDevice.id())
                    .stream().filter(port -> {
                        if (port.number().toLong() == linkAbs.remotePortIndex) {
                            return true;
                        }
                        if (port.annotations().value(AnnotationKeys.PORT_MAC) != null
                                && linkAbs.remotePortId != null
                                && port.annotations().value(AnnotationKeys.PORT_MAC).equals(linkAbs.remotePortId)) {
                            return true;
                        }
                        if (port.annotations().value(AnnotationKeys.PORT_NAME) != null
                                && linkAbs.remotePortId != null
                                && port.annotations().value(AnnotationKeys.PORT_NAME).equals(linkAbs.remotePortId)) {
                            return true;
                        }
                        if (port.annotations().value(AnnotationKeys.PORT_NAME) != null
                                && linkAbs.remotePortDescription != null
                                && port.annotations().value(AnnotationKeys.PORT_NAME)
                                       .equals(linkAbs.remotePortDescription)) {
                            return true;
                        }
                        return false;
                    }).findAny();
            if (!remotePort.isPresent()) {
                log.warn("Port does not exist in remote device {}. Referenced by {}/{}",
                        remoteDevice.id(), localDeviceId, linkAbs);
                continue;
            }

            if (!localPort.get().isEnabled() || !remotePort.get().isEnabled()) {
                log.debug("Ports are disabled. Cannot create a link between {}/{} and {}/{}",
                        localDeviceId, localPort.get(), remoteDevice.id(), remotePort.get());
                continue;
            }

            JuniperUtils.createOneWayLinkDescription(localDeviceId,
                                                     localPort.get(),
                                                     remoteDevice.id(),
                                                     remotePort.get(),
                                                     descriptions);
        }
        return descriptions;
    }
}
