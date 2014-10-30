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

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.flow.DefaultTrafficSelector;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.net.intent.PointToPointIntentWithBandwidthConstraint;
import org.onlab.onos.net.resource.BandwidthResourceRequest;
import org.onlab.packet.Ethernet;
import org.onlab.rest.BaseResource;

import static org.onlab.onos.net.PortNumber.portNumber;
import static org.onlab.onos.net.flow.DefaultTrafficTreatment.builder;

/**
 * Web resource for triggering calendared intents.
 */
@Path("intent")
public class BandwidthCalendarResource extends BaseResource {

    @POST
    @Path("{src}/{dst}/{srcPort}/{dstPort}/{bandwidth}")
    public Response createIntent(@PathParam("src") String src,
                                 @PathParam("dst") String dst,
                                 @PathParam("srcPort") String srcPort,
                                 @PathParam("dstPort") String dstPort,
                                 @PathParam("bandwidth") String bandwidth) {
        // TODO: implement calls to intent framework
        IntentService service = get(IntentService.class);

        ConnectPoint srcPoint = new ConnectPoint(deviceId(src), portNumber(srcPort));
        ConnectPoint dstPoint = new ConnectPoint(deviceId(dst), portNumber(dstPort));

        TrafficSelector selector = buildTrafficSelector();
        TrafficTreatment treatment = builder().build();

        Intent intent = new PointToPointIntentWithBandwidthConstraint(
                appId(), selector, treatment,
                srcPoint, dstPoint, new BandwidthResourceRequest(Double.parseDouble(bandwidth)));
        service.submit(intent);

        return Response.ok("Yo! We got src=" + srcPoint + "; dst=" + dstPoint +
                                   "; bw=" + bandwidth + "; intent service " + service).build();
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
