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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.component.ComponentService;
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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
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
