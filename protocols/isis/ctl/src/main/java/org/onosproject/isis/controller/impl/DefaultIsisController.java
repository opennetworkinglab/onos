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
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.isis.controller.IsisController;
import org.onosproject.isis.controller.IsisProcess;
import org.onosproject.isis.controller.topology.IsisAgent;
import org.onosproject.isis.controller.topology.IsisLink;
import org.onosproject.isis.controller.topology.IsisLinkListener;
import org.onosproject.isis.controller.topology.IsisRouter;
import org.onosproject.isis.controller.topology.IsisRouterListener;
import org.onosproject.net.driver.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    protected Set<IsisRouterListener> isisRouterListener = new HashSet<>();
    protected Set<IsisLinkListener> isisLinkListener = Sets.newHashSet();
    protected IsisAgent agent = new InternalDeviceConfig();

    @Activate
    public void activate() {
        log.debug("ISISControllerImpl activate");
        controller.setAgent(agent);
    }

    @Deactivate
    public void deactivate() {
        controller.isisDeactivate();
        log.debug("ISISControllerImpl deActivate");
    }

    @Override
    public void addRouterListener(IsisRouterListener listener) {
        if (!isisRouterListener.contains(listener)) {
            this.isisRouterListener.add(listener);
        }
    }

    @Override
    public void removeRouterListener(IsisRouterListener listener) {
        this.isisRouterListener.remove(listener);
    }

    @Override
    public void addLinkListener(IsisLinkListener listener) {
        isisLinkListener.add(listener);
    }

    @Override
    public void removeLinkListener(IsisLinkListener listener) {
        isisLinkListener.remove(listener);
    }

    @Override
    public Set<IsisRouterListener> listener() {
        return isisRouterListener;
    }

    @Override
    public Set<IsisLinkListener> linkListener() {
        return isisLinkListener;
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

    /**
     * Notifier for internal ISIS device and link changes.
     */
    private class InternalDeviceConfig implements IsisAgent {
        @Override
        public boolean addConnectedRouter(IsisRouter isisRouter) {
            for (IsisRouterListener l : listener()) {
                l.routerAdded(isisRouter);
            }
            return true;
        }

        @Override
        public void removeConnectedRouter(IsisRouter isisRouter) {
            for (IsisRouterListener l : listener()) {
                l.routerRemoved(isisRouter);
            }
        }

        @Override
        public void addLink(IsisLink isisLink) {
            for (IsisLinkListener l : linkListener()) {
                l.addLink(isisLink);
            }
        }

        @Override
        public void deleteLink(IsisLink isisLink) {
            for (IsisLinkListener l : linkListener()) {
                l.deleteLink(isisLink);
            }
        }
    }
}