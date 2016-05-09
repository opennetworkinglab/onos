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
package org.onosproject.isis.controller.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.isis.controller.IsisController;
import org.onosproject.isis.controller.IsisProcess;
import org.onosproject.isis.controller.topology.IsisRouterListener;
import org.onosproject.net.driver.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Represents ISIS controller implementation.
 */
@Component(immediate = true)
@Service
public class DefaultIsisController implements IsisController {

    protected static final Logger log = LoggerFactory.getLogger(DefaultIsisController.class);
    private final Controller controller = new Controller();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Activate
    public void activate() {
        log.debug("ISISControllerImpl activate");
    }

    @Deactivate
    public void deactivate() {
        controller.isisDeactivate();
        log.debug("ISISControllerImpl deActivate");
    }

    @Override
    public List<IsisProcess> allConfiguredProcesses() {
        List<IsisProcess> processes = controller.getAllConfiguredProcesses();
        return processes;
    }

    @Override
    public void updateConfig(JsonNode jsonNode) {
        log.debug("updateConfig::IsisList::processes::{}", jsonNode);
        try {
            controller.updateConfig(jsonNode);
        } catch (Exception e) {
            log.debug("Error::updateConfig::{}", e.getMessage());
        }
    }

    @Override
    public void addRouterListener(IsisRouterListener isisRouterListener) {
        log.debug("IsisControllerImpl::addRouterListener...");
    }

    @Override
    public void removeRouterListener(IsisRouterListener isisRouterListener) {
        log.debug("IsisControllerImpl::removeRouterListener...");
    }
}