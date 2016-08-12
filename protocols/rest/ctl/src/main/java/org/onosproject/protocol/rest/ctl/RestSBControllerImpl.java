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

package org.onosproject.protocol.rest.ctl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.protocol.http.ctl.HttpSBControllerImpl;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of RestSBController.
 */
@Component(immediate = true)
@Service
public class RestSBControllerImpl extends HttpSBControllerImpl implements RestSBController {

    private static final Logger log =
            LoggerFactory.getLogger(RestSBControllerImpl.class);

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        this.getClientMap().clear();
        this.getDeviceMap().clear();
        log.info("Stopped");
    }

}
