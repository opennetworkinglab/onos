/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.workflow.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static org.onlab.util.Tools.readTreeFromStream;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.onosproject.workflow.api.WorkflowDescription.WF_ID;
import static org.onosproject.workflow.impl.WorkflowNetConfigListener.WORKFLOW_INVOKE;

@Path("workflow")
public class WorkflowRestApi extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private WorkflowService service = get(WorkflowService.class);
    private static final String OPERATION = "op";
    private static final String PARAMS = "params";


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/invoke")
    public Response invoke(InputStream stream) {
        ObjectNode ret = JsonNodeFactory.instance.objectNode();
        try {
            ObjectNode payload = readTreeFromStream(mapper(), stream);
            String operation = payload.path(OPERATION).asText();

            if (!(WORKFLOW_INVOKE.equals(operation))) {
                log.error("Operation is not matched");
                return Response.status(BAD_REQUEST)
                        .entity("Operation is not matched!").build();
            }

            JsonNode wfDescJson = payload.path(PARAMS).deepCopy();
            service.invokeWorkflow(wfDescJson);
            ret.put(WF_ID, wfDescJson.path(WF_ID).asText());

            return ok(ret).build();
        } catch (JsonProcessingException e) {
            log.error("Failed to get json ", e);
            ret.put(WF_ID, "Failed to get Json " + e.getCause());
            return Response.status(BAD_REQUEST)
                    .entity(ret).build();
        } catch (IOException e) {
            log.error("Failed to get request body ", e);
            ret.put(WF_ID, "Failed to get request body " + e.getCause());
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(ret).build();
        } catch (WorkflowException e) {
            log.error("Failed to invoke workflow ", e);
            ret.put(WF_ID, "Failed to invoke workflow " + e.getCause());
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(ret).build();

        }
    }

}
