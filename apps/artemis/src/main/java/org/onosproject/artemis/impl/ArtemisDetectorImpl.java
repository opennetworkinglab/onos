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

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;
import org.onlab.packet.IpPrefix;
import org.onosproject.artemis.ArtemisDetector;
import org.onosproject.artemis.ArtemisEventListener;
import org.onosproject.artemis.ArtemisService;
import org.onosproject.core.CoreService;
import org.onosproject.event.EventDeliveryService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service = ArtemisDetector.class)
public class ArtemisDetectorImpl implements ArtemisDetector {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /* Services */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ArtemisService artemisService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EventDeliveryService eventDispatcher;

    private final ArtemisEventListener artemisEventListener = this::handleArtemisEvent;

    @Activate
    protected void activate() {
        artemisService.addListener(artemisEventListener);
        log.info("Artemis Detector Service Started");
    }

    @Deactivate
    protected void deactivate() {
        artemisService.removeListener(artemisEventListener);
        log.info("Artemis Detector Service Stopped");
    }

    /**
     * Handles a artemis event.
     *
     * @param event the artemis event
     */
    void handleArtemisEvent(ArtemisEvent event) {
        // If an instance was deactivated, check whether we need to roll back the upgrade.
        if (event.type().equals(ArtemisEvent.Type.BGPUPDATE_ADDED)) {
            JsonObject take = (JsonObject) event.subject();

            log.info("Received information about monitored prefix " + take.toString());
            artemisService.getConfig().ifPresent(config ->
                    config.monitoredPrefixes().forEach(artemisPrefix -> {
                        IpPrefix prefix = artemisPrefix.prefix(), receivedPrefix;

                        receivedPrefix = IpPrefix.valueOf(take.get("prefix").asString());

                        if (prefix.contains(receivedPrefix)) {
                            JsonArray path = take.get("path").asArray();

                            int state = artemisPrefix.checkPath(path);
                            if (state >= 100) {
                                log.info("BGP Hijack detected; pushing prefix for hijack Deaggregation");
                                eventDispatcher.post(new ArtemisEvent(ArtemisEvent.Type.HIJACK_ADDED,
                                        receivedPrefix));
                            } else {
                                log.info("BGP Update is legit");
                            }
                        }
                    })
            );
        }
    }

}
