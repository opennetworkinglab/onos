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
package org.onosproject.lisp.ctl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;

import org.onosproject.core.CoreService;
import org.onosproject.lisp.LispController;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LISP controller initiation class.
 */
@Component(immediate = true)
@Service
public class LispControllerImpl implements LispController {

    private static final String APP_ID = "org.onosproject.lisp-base";

    private static final Logger log =
            LoggerFactory.getLogger(LispControllerImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private final LispControllerBootstrap bootstrap = new LispControllerBootstrap();

    @Activate
    public void activate() {
        coreService.registerApplication(APP_ID);
        bootstrap.start();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        bootstrap.stop();
        log.info("Stopped");
    }
}
