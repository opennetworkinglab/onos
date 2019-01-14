/**
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.kafkaintegration.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.onosproject.event.Event;
import org.onosproject.kafkaintegration.api.EventConversionService;
import org.onosproject.kafkaintegration.api.dto.OnosEvent;
import org.onosproject.kafkaintegration.converter.EventConverter;
import org.onosproject.kafkaintegration.converter.DeviceEventConverter;
import org.onosproject.kafkaintegration.converter.LinkEventConverter;
import org.onosproject.kafkaintegration.converter.HostEventConverter;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.host.HostEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.DEVICE;
import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.LINK;
import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.HOST;

/**
 * Implementation of Event Conversion Service.
 *
 */
@Component(immediate = true, service = EventConversionService.class)
public class EventConversionManager implements EventConversionService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private EventConverter deviceEventConverter;
    private EventConverter linkEventConverter;
    private EventConverter hostEventConverter;

    @Activate
    protected void activate() {
        deviceEventConverter = new DeviceEventConverter();
        linkEventConverter = new LinkEventConverter();
        hostEventConverter = new HostEventConverter();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public OnosEvent convertEvent(Event<?, ?> event) {
        if (event instanceof DeviceEvent) {
            return new OnosEvent(DEVICE, deviceEventConverter.convertToProtoMessage(event));
        } else if (event instanceof LinkEvent) {
            return new OnosEvent(LINK, linkEventConverter.convertToProtoMessage(event));
        } else if (event instanceof HostEvent) {
            return new OnosEvent(HOST, hostEventConverter.convertToProtoMessage(event));
        } else {
            throw new IllegalArgumentException("Unsupported event type");
        }
    }
}
