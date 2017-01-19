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

package org.onosproject.ospf.controller.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.driver.DriverService;
import org.onosproject.ospf.controller.OspfAgent;
import org.onosproject.ospf.controller.OspfController;
import org.onosproject.ospf.controller.OspfLinkListener;
import org.onosproject.ospf.controller.OspfLinkTed;
import org.onosproject.ospf.controller.OspfProcess;
import org.onosproject.ospf.controller.OspfRouter;
import org.onosproject.ospf.controller.OspfRouterListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representation of an OSPF controller implementation.
 * Serves as a one stop shop for obtaining OSPF devices and (un)register listeners on OSPF events
 */
@Component(immediate = true)
@Service
public class OspfControllerImpl implements OspfController {

    protected static final Logger log = LoggerFactory.getLogger(OspfControllerImpl.class);
    private final Controller ctrl = new Controller();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;
    protected Set<OspfRouterListener> ospfRouterListener = new HashSet<>();
    protected Set<OspfLinkListener> ospfLinkListener = Sets.newHashSet();
    protected OspfAgent agent = new InternalDeviceConfig();

    @Activate
    public void activate() {
        log.info("OSPFControllerImpl activate...!!!");
        ctrl.start(agent, driverService);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        ctrl.stop();
        log.info("Stopped");
    }


    @Override
    public void addRouterListener(OspfRouterListener listener) {
        if (!ospfRouterListener.contains(listener)) {
            this.ospfRouterListener.add(listener);
        }
    }

    @Override
    public void removeRouterListener(OspfRouterListener listener) {
        this.ospfRouterListener.remove(listener);
    }

    @Override
    public void addLinkListener(OspfLinkListener listener) {
        ospfLinkListener.add(listener);

    }

    @Override
    public void removeLinkListener(OspfLinkListener listener) {
        ospfLinkListener.remove(listener);

    }

    @Override
    public Set<OspfRouterListener> listener() {
        return ospfRouterListener;
    }

    @Override
    public Set<OspfLinkListener> linkListener() {
        return ospfLinkListener;
    }


    @Override
    public List<OspfProcess> getAllConfiguredProcesses() {
        List<OspfProcess> processes = ctrl.getAllConfiguredProcesses();
        return processes;
    }

    @Override
    public void updateConfig(JsonNode processesNode) {
        try {
            List<OspfProcess> ospfProcesses = OspfConfigUtil.processes(processesNode);
            //if there is interface details then update configuration
            if (!ospfProcesses.isEmpty() &&
                    ospfProcesses.get(0).areas() != null && !ospfProcesses.get(0).areas().isEmpty() &&
                    ospfProcesses.get(0).areas().get(0) != null &&
                    !ospfProcesses.get(0).areas().get(0).ospfInterfaceList().isEmpty()) {
                ctrl.updateConfig(ospfProcesses);
            }
        } catch (Exception e) {
            log.debug("Error::updateConfig::{}", e.getMessage());
        }
    }

    @Override
    public void deleteConfig(List<OspfProcess> processes, String attribute) {
    }

    /**
     * Notifier for internal OSPF device and link changes.
     */
    private class InternalDeviceConfig implements OspfAgent {

        @Override
        public boolean addConnectedRouter(OspfRouter ospfRouter) {
            for (OspfRouterListener l : listener()) {
                l.routerAdded(ospfRouter);
            }
            return true;
        }

        @Override
        public void removeConnectedRouter(OspfRouter ospfRouter) {
            for (OspfRouterListener l : listener()) {
                l.routerRemoved(ospfRouter);
            }
        }

        @Override
        public void addLink(OspfRouter ospfRouter, OspfLinkTed ospfLinkTed) {
            for (OspfLinkListener l : linkListener()) {
                l.addLink(ospfRouter, ospfLinkTed);
            }

        }

        @Override
        public void deleteLink(OspfRouter ospfRouter, OspfLinkTed ospfLinkTed) {
            for (OspfLinkListener l : linkListener()) {
                l.deleteLink(ospfRouter, ospfLinkTed);
            }
        }
    }
}