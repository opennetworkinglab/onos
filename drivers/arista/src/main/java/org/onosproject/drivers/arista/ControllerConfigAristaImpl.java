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

package org.onosproject.drivers.arista;

import com.google.common.collect.Lists;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sets, gets and removes the openflow controller configuration from a Arista Rest device.
 */
public class ControllerConfigAristaImpl extends AbstractHandlerBehaviour implements ControllerConfig {

    private static final String CONFIGURE_TERMINAL = "configure";
    private static final String OPENFLOW_CMD = "openflow";
    private static final String REMOVE_CONTROLLER_CMD = "no controller tcp:%s:%d";
    private static final int MAX_CONTROLLERS = 8;

    private final Logger log = getLogger(getClass());

    @Override
    public List<ControllerInfo> getControllers() {
        throw new UnsupportedOperationException("get controllers configuration is not supported");
    }

    @Override
    public void setControllers(List<ControllerInfo> controllers) {
        throw new UnsupportedOperationException("set controllers configuration is not supported");
    }

    @Override
    public void removeControllers(List<ControllerInfo> controllers) {
        List<String> cmds = Lists.newArrayList();

        cmds.add(CONFIGURE_TERMINAL);
        cmds.add(OPENFLOW_CMD);
        controllers.stream().limit(MAX_CONTROLLERS).forEach(c -> cmds
                .add(String.format(REMOVE_CONTROLLER_CMD, c.ip().toString(), c.port())));

        AristaUtils.retrieveCommandResult(handler(), cmds);
    }
}
