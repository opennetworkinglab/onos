package org.onosproject.store.cluster.messaging.impl;

import com.google.common.base.Strings;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.netty.NettyMessaging;
import org.onosproject.cluster.ClusterDefinitionService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.store.cluster.messaging.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty based MessagingService.
 */
@Component(immediate = true, enabled = true)
@Service
public class NettyMessagingManager extends NettyMessaging {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final short MIN_KS_LENGTH = 6;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterDefinitionService clusterDefinitionService;

    @Activate
    public void activate() throws Exception {
        ControllerNode localNode = clusterDefinitionService.localNode();
        getTLSParameters();
        super.start(new Endpoint(localNode.ip(), localNode.tcpPort()));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() throws Exception {
        super.stop();
        log.info("Stopped");
    }

    private void getTLSParameters() {
        String tempString = System.getProperty("enableNettyTLS");
        enableNettyTLS = Strings.isNullOrEmpty(tempString) ? TLS_DISABLED : Boolean.parseBoolean(tempString);
        log.info("enableNettyTLS = {}", enableNettyTLS);
        if (enableNettyTLS) {
            ksLocation = System.getProperty("javax.net.ssl.keyStore");
            if (Strings.isNullOrEmpty(ksLocation)) {
                enableNettyTLS = TLS_DISABLED;
                return;
            }
            tsLocation = System.getProperty("javax.net.ssl.trustStore");
            if (Strings.isNullOrEmpty(tsLocation)) {
                enableNettyTLS = TLS_DISABLED;
                return;
            }
            ksPwd = System.getProperty("javax.net.ssl.keyStorePassword").toCharArray();
            if (MIN_KS_LENGTH > ksPwd.length) {
                enableNettyTLS = TLS_DISABLED;
                return;
            }
            tsPwd = System.getProperty("javax.net.ssl.trustStorePassword").toCharArray();
            if (MIN_KS_LENGTH > tsPwd.length) {
                enableNettyTLS = TLS_DISABLED;
                return;
            }
        }
    }
}
