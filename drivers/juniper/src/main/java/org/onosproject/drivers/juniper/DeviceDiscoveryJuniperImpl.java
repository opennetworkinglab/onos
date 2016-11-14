/*
 * Copyright 2016 Open Networking Laboratory
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
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.juniper.JuniperUtils.FAILED_CFG;
import static org.onosproject.drivers.juniper.JuniperUtils.REQ_IF_INFO;
import static org.onosproject.drivers.juniper.JuniperUtils.REQ_MAC_ADD_INFO;
import static org.onosproject.drivers.juniper.JuniperUtils.REQ_SYS_INFO;
import static org.onosproject.drivers.juniper.JuniperUtils.requestBuilder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Retrieve the Device information and ports via NETCONF for Juniper Router.
 * Tested with MX240 junos 14.2
 */
@Beta
public class DeviceDiscoveryJuniperImpl extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    public final org.slf4j.Logger log = getLogger(getClass());

    @Override
    public DeviceDescription discoverDeviceDetails() {
        DeviceId deviceId = handler().data().deviceId();
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();
        String sysInfo;
        String chassis;
        try {
            sysInfo = session.get(requestBuilder(REQ_SYS_INFO));
            chassis = session.get(requestBuilder(REQ_MAC_ADD_INFO));
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException(FAILED_CFG, e));
        }
        DeviceDescription description =
                JuniperUtils.parseJuniperDescription(deviceId, XmlConfigParser.
                        loadXml(new ByteArrayInputStream(sysInfo.getBytes())), chassis);
        log.debug("Device  description {}", description);
        return description;
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        String reply;
        try {
            reply = session.get(requestBuilder(REQ_IF_INFO));
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException(FAILED_CFG, e));
        }
        List<PortDescription> descriptions =
                JuniperUtils.parseJuniperPorts(XmlConfigParser.
                        loadXml(new ByteArrayInputStream(reply.getBytes())));
        log.debug("Discovered ports {}", descriptions);
        return descriptions;
    }
}
