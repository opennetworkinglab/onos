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

import org.onlab.packet.Ethernet;
import org.onlab.rest.BaseResource;
import org.onlab.util.Tools;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.TwoWayP2PIntent;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LatencyConstraint;
import org.onosproject.net.resource.Bandwidth;
import org.slf4j.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.flow.DefaultTrafficTreatment.builder;
import static org.onosproject.net.intent.IntentState.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Web resource for triggering calendared intents.
 */
@javax.ws.rs.Path("intent")
public class BandwidthCalendarResource extends BaseResource {

    private static final Logger log = getLogger(BandwidthCalendarResource.class);
    private static final long TIMEOUT = 10; // seconds

    private static final String INVALID_PARAMETER = "INVALID_PARAMETER\n";
    private static final String OPERATION_INSTALLED = "INSTALLED\n";
    private static final String OPERATION_FAILED = "FAILED\n";
    private static final String OPERATION_WITHDRAWN = "WITHDRAWN\n";

    /**
     * Setup a bi-directional path with constraints between switch to switch.
     * Switch is identified by DPID.
     *
     * @param src the path source (DPID or hostID)
     * @param dst the path destination (DPID or hostID)
     * @param srcPort the source port (-1 if src/dest is a host)
     * @param dstPort the destination port (-1 if src/dest is a host)
     * @param bandwidth the bandwidth (mbps) requirement for the path
     * @param latency the latency (micro sec) requirement for the path
     * @return intent key if successful,
     *         server error message or "FAILED" if failed to create or submit intent
     */
    @javax.ws.rs.Path("/{src}/{dst}/{srcPort}/{dstPort}/{bandwidth}/{latency}")
    @POST
    // TODO could allow applications to provide optional key
    // ... if you do, you will need to change from LongKeys to StringKeys
    public Response setupPath(@PathParam("src") String src,
                              @PathParam("dst") String dst,
                              @PathParam("srcPort") String srcPort,
                              @PathParam("dstPort") String dstPort,
                              @PathParam("bandwidth") String bandwidth,
                              @PathParam("latency") String latency) {

        log.info("Path Constraints: Src = {} SrcPort = {} Dest = {} DestPort = {} " +
                          "BW = {} latency = {}",
                 src, srcPort, dst, dstPort, bandwidth, latency);

        if (src == null || dst == null || srcPort == null || dstPort == null) {
            return Response.ok(INVALID_PARAMETER).build();
        }

        Long bandwidthL = 0L;
        Long latencyL = 0L;
        try {
            bandwidthL = Long.parseLong(bandwidth, 10);
            latencyL = Long.parseLong(latency, 10);
        } catch (Exception e) {
            return Response.ok(INVALID_PARAMETER).build();
        }

        Intent intent = createIntent(null, src, dst, srcPort, dstPort, bandwidthL, latencyL);
        try {
            if (submitIntent(intent)) {
                return Response.ok(intent.key() + "\n").build();
            } else {
                return Response.ok(OPERATION_FAILED).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Modify a bi-directional path's bandwidth.
     *
     * @param intentKey the path intent key
     * @param src the path source (DPID or hostID)
     * @param dst the path destination (DPID or hostID)
     * @param srcPort the source port (-1 if src/dest is a host)
     * @param dstPort the destination port (-1 if src/dest is a host)
     * @param bandwidth the bandwidth (mbps) requirement for the path
     * @return Intent state, "INSTALLED", if successful,
     *         server error message or "FAILED" if failed to modify any direction intent
     */
    @javax.ws.rs.Path("/{intentKey}/{src}/{dst}/{srcPort}/{dstPort}/{bandwidth}")
    @PUT
    public Response modifyBandwidth(@PathParam("intentKey") String intentKey,
                                    @PathParam("src") String src,
                                    @PathParam("dst") String dst,
                                    @PathParam("srcPort") String srcPort,
                                    @PathParam("dstPort") String dstPort,
                                    @PathParam("bandwidth") String bandwidth) {

        log.info("Modify bw for intentKey = {}; src = {}; dst = {};" +
                         "srcPort = {}; dstPort = {}; with new bandwidth = {}",
                 intentKey, src, dst, srcPort, dstPort, bandwidth);

        if (src == null || dst == null || srcPort == null || dstPort == null) {
            return Response.ok(INVALID_PARAMETER).build();
        }

        Long bandwidthL = 0L;
        try {
            bandwidthL = Long.parseLong(bandwidth, 10);
        } catch (Exception e) {
            return Response.ok(INVALID_PARAMETER).build();
        }

        IntentService service = get(IntentService.class);
        Intent originalIntent
                = service.getIntent(Key.of(Tools.fromHex(intentKey.replace("0x", "")), appId()));

        if (originalIntent == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // get the latency constraint from the original intent
        Long latencyL = 0L;
        if (originalIntent instanceof ConnectivityIntent) {
            ConnectivityIntent connectivityIntent = (ConnectivityIntent) originalIntent;
            for (Constraint constraint : connectivityIntent.constraints()) {
                if (constraint instanceof LatencyConstraint) {
                    latencyL = ((LatencyConstraint) constraint).latency().get(ChronoUnit.MICROS);
                }
            }
        }

        Intent newIntent = createIntent(originalIntent.key(), src, dst,
                                        srcPort, dstPort, bandwidthL, latencyL);
        try {
            if (submitIntent(newIntent)) {
                return Response.ok(OPERATION_INSTALLED).build();
            } else {
                return Response.ok(OPERATION_FAILED).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Create an Intent for a bidirectional path with constraints.
     *
     * @param key optional intent key
     * @param src the path source (DPID or hostID)
     * @param dst the path destination (DPID or hostID)
     * @param srcPort the source port (-1 if src/dest is a host)
     * @param dstPort the destination port (-1 if src/dest is a host)
     * @param bandwidth the bandwidth (mbps) requirement for the path
     * @param latency the latency (micro sec) requirement for the path
     * @return the appropriate intent
     */
    private Intent createIntent(Key key,
                                String src,
                                String dst,
                                String srcPort,
                                String dstPort,
                                Long bandwidth,
                                Long latency) {

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = builder().build();

        final Constraint constraintBandwidth =
                new BandwidthConstraint(Bandwidth.mbps(bandwidth));
        final Constraint constraintLatency =
                new LatencyConstraint(Duration.of(latency, ChronoUnit.MICROS));
        final List<Constraint> constraints = new LinkedList<>();

        constraints.add(constraintBandwidth);
        constraints.add(constraintLatency);

        if (srcPort.equals("-1")) {
            HostId srcPoint = HostId.hostId(src);
            HostId dstPoint = HostId.hostId(dst);
            return new HostToHostIntent(appId(), key, srcPoint, dstPoint,
                                        selector, treatment, constraints);
        } else {
            ConnectPoint srcPoint = new ConnectPoint(deviceId(src), portNumber(srcPort));
            ConnectPoint dstPoint = new ConnectPoint(deviceId(dst), portNumber(dstPort));
            return new TwoWayP2PIntent(appId(), key, srcPoint, dstPoint,
                                       selector, treatment, constraints);
        }
    }


    /**
     * Synchronously submits an intent to the Intent Service.
     *
     * @param intent intent to submit
     * @return true if operation succeed, false otherwise
     */
    private boolean submitIntent(Intent intent)
        throws InterruptedException {
        IntentService service = get(IntentService.class);

        CountDownLatch latch = new CountDownLatch(1);
        InternalIntentListener listener = new InternalIntentListener(intent, service, latch);
        service.addListener(listener);
        service.submit(intent);
        log.info("Submitted Calendar App intent and waiting: {}", intent);
        if (latch.await(TIMEOUT, TimeUnit.SECONDS) &&
                listener.getState() == INSTALLED) {
            return true;
        }
        return false;
    }

    /**
     * Remove a bi-directional path with created intent key.
     *
     * @param intentKey the string key for the intent to remove
     * @return Intent state, "WITHDRAWN", if successful,
     *         server error message or FAILED" if any direction intent remove failed
     */
    @javax.ws.rs.Path("/{intentKey}")
    @DELETE
    public Response removePath(@PathParam("intentKey") String intentKey) {

        log.info("Receiving tear down request for {}", intentKey);

        if (intentKey == null) {
            return Response.ok(INVALID_PARAMETER).build();
        }

        IntentService service = get(IntentService.class);
        Intent intent = service.getIntent(Key.of(Tools.fromHex(intentKey.replace("0x", "")), appId()));

        if (intent == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            if (withdrawIntent(intent)) {
                return Response.ok(OPERATION_WITHDRAWN).build();
            } else {
                return Response.ok(OPERATION_FAILED).build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Synchronously withdraws an intent to the Intent Service.
     *
     * @param intent intent to submit
     * @return true if operation succeed, false otherwise
     */
    private boolean withdrawIntent(Intent intent)
            throws InterruptedException {
        IntentService service = get(IntentService.class);

        CountDownLatch latch = new CountDownLatch(1);
        InternalIntentListener listener = new InternalIntentListener(intent, service, latch);
        service.addListener(listener);
        service.withdraw(intent);
        log.info("Withdrawing intent and waiting: {}", intent);
        if (latch.await(TIMEOUT, TimeUnit.SECONDS) &&
                listener.getState() == WITHDRAWN) {
            return true;
        }
        return false;
    }


    private static TrafficSelector buildTrafficSelector() {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        Short ethType = Ethernet.TYPE_IPV4;

        selectorBuilder.matchEthType(ethType);

        return selectorBuilder.build();
    }

    private static DeviceId deviceId(String dpid) {
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
                    service.removeListener(this);
                }
            }
        }

        public IntentState getState() {
            return state;
        }
    }
}
