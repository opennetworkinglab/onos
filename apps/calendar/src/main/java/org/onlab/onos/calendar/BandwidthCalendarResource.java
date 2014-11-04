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
package org.onlab.onos.calendar;

import java.net.URI;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.rest.BaseResource;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.packet.Ethernet;
import static org.onlab.onos.net.PortNumber.portNumber;
import static org.onlab.onos.net.flow.DefaultTrafficTreatment.builder;

import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;

/**
 * Web resource for triggering calendared intents.
 */
@javax.ws.rs.Path("intent")
public class BandwidthCalendarResource extends BaseResource {

    private static final Logger log = getLogger(BandwidthCalendarResource.class);

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
        service.submit(intentP2P);
        log.info("Submitted Calendar App intent: src = " + src + "dest = " + dst
                + "srcPort = " + srcPort + "destPort" + dstPort + "intentID = " + intentP2P.id().toString());
        String reply =  intentP2P.id().toString() + "\n";

        return Response.ok(reply).build();
    }

    @javax.ws.rs.Path("/cancellation/{intentId}")
    @DELETE
    public Response withdrawIntent(@PathParam("intentId") String intentId) {

        log.info("Receiving Teardown request...");
        log.info("Withdraw intentId = {} ", intentId);

        String reply =  "ok\n";
        return Response.ok(reply).build();
    }

    @javax.ws.rs.Path("/modification/{intentId}/{bandwidth}")
    @POST
    public Response modifyBandwidth(@PathParam("intentId") String intentId,
                                 @PathParam("bandwidth") String bandwidth) {

        log.info("Receiving Modify request...");
        log.info("Modify bw for intentId = {} with new bandwidth = {}", intentId, bandwidth);

        String reply =  "ok\n";
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
        return get(CoreService.class).registerApplication("org.onlab.onos.calendar");
    }
}
