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
package org.onosproject.flow.manager.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.flow.forwarder.BgpFlowForwarderService;
import org.onosproject.flow.forwarder.impl.BgpFlowForwarderImpl;
import org.onosproject.flow.manager.BgpFlowService;
import org.onosproject.flowapi.ExtFlowContainer;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of Bgp flow Service.
 */
@Component(immediate = true)
@Service
public class BgpFlowManager implements BgpFlowService {

    private final Logger log = getLogger(getClass());
    private static final String APP_ID = "org.onosproject.app.bgpflow";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    protected ApplicationId appId;
    private BgpFlowForwarderService bgpFlowForwarderService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        bgpFlowForwarderService = new BgpFlowForwarderImpl(appId, flowObjectiveService, deviceService, driverService);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean onBgpFlowCreated(ExtFlowContainer container) {
        return bgpFlowForwarderService.installForwardingRule(container);
    }

    @Override
    public boolean onBgpFlowDeleted(ExtFlowContainer container) {
        return bgpFlowForwarderService.unInstallForwardingRule(container);
    }
}
