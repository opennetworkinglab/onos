package org.onlab.onos.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.onlab.rest.BaseResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Rest API for demos.
 */
@Path("intents")
public class DemoResource extends BaseResource {


    @POST
    @Path("setup")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setup(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode cfg = mapper.readTree(input);
        if (!cfg.has("type")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Expected type field containing either mesh or random.").build();
        }


        DemoAPI.InstallType type = DemoAPI.InstallType.valueOf(
                cfg.get("type").asText().toUpperCase());
        DemoAPI demo = get(DemoAPI.class);
        demo.setup(type, Optional.ofNullable(cfg.get("runParams")));

        return Response.ok(mapper.createObjectNode().toString()).build();
    }

    @GET
    @Path("teardown")
    @Produces(MediaType.APPLICATION_JSON)
    public Response tearDown() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        DemoAPI demo = get(DemoAPI.class);
        demo.tearDown();
        return Response.ok(mapper.createObjectNode().toString()).build();
    }

}
