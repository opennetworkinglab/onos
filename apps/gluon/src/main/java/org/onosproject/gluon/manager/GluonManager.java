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
package org.onosproject.gluon.manager;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.core.CoreService;
import org.onosproject.gluon.rsc.GluonConfig;
import org.onosproject.gluon.rsc.GluonServer;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gluon Shim Application.
 */
@Component(immediate = true)
public class GluonManager {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String APP_ID = "org.onosproject.gluon";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry configRegistry;


    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY,
                              GluonConfig.class, "gluon") {
                @Override
                public GluonConfig createConfig() {
                    return new GluonConfig();
                }
            };

    private static Map<String, GluonServer> serverMap = new LinkedHashMap<>();

    @Activate
    public void activate() {
        coreService.registerApplication(APP_ID);
        configRegistry.registerConfigFactory(configFactory);
        log.info("Gluon app Started");
    }

    @Deactivate
    public void deactivate() {
        configRegistry.unregisterConfigFactory(configFactory);
        log.info("Gluon app Stopped");
    }

    /**
     * Creating gluon server object.
     *
     * @param etcduri         server url
     * @param targetProtonKey server key type, default net-l3vpn
     * @param mode            server mode start or stop
     * @param version         running server version
     */
    public static void createServer(String etcduri, String targetProtonKey,
                                    String mode, String version) {
        new GluonServer(etcduri, targetProtonKey, mode, version);
    }

    /**
     * Deleting gluon server from server list.
     *
     * @param etcduri server url
     */
    public static void deleteServer(String etcduri) {
        for (Map.Entry<String, GluonServer> server : serverMap.entrySet()) {
            if (etcduri.equals(server.getKey())) {
                serverMap.remove(etcduri);
                return;
            }
        }
    }

    /**
     * Add server into map.
     *
     * @param etcduri     server url
     * @param gluonObject store server object
     */
    public static void addServer(String etcduri, GluonServer gluonObject) {
        serverMap.put(etcduri, gluonObject);
    }

    /**
     * Returns serverMap size.
     *
     * @return total number of servers
     */
    public static int getTotalServers() {
        return serverMap.size();
    }

    /**
     * Returns all server IPs.
     *
     * @return serverMap
     */
    public static Map<String, GluonServer> getAllServersIP() {
        return serverMap;
    }


}