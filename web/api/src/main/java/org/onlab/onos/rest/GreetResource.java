package org.onlab.onos.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.onos.GreetService;
import org.onlab.rest.BaseResource;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Simple example on how to write a testable JAX-RS resource.
 */
@Path("greet")
public class GreetResource extends BaseResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response yo(@QueryParam("name") @DefaultValue("dude") String name) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("greeting", get(GreetService.class).yo(name));
        return Response.ok(root.toString()).build();
    }

}
