package org.onosproject.store.cluster.messaging.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.nio.service.IOLoopMessaging;
import org.onosproject.cluster.ClusterDefinitionService;
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
    protected ClusterDefinitionService clusterDefinitionService;

    @Activate
    public void activate() throws Exception {
        ControllerNode localNode = clusterDefinitionService.localNode();
        super.start(new Endpoint(localNode.ip(), localNode.tcpPort()));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() throws Exception {
        super.stop();
        log.info("Stopped");
    }
}
