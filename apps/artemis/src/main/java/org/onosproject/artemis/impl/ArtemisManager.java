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

package org.onosproject.artemis.impl;

import org.onosproject.artemis.ArtemisEventListener;
import org.onosproject.artemis.ArtemisService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Component(immediate = true, service = ArtemisService.class)
public class ArtemisManager
        extends AbstractListenerManager<ArtemisEvent, ArtemisEventListener>
        implements ArtemisService {

    private static final String ARTEMIS_APP_ID = "org.onosproject.artemis";
    private static final Class<ArtemisConfig> CONFIG_CLASS = ArtemisConfig.class;
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();
    /* Services */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    /* Variables */
    private ApplicationId appId;
    private ArtemisConfig artemisConfig;


    /* Config */
    private ConfigFactory<ApplicationId, ArtemisConfig> artemisConfigFactory =
            new ConfigFactory<ApplicationId, ArtemisConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, ArtemisConfig.class, "artemis") {
                @Override
                public ArtemisConfig createConfig() {
                    return new ArtemisConfig();
                }
            };

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(ARTEMIS_APP_ID);
        configService.addListener(configListener);
        registry.registerConfigFactory(artemisConfigFactory);

        eventDispatcher.addSink(ArtemisEvent.class, listenerRegistry);

        log.info("Artemis Service Started");
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        registry.unregisterConfigFactory(artemisConfigFactory);

        eventDispatcher.removeSink(ArtemisEvent.class);

        log.info("Artemis Service Stopped");
    }

    @Override
    public Optional<ArtemisConfig> getConfig() {
        return Optional.ofNullable(artemisConfig);
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
                case CONFIG_REGISTERED:
                case CONFIG_UNREGISTERED: {
                    break;
                }
                case CONFIG_REMOVED: {
                    if (event.configClass() == CONFIG_CLASS) {
                        artemisConfig = null;
                    }
                    break;
                }
                case CONFIG_UPDATED:
                case CONFIG_ADDED: {
                    if (event.configClass() == CONFIG_CLASS) {
                        event.config().ifPresent(config -> artemisConfig = (ArtemisConfig) config);
                    }
                    break;
                }
                default:
                    break;
            }
        }

    }

}
