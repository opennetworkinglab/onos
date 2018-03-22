/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.mcast.web;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.codec.CodecService;
import org.onosproject.mcast.api.McastRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the JSON codec brokering service for route service app.
 */
@Component(immediate = true)
public class McastServiceCodecRegistrator {

    private static Logger log = LoggerFactory.getLogger(McastServiceCodecRegistrator.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CodecService codecService;

    @Activate
    public void activate() {
        codecService.registerCodec(McastRoute.class, new McastHostRouteCodec());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        codecService.unregisterCodec(McastRoute.class);
        log.info("Stopped");
    }
}
