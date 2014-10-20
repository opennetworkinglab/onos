package org.onlab.onos.calendar;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.rest.BaseResource;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.onlab.onos.net.PortNumber.portNumber;

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

        return Response.ok("Yo! We got src=" + srcPoint + "; dst=" + dstPoint +
                                   "; bw=" + bandwidth + "; intent service " + service).build();
    }

    private DeviceId deviceId(String dpid) {
        return DeviceId.deviceId(URI.create("of:" + dpid));
    }

}
