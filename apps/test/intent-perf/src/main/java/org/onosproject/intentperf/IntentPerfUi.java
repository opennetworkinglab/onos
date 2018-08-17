/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.intentperf.IntentPerfCollector.Sample;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.UiView;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.synchronizedSet;
import static org.onosproject.ui.UiView.Category.OTHER;

/**
 * Mechanism to stream data to the GUI.
 */
@Component(immediate = true, service = IntentPerfUi.class)
public class IntentPerfUi {

    private static final String INTENT_PERF_START = "intentPerfStart";
    private static final String INTENT_PERF_STOP = "intentPerfStop";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected UiExtensionService uiExtensionService;

    private final Set<StreamingControl> handlers = synchronizedSet(new HashSet<>());

    private List<UiView> views = ImmutableList.of(
            new UiView(OTHER, "intentPerf", "Intent Performance")
    );

    private UiExtension uiExtension =
            new UiExtension.Builder(getClass().getClassLoader(), views)
                .messageHandlerFactory(this::newHandlers)
                .build();

    private IntentPerfCollector collector;

    @Activate
    protected void activate() {
        uiExtensionService.register(uiExtension);
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
     * Binds the sample collector.
     *
     * @param collector list of headers for future samples
     */
    public void setCollector(IntentPerfCollector collector) {
        this.collector = collector;
    }

    // Creates and returns session specific message handler.
    private Collection<UiMessageHandler> newHandlers() {
        return ImmutableList.of(new StreamingControl());
    }


    // UI Message handlers for turning on/off reporting to a session.
    private class StreamingControl extends UiMessageHandler {

        private boolean streamingEnabled = false;

        @Override
        protected Collection<RequestHandler> createRequestHandlers() {
            return ImmutableSet.of(
                    new IntentPerfStart(),
                    new IntentPerfStop()
            );
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
                connection().sendMessage("intentPerfSample", sampleNode(sample));
            }
        }


        private ObjectNode sampleNode(Sample sample) {
            ObjectNode sampleNode = objectNode();
            ArrayNode an = arrayNode();
            sampleNode.put("time", sample.time);
            sampleNode.set("data", an);

            for (double d : sample.data) {
                an.add(d);
            }
            return sampleNode;
        }

        // ======================================================================

        private final class IntentPerfStart extends RequestHandler {

            private IntentPerfStart() {
                super(INTENT_PERF_START);
            }

            @Override
            public void process(ObjectNode payload) {
                streamingEnabled = true;
                sendInitData();
            }

            private void sendInitData() {
                ObjectNode rootNode = MAPPER.createObjectNode();
                ArrayNode an = MAPPER.createArrayNode();
                ArrayNode sn = MAPPER.createArrayNode();
                rootNode.set("headers", an);
                rootNode.set("samples", sn);

                collector.getSampleHeaders().forEach(an::add);
                collector.getSamples().forEach(s -> sn.add(sampleNode(s)));
                sendMessage("intentPerfInit", rootNode);
            }
        }

        // ======================================================================

        private final class IntentPerfStop extends RequestHandler {

            private IntentPerfStop() {
                super(INTENT_PERF_STOP);
            }

            @Override
            public void process(ObjectNode payload) {
                streamingEnabled = false;
            }
        }

    }

}
