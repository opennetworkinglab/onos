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
package org.onosproject.artemis.impl;

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.routing.bgp.BgpInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;

/**
 * Artemis Component.
 */
@Component(immediate = true)
@Service
public class ArtemisManager implements ArtemisService {
    private static final String ARTEMIS_APP_ID = "org.onosproject.artemis";
    private static final Class<ArtemisConfig> CONFIG_CLASS = ArtemisConfig.class;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private BgpInfoService bgpInfoService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    public static boolean logging = false;

    private Set<PrefixHandler> prefixHandlers = Sets.newHashSet();
    private Deaggregator deaggr;
    private Timer timer;

    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

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
        log.info("Artemis Started");
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        registry.unregisterConfigFactory(artemisConfigFactory);
        prefixHandlers.forEach(PrefixHandler::stopPrefixMonitors);
        log.info("Artemis Stopped");
    }

    /**
     * Helper function to start and stop monitors on configuration changes.
     */
    private void setUpConfiguration() {
        ArtemisConfig config = configService.getConfig(appId, CONFIG_CLASS);

        if (config == null) {
            log.warn("No artemis config available!");
            return;
        }

        final Set<ArtemisConfig.ArtemisPrefixes> prefixes = config.monitoredPrefixes();
        final Integer frequency = config.detectionFrequency();
        final Map<String, Set<String>> monitors = config.activeMonitors();

        Set<PrefixHandler> toRemove = Sets.newHashSet(prefixHandlers);

        for (ArtemisConfig.ArtemisPrefixes curr : prefixes) {
            final Optional<PrefixHandler> handler = prefixHandlers
                    .stream()
                    .filter(prefixHandler -> prefixHandler.getPrefix().equals(curr.prefix()))
                    .findFirst();

            if (handler.isPresent()) {
                PrefixHandler oldHandler = handler.get();
                oldHandler.changeMonitors(monitors);

                // remove the ones we are going to keep from toRemove list
                toRemove.remove(oldHandler);
            } else {
                // Add new handler
                PrefixHandler newHandler = new PrefixHandler(curr.prefix(), monitors);
                newHandler.startPrefixMonitors();
                prefixHandlers.add(newHandler);
            }
        }

        // stop and remove old monitors that do not exist on new configuration
        toRemove.forEach(PrefixHandler::stopPrefixMonitors);
        prefixHandlers.removeAll(toRemove);

        // new timer task with updated bgp speakers
        deaggr = new Deaggregator(bgpInfoService);
        deaggr.setPrefixes(prefixes);

        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(deaggr, frequency, frequency);
    }

    @Override
    public void setLogger(boolean value) {
        logging = value;
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
                case CONFIG_REGISTERED:
                    break;
                case CONFIG_UNREGISTERED:
                    break;
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                case CONFIG_REMOVED:
                    if (event.configClass() == CONFIG_CLASS) {
                        setUpConfiguration();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
