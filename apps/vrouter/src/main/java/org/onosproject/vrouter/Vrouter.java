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
package org.onosproject.vrouter;

import com.google.common.collect.Lists;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.component.ComponentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Virtual router (vRouter) application.
 */
@Component(immediate = true)
public class Vrouter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String APP_NAME = "org.onosproject.vrouter";
    private static final String DIRECT_HOST_MGR = "org.onosproject.routing.impl.DirectHostManager";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ComponentService componentService;

    private ApplicationId appId;

    private List<String> baseComponents = Lists.newArrayList(DIRECT_HOST_MGR);

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        baseComponents.forEach(name -> componentService.activate(appId, name));

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }
}
