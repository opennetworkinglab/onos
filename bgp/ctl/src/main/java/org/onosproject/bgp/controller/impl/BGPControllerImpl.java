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

package org.onosproject.bgp.controller.impl;

import static org.onlab.util.Tools.groupedThreads;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.bgp.controller.BGPCfg;
import org.onosproject.bgp.controller.BGPController;
import org.onosproject.bgp.controller.BGPId;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Service
public class BGPControllerImpl implements BGPController {

    private static final Logger log = LoggerFactory.getLogger(BGPControllerImpl.class);

    private final ExecutorService executorMsgs = Executors.newFixedThreadPool(32,
                                                                              groupedThreads("onos/bgp",
                                                                                      "event-stats-%d"));

    private final ExecutorService executorBarrier = Executors.newFixedThreadPool(4,
                                                                                 groupedThreads("onos/bgp",
                                                                                         "event-barrier-%d"));

    final Controller ctrl = new Controller(this);

    private BGPConfig bgpconfig = new BGPConfig();

    @Activate
    public void activate() {
        this.ctrl.start();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        // Close all connected peers
        this.ctrl.stop();
        log.info("Stopped");
    }

    @Override
    public void writeMsg(BGPId bgpId, BGPMessage msg) {
        // TODO: Send message
    }

    @Override
    public void processBGPPacket(BGPId bgpId, BGPMessage msg) {

        switch (msg.getType()) {
        case OPEN:
            // TODO: Process Open message
            break;
        case KEEP_ALIVE:
            // TODO: Process keepalive message
            break;
        case NOTIFICATION:
            // TODO: Process notificatoin message
            break;
        case UPDATE:
            // TODO: Process update message
            break;
        default:
            // TODO: Process other message
            break;
        }
    }

    /**
     * Get controller instance.
     *
     * @return ctrl the controller.
     */
    public Controller getController() {
        return ctrl;
    }

    @Override
    public BGPCfg getConfig() {
        return this.bgpconfig;
    }
}