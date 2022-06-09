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

import com.google.common.collect.Sets;
import io.netty.channel.ChannelHandlerContext;
import com.eclipsesource.json.JsonObject;
import org.onlab.packet.IpPrefix;
import org.onosproject.artemis.ArtemisMonitor;
import org.onosproject.artemis.ArtemisPacketProcessor;
import org.onosproject.artemis.impl.objects.ArtemisMessage;
import org.onosproject.event.EventDeliveryService;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component(immediate = true, service = ArtemisMonitor.class)
public class ArtemisMonitorImpl implements ArtemisMonitor {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final Class<ArtemisConfig> CONFIG_CLASS = ArtemisConfig.class;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private NetworkConfigService configService;

    /* Variables */
    private Set<PrefixHandler> prefixHandlers = Sets.newHashSet();
    private InternalPacketProcessor packetProcessor = new InternalPacketProcessor();

    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    @Activate
    protected void activate() {
        configService.addListener(configListener);
        log.info("Artemis Monitor Service Started");
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        prefixHandlers.forEach(PrefixHandler::stopPrefixMonitors);
        prefixHandlers.clear();

        log.info("Artemis Monitor Service Stopped");
    }

    private class InternalPacketProcessor implements ArtemisPacketProcessor {

        @Override
        public void processMoasPacket(ArtemisMessage msg, ChannelHandlerContext ctx) {

        }

        @Override
        public void processMonitorPacket(JsonObject msg) {
            // TODO: in future maybe store the BGP Update message and propagate it to the cluster instead of Events
            eventDispatcher.post(new ArtemisEvent(ArtemisEvent.Type.BGPUPDATE_ADDED, msg));
        }
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
                        prefixHandlers.forEach(PrefixHandler::stopPrefixMonitors);
                        prefixHandlers.clear();
                    }
                    break;
                }
                case CONFIG_UPDATED:
                case CONFIG_ADDED: {
                    if (event.configClass() == CONFIG_CLASS) {
                        event.config().ifPresent(config -> {
                            ArtemisConfig artemisConfig = (ArtemisConfig) config;
                            Set<IpPrefix> ipPrefixes = artemisConfig.prefixesToMonitor();
                            Map<String, Set<String>> monitors = artemisConfig.activeMonitors();

                            prefixHandlers.forEach(PrefixHandler::stopPrefixMonitors);
                            prefixHandlers.clear();
                            prefixHandlers = ipPrefixes.stream()
                                    .map(prefix -> new PrefixHandler(prefix, monitors, packetProcessor))
                                    .collect(Collectors.toSet());

                            prefixHandlers.forEach(PrefixHandler::startPrefixMonitors);
                        });
                    }
                    break;
                }
                default:
                    break;
            }
        }

    }
}
