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

package org.onosproject.drivers.netconf;

import com.google.common.base.Preconditions;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of controller config which allows to get and set controllers
 * through the Netconf protocol.
 */
public class NetconfControllerConfig extends AbstractHandlerBehaviour
        implements ControllerConfig {

    private final Logger log = getLogger(NetconfControllerConfig.class);


    @Override
    public List<ControllerInfo> getControllers() {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        DeviceId deviceId = handler.data().deviceId();
        Preconditions.checkNotNull(controller, "Netconf controller is null");
        List<ControllerInfo> controllers = new ArrayList<>();
        if (mastershipService.isLocalMaster(deviceId)) {
            try {
                String reply = controller.getNetconfDevice(deviceId).getSession().
                        getConfig(DatastoreId.RUNNING);
                log.debug("Reply XML {}", reply);
                controllers.addAll(XmlConfigParser.parseStreamControllers(XmlConfigParser.
                        loadXml(new ByteArrayInputStream(reply.getBytes(StandardCharsets.UTF_8)))));
            } catch (NetconfException e) {
                log.error("Cannot communicate with device {} ", deviceId, e);
            }
        } else {
            log.warn("I'm not master for {} please use master, {} to execute command",
                     deviceId,
                     mastershipService.getMasterFor(deviceId));
        }
        return controllers;
    }

    @Override
    public void setControllers(List<ControllerInfo> controllers) {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        DeviceId deviceId = handler.data().deviceId();
        Preconditions.checkNotNull(controller, "Netconf controller is null");
        MastershipService mastershipService = handler.get(MastershipService.class);
        if (mastershipService.isLocalMaster(deviceId)) {
            try {
                NetconfDevice device = controller.getNetconfDevice(deviceId);
                String config = null;

                try {
                    String reply = device.getSession().getConfig(DatastoreId.RUNNING);
                    log.info("reply XML {}", reply);
                    config = XmlConfigParser.createControllersConfig(
                            XmlConfigParser.loadXml(getClass().getResourceAsStream("controllers.xml")),
                            XmlConfigParser.loadXml(
                                    new ByteArrayInputStream(reply.getBytes(StandardCharsets.UTF_8))),
                            "running", "merge", "create", controllers
                    );
                } catch (NetconfException e) {
                    log.error("Cannot comunicate to device {} , exception {}", deviceId, e.getMessage());
                    return;
                }
                device.getSession().editConfig(config.substring(config.indexOf("-->") + 3));
            } catch (NullPointerException e) {
                log.warn("No NETCONF device with requested parameters " + e);
                throw new NullPointerException("No NETCONF device with requested parameters " + e);
            } catch (NetconfException e) {
                log.error("Cannot comunicate to device {} , exception {}", deviceId, e.getMessage());
            }
        } else {
            log.warn("I'm not master for {} please use master, {} to execute command",
                     deviceId,
                     mastershipService.getMasterFor(deviceId));
        }
    }

    //TODO maybe put method getNetconfClientService like in ovsdb if we need it

}


