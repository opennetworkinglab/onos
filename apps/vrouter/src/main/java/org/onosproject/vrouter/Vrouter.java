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
package org.onosproject.vrouter;

import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.component.ComponentService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.List;

/**
 * Virtual router (vRouter) application.
 */
@Component(immediate = true)
public class Vrouter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String APP_NAME = "org.onosproject.vrouter";
    private static final String FPM_MANAGER = "org.onosproject.routing.fpm.FpmManager";
    private static final String FIB_INSTALLER = "org.onosproject.routing.impl.SingleSwitchFibInstaller";
    private static final String CP_REDIRECT = "org.onosproject.routing.impl.ControlPlaneRedirectManager";
    private static final String DIRECT_HOST_MGR = "org.onosproject.routing.impl.DirectHostManager";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ComponentService componentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ComponentConfigService componentConfigService;

    /**
     * vRouter will push flows to the switches when receiving routes by enabling
     * FIB installer.
     * <p>
     * It should be turned off when vRouter is deployed in a scenario where
     * other components that pushes the routes.
     */
    @Property(name = "fibInstallerEnabled", boolValue = true,
            label = "Enable single switch fib installer; default is true")
    private boolean fibInstallerEnabled = true;

    private ApplicationId appId;

    private List<String> baseComponents = Lists.newArrayList(FPM_MANAGER, CP_REDIRECT, DIRECT_HOST_MGR);

    @Activate
    protected void activate(ComponentContext context) {
        appId = coreService.registerApplication(APP_NAME);
        componentConfigService.registerProperties(getClass());

        componentConfigService.preSetProperty(
                "org.onosproject.incubator.store.routing.impl.RouteStoreImpl",
                "distributed", "true");

        baseComponents.forEach(name -> componentService.activate(appId, name));
        modified(context);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.preSetProperty(
                "org.onosproject.incubator.store.routing.impl.RouteStoreImpl",
                "distributed", "false");

        log.info("Stopped");
    }

    @Modified
    private void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        if (properties == null) {
            return;
        }

        Boolean newFibInstallerEnabled = Tools.isPropertyEnabled(properties, "fibInstalledEnabled");
        if (newFibInstallerEnabled == null) {
            log.info("fibInstallerEnabled is not configured, " +
                    "using current value of {}", fibInstallerEnabled);
        } else {
            fibInstallerEnabled = newFibInstallerEnabled;
            log.info("Configured. fibInstallerEnabled set to {}, ",
                    fibInstallerEnabled ? "enabled" : "disabled");
        }

        if (fibInstallerEnabled) {
            componentService.activate(appId, FIB_INSTALLER);
        } else {
            componentService.deactivate(appId, FIB_INSTALLER);
        }
    }
}
