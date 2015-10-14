/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.cluster.messaging.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.nio.service.IOLoopMessaging;
import org.onosproject.cluster.ClusterMetadataService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IOLoop based MessagingService.
 */
@Component(immediate = true, enabled = false)
@Service
public class IOLoopMessagingManager extends IOLoopMessaging {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataService clusterMetadataService;

    @Activate
    public void activate() throws Exception {
        ControllerNode localNode = clusterMetadataService.getLocalNode();
        super.start(new Endpoint(localNode.ip(), localNode.tcpPort()));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() throws Exception {
        super.stop();
        log.info("Stopped");
    }
}
