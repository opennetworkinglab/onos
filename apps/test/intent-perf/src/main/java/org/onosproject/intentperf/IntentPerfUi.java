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
package org.onosproject.intentperf;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.intentperf.IntentPerfCollector.Sample;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.UiView;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TimerTask;

import static java.util.Collections.synchronizedSet;

/**
 * Mechanism to stream data to the GUI.
 */
@Component(immediate = true, enabled = true)
@Service(value = IntentPerfUi.class)
public class IntentPerfUi {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected UiExtensionService uiExtensionService;

    private final Set<StreamingControl> handlers = synchronizedSet(new HashSet<>());

    private List<UiView> views = ImmutableList.of(new UiView("intentPerf", "Intent Performance"));
    private UiExtension uiExtension = new UiExtension(views, this::newHandlers,
                                                      getClass().getClassLoader());

    private List<String> headers = ImmutableList.of("One", "Two", "Three", "Four", "Five");

    private Random random = new Random();
    private TimerTask task;

    @Activate
    protected void activate() {
        uiExtensionService.register(uiExtension);
//        task = new TimerTask() {
//            @Override
//            public void run() {
//                Sample sample = new Sample(System.currentTimeMillis(), headers.size());
//                for (int i = 0; i < headers.size(); i++) {
//                    sample.data[i] = 25_000 + random.nextInt(20_000) - 5_000;
//                }
//                reportSample(sample);
//            }
//        };
//        SharedExecutors.getTimer().scheduleAtFixedRate(task, 1000, 1000);
    }

    @Deactivate
    protected void deactivate() {
        uiExtensionService.unregister(uiExtension);
    }

    /**
     * Reports a single sample of performance data.
     *
     * @param sample performance sample
     */
    public void reportSample(Sample sample) {
        synchronized (handlers) {
            handlers.forEach(h -> h.send(sample));
        }
    }

    /**
     * Sets the headers for the subsequently reported samples.
     *
     * @param headers list of headers for future samples
     */
    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    // Creates and returns session specific message handler.
    private Collection<UiMessageHandler> newHandlers() {
        return ImmutableList.of(new StreamingControl());
    }

    // UI Message handlers for turning on/off reporting to a session.
    private class StreamingControl extends UiMessageHandler {

        private boolean streamingEnabled = false;

        protected StreamingControl() {
            super(ImmutableSet.of("intentPerfStart", "intentPerfStop"));
        }

        @Override
        public void process(ObjectNode message) {
            streamingEnabled = message.path("event").asText("unknown").equals("intentPerfStart");
            if (streamingEnabled) {
                sendHeaders();
            }
        }

        private void sendHeaders() {
            ArrayNode an = mapper.createArrayNode();
            for (String header : headers) {
                an.add(header);
            }

            ObjectNode sn = mapper.createObjectNode();
            sn.set("headers", an);

            connection().sendMessage("intentPerfHeaders", 0, sn);
        }

        @Override
        public void init(UiConnection connection, ServiceDirectory directory) {
            super.init(connection, directory);
            handlers.add(this);
        }

        @Override
        public void destroy() {
            super.destroy();
            handlers.remove(this);
        }

        private void send(Sample sample) {
            if (streamingEnabled) {
                ArrayNode an = mapper.createArrayNode();
                for (double d : sample.data) {
                    an.add(d);
                }

                ObjectNode sn = mapper.createObjectNode();
                sn.put("time", sample.time);
                sn.set("data", an);

                connection().sendMessage("intentPerfSample", 0, sn);
            }
        }
    }

}
