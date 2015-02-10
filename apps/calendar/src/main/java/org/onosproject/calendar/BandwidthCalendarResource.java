/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.calendar;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onlab.rest.BaseResource;

import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.PointToPointIntent;
import org.onlab.packet.Ethernet;

import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.flow.DefaultTrafficTreatment.builder;

import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.INSTALLED;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * Web resource for triggering calendared intents.
 */
@javax.ws.rs.Path("intent")
public class BandwidthCalendarResource extends BaseResource {

    private static final Logger log = getLogger(BandwidthCalendarResource.class);
    private static final long TIMEOUT = 5; // seconds

    @javax.ws.rs.Path("/{src}/{dst}/{srcPort}/{dstPort}/{bandwidth}")
    @POST
    public Response createIntent(@PathParam("src") String src,
                                 @PathParam("dst") String dst,
                                 @PathParam("srcPort") String srcPort,
                                 @PathParam("dstPort") String dstPort,
                                 @PathParam("bandwidth") String bandwidth) {

        log.info("Receiving Create Intent request...");
        log.info("Path Constraints: Src = {} SrcPort = {} Dest = {} DestPort = {} BW = {}",
                 src, srcPort, dst, dstPort, bandwidth);

        IntentService service = get(IntentService.class);

        ConnectPoint srcPoint = new ConnectPoint(deviceId(src), portNumber(srcPort));
        ConnectPoint dstPoint = new ConnectPoint(deviceId(dst), portNumber(dstPort));

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = builder().build();

        PointToPointIntent intentP2P =
                new PointToPointIntent(appId(), selector, treatment,
                                       srcPoint, dstPoint);

        CountDownLatch latch = new CountDownLatch(1);
        InternalIntentListener listener = new InternalIntentListener(intentP2P, service, latch);
        service.addListener(listener);
        service.submit(intentP2P);
        try {
            if (latch.await(TIMEOUT, TimeUnit.SECONDS)) {
                log.info("Submitted Calendar App intent: src = {}; dst = {}; " +
                                 "srcPort = {}; dstPort = {}; intentID = {}",
                         src, dst, srcPort, dstPort, intentP2P.id());
                String reply = intentP2P.id() + " " + listener.getState() + "\n";
                return Response.ok(reply).build();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for intent {} status", intentP2P.id());
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @javax.ws.rs.Path("/cancellation/{intentId}")
    @DELETE
    public Response withdrawIntent(@PathParam("intentId") String intentId) {
        log.info("Receiving Teardown request for {}", intentId);
        IntentService service = get(IntentService.class);
        // TODO: there needs to be an app id and key here
        /*
        Intent intent = service.getIntent(IntentId.valueOf(Long.parseLong(intentId)));
        if (intent != null) {
            service.withdraw(intent);
            String reply = "ok\n";
            return Response.ok(reply).build();
        }
        */
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @javax.ws.rs.Path("/modification/{intentId}/{bandwidth}")
    @POST
    public Response modifyBandwidth(@PathParam("intentId") String intentId,
                                    @PathParam("bandwidth") String bandwidth) {

        log.info("Receiving Modify request...");
        log.info("Modify bw for intentId = {} with new bandwidth = {}", intentId, bandwidth);

        String reply = "ok\n";
        return Response.ok(reply).build();
    }

    private TrafficSelector buildTrafficSelector() {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        Short ethType = Ethernet.TYPE_IPV4;

        selectorBuilder.matchEthType(ethType);

        return selectorBuilder.build();
    }

    private DeviceId deviceId(String dpid) {
        return DeviceId.deviceId(URI.create("of:" + dpid));
    }

    protected ApplicationId appId() {
        return get(CoreService.class).registerApplication("org.onosproject.calendar");
    }

    // Auxiliary listener to wait until the given intent reaches the installed or failed states.
    private final class InternalIntentListener implements IntentListener {
        private final Intent intent;
        private final IntentService service;
        private final CountDownLatch latch;
        private IntentState state;

        private InternalIntentListener(Intent intent, IntentService service,
                                       CountDownLatch latch) {
            this.intent = intent;
            this.service = service;
            this.latch = latch;
        }

        @Override
        public void event(IntentEvent event) {
            if (event.subject().equals(intent)) {
                state = service.getIntentState(intent.key());
                if (state == INSTALLED || state == FAILED || state == WITHDRAWN) {
                    latch.countDown();
                }
                service.removeListener(this);
            }
        }

        public IntentState getState() {
            return state;
        }
    }
}
