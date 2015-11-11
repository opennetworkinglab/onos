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

import com.google.common.base.Strings;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.netty.NettyMessaging;
import org.onosproject.cluster.ClusterMetadataService;
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
    protected ClusterMetadataService clusterMetadataService;

    @Activate
    public void activate() throws Exception {
        ControllerNode localNode = clusterMetadataService.getLocalNode();
        getTlsParameters();
        super.start(clusterMetadataService.getClusterMetadata().getName().hashCode(),
                    new Endpoint(localNode.ip(), localNode.tcpPort()));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() throws Exception {
        super.stop();
        log.info("Stopped");
    }

    private void getTlsParameters() {
        String tempString = System.getProperty("enableNettyTLS");
        enableNettyTls = Strings.isNullOrEmpty(tempString) ? TLS_DISABLED : Boolean.parseBoolean(tempString);
        log.info("enableNettyTLS = {}", enableNettyTls);
        if (enableNettyTls) {
            ksLocation = System.getProperty("javax.net.ssl.keyStore");
            if (Strings.isNullOrEmpty(ksLocation)) {
                enableNettyTls = TLS_DISABLED;
                return;
            }
            tsLocation = System.getProperty("javax.net.ssl.trustStore");
            if (Strings.isNullOrEmpty(tsLocation)) {
                enableNettyTls = TLS_DISABLED;
                return;
            }
            ksPwd = System.getProperty("javax.net.ssl.keyStorePassword").toCharArray();
            if (MIN_KS_LENGTH > ksPwd.length) {
                enableNettyTls = TLS_DISABLED;
                return;
            }
            tsPwd = System.getProperty("javax.net.ssl.trustStorePassword").toCharArray();
            if (MIN_KS_LENGTH > tsPwd.length) {
                enableNettyTls = TLS_DISABLED;
                return;
            }
        }
    }
}
