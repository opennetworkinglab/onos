package org.onlab.onos.gui;

import org.onlab.onos.net.GreetService;
import org.onlab.rest.BaseResource;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * Simple example of a GUI JAX-RS resource.
 */
@Path("greet")
public class GreetResource extends BaseResource {

    @GET
    public Response yo(@QueryParam("name") @DefaultValue("dude") String name) {
        return Response.ok(get(GreetService.class).yo(name)).build();
    }

}
