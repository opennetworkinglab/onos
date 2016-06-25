/**
 * Copyright 2016 Open Networking Laboratory
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.kafkaintegration.listener;

import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.LINK;

import org.onosproject.event.ListenerService;
import org.onosproject.kafkaintegration.impl.Dispatcher;
import org.onosproject.kafkaintegration.api.dto.OnosEvent.Type;
import org.onosproject.kafkaintegration.converter.ConversionFactory;
import org.onosproject.kafkaintegration.converter.EventConverter;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;

import com.google.protobuf.GeneratedMessage;

/**
 * Listens for ONOS Link Events.
 *
 */
final class LinkEventsListener implements OnosEventListener {

    private boolean listenerRunning = false;

    private InnerListener listener = null;

    // Exists to defeat instantiation
    private LinkEventsListener() {
    }

    private static class SingletonHolder {
        private static final LinkEventsListener INSTANCE =
                new LinkEventsListener();
    }

    /**
     * Returns a static reference to the Listener Factory.
     *
     * @return singleton object
     */
    public static LinkEventsListener getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public void startListener(Type e, ListenerService<?, ?> service) {
        if (!listenerRunning) {
            listener = new InnerListener();
            LinkService linkService = (LinkService) service;
            linkService.addListener(listener);
            listenerRunning = true;
        }
    }

    private class InnerListener implements LinkListener {

        @Override
        public void event(LinkEvent arg0) {

            // Convert the event to GPB format
            ConversionFactory conversionFactory =
                    ConversionFactory.getInstance();
            EventConverter converter = conversionFactory.getConverter(LINK);
            GeneratedMessage message = converter.convertToProtoMessage(arg0);

            // Call Dispatcher and publish event
            Dispatcher.getInstance().publish(LINK, message);
        }
    }

    @Override
    public void stopListener(Type e, ListenerService<?, ?> service) {
        if (listenerRunning) {
            LinkService linkService = (LinkService) service;
            linkService.removeListener(listener);
            listenerRunning = false;
        }
    }
}
