/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.protocol.restconf.server.restconfmanager;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.glassfish.jersey.server.ChunkedOutput;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.protocol.restconf.server.api.RestconfException;
import org.onosproject.protocol.restconf.server.api.RestconfServiceBroker;
import org.onosproject.restconf.api.RestconfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

/**
 * Implementation of the RestconfServiceBroker interface.
 */
@Component(immediate = false)
@Service
public class RestconfBrokerImpl implements RestconfServiceBroker {

    private static final String APP_NAME = "org.onosproject.protocols.restconfserver";
    private static final String CONFIG_KEY = "restconfCfg";
    private static final String DYN_CONFIG_MODE = "true";
    private static final String RESTCONF_ROOT = "/onos/restconf";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected org.onosproject.protocol.restconf.server.api.RestconfService restconfYms;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RestconfService restconfDynConfig;

    private ApplicationId appId;
    private boolean useDynamicConfig = false;

    private final NetworkConfigListener cfgLister = new InternalConfigListener();
    private final ConfigFactory<ApplicationId, RestconfConfig> factory =
            new ConfigFactory<ApplicationId, RestconfConfig>(APP_SUBJECT_FACTORY,
                                                             RestconfConfig.class,
                                                             CONFIG_KEY,
                                                             false) {
                @Override
                public RestconfConfig createConfig() {
                    return new RestconfConfig();
                }
            };

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgLister);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgLister);
        cfgService.unregisterConfigFactory(factory);
        log.info("Stopped");
    }

    @Override
    public ObjectNode runGetOperationOnDataResource(String uri)
            throws RestconfException {
        return useDynamicConfig ?
                restconfDynConfig.runGetOperationOnDataResource(uri) :
                restconfYms.runGetOperationOnDataResource(uri);
    }


    @Override
    public void runPostOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException {
        if (useDynamicConfig) {
            restconfDynConfig.runPostOperationOnDataResource(uri, rootNode);
        } else {
            restconfYms.runPostOperationOnDataResource(uri, rootNode);
        }
    }

    @Override
    public void runPutOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException {
        if (useDynamicConfig) {
            restconfDynConfig.runPutOperationOnDataResource(uri, rootNode);
        } else {
            restconfYms.runPutOperationOnDataResource(uri, rootNode);
        }
    }

    @Override
    public void runDeleteOperationOnDataResource(String uri)
            throws RestconfException {
        if (useDynamicConfig) {
            restconfDynConfig.runDeleteOperationOnDataResource(uri);
        } else {
            restconfYms.runDeleteOperationOnDataResource(uri);
        }
    }

    @Override
    public void runPatchOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException {
        if (useDynamicConfig) {
            restconfDynConfig.runPatchOperationOnDataResource(uri, rootNode);
        } else {
            restconfYms.runPatchOperationOnDataResource(uri, rootNode);
        }
    }

    @Override
    public String getRestconfRootPath() {
        return RESTCONF_ROOT;
    }

    @Override
    public void subscribeEventStream(String streamId,
                                     ChunkedOutput<String> output)
            throws RestconfException {
        if (useDynamicConfig) {
            restconfDynConfig.subscribeEventStream(streamId, output);
        } else {
            restconfYms.subscribeEventStream(streamId, output);
        }
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            try {
                useDynamicConfig = cfgService.getConfig(appId, RestconfConfig.class)
                        .useDynamicConfig().equals(DYN_CONFIG_MODE);
            } catch (ConfigException e) {
                log.error("Configuration error {}", e);
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(RestconfConfig.class) &&
                    (event.type() == CONFIG_ADDED ||
                            event.type() == CONFIG_UPDATED);
        }
    }
}
