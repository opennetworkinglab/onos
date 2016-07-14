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
package org.onosproject.xosclient.impl;

import com.google.common.base.Strings;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.xosclient.api.VtnPortApi;
import org.onosproject.xosclient.api.VtnServiceApi;
import org.onosproject.xosclient.api.XosAccess;
import org.onosproject.xosclient.api.XosAccessConfig;
import org.onosproject.xosclient.api.XosClientService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides interactions with XOS.
 */
@Component(immediate = true)
@Service
public class XosClient implements XosClientService {

    protected final Logger log = getLogger(getClass());

    private static final String VTN_SERVICE_URL = "vtnServiceBaseUrl";
    private static final String DEFAULT_VTN_SERVICE_URL = "/api/service/vtn/services/";

    private static final String VTN_PORT_URL = "vtnPortBaseUrl";
    private static final String DEFAULT_VTN_PORT_URL = "/api/service/vtn/ports/";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Property(name = VTN_SERVICE_URL, value = DEFAULT_VTN_SERVICE_URL,
            label = "XOS VTN service API base url")
    private String vtnServiceUrl = DEFAULT_VTN_SERVICE_URL;

    @Property(name = VTN_PORT_URL, value = DEFAULT_VTN_PORT_URL,
            label = "XOS VTN port API base url")
    private String vtnPortUrl = DEFAULT_VTN_PORT_URL;

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, XosAccessConfig.class, "xosclient") {
                @Override
                public XosAccessConfig createConfig() {
                    return new XosAccessConfig();
                }
            };

    private final NetworkConfigListener configListener = new InternalConfigListener();

    private ApplicationId appId;
    private XosAccess access = null;

    private VtnServiceApi vtnServiceApi = null;
    private VtnPortApi vtnPortApi = null;

    /*
     * adds more XOS service APIs below.
     */

    @Activate
    protected void activate(ComponentContext context) {
        appId = coreService.registerApplication("org.onosproject.xosclient");

        componentConfigService.registerProperties(getClass());
        modified(context);

        configRegistry.registerConfigFactory(configFactory);
        configRegistry.addListener(configListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configRegistry.unregisterConfigFactory(configFactory);
        configRegistry.removeListener(configListener);

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        String updatedUrl;

        updatedUrl = Tools.get(properties, VTN_SERVICE_URL);
        if (!Strings.isNullOrEmpty(updatedUrl) && !updatedUrl.equals(vtnServiceUrl)) {
            vtnServiceUrl = updatedUrl;
            vtnServiceApi = new DefaultVtnServiceApi(vtnServiceUrl, access);
        }

       updatedUrl = Tools.get(properties, VTN_PORT_URL);
        if (!Strings.isNullOrEmpty(updatedUrl) && !updatedUrl.equals(vtnPortUrl)) {
            vtnPortUrl = updatedUrl;
            vtnPortApi = new DefaultVtnPortApi(vtnPortUrl, access);
        }

        log.info("Modified");
    }

    @Override
    public XosAccess access() {
        return access;
    }

    @Override
    public synchronized XosClient getClient(XosAccess access) {
        checkNotNull(access);

        if (!Objects.equals(this.access, access)) {
            // TODO do authentication before return
            this.access = access;

            vtnServiceApi = new DefaultVtnServiceApi(vtnServiceUrl, access);
            vtnPortApi = new DefaultVtnPortApi(vtnPortUrl, access);
        }
        return this;
    }

    @Override
    public VtnServiceApi vtnService() {
        checkNotNull(vtnServiceApi, "VtnServiceApi is null");
        return vtnServiceApi;
    }

    @Override
    public VtnPortApi vtnPort() {
        checkNotNull(vtnPortApi, "VtnPortApi is null");
        return vtnPortApi;
    }

    /*
     * adds more XOS service APIs below.
     */

    private void readConfiguration() {
        XosAccessConfig config = configRegistry.getConfig(appId, XosAccessConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }
        getClient(config.xosAccess());
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(XosAccessConfig.class)) {
                return;
            }

            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    log.info("Network configuration changed");
                    readConfiguration();
                    break;
                default:
                    break;
            }
        }
    }
}
