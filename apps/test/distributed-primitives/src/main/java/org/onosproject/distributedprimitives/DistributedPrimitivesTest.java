/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.distributedprimitives;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Simple application to test distributed primitives.
 */
@Component(immediate = true)
public class DistributedPrimitivesTest {

    private final Logger log = getLogger(getClass());

    private static final String APP_NAME = "org.onosproject.distributedprimitives";
    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


    @Activate
    protected void activate() {

        log.info("Distributed-Primitives-test app started");
        appId = coreService.registerApplication(APP_NAME);
    }

    @Deactivate
    protected void deactivate() {

        log.info("Distributed-Primitives-test app Stopped");
    }
}
