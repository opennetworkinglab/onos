/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.yang.web;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.yang.YangLiveCompilerService;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModel;
import org.onosproject.yang.model.YangModule;
import org.onosproject.yang.runtime.YangModelRegistry;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Yang files upload resource.
 */
@Path("models")
public class YangWebResource extends AbstractWebResource {

    private final Logger log = getLogger(getClass());
    private YangModelRegistry modelRegistry = getService(YangModelRegistry.class);

    /**
     * Compiles and registers the given yang files.
     *
     * @param modelId model identifier
     * @param stream  YANG, ZIP or JAR file
     * @return 200 OK
     * @throws IOException when fails to generate a file
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@QueryParam("modelId") @DefaultValue("org.onosproject.model.unknown") String modelId,
                           @FormDataParam("file") InputStream stream) throws IOException {
        YangLiveCompilerService compiler = get(YangLiveCompilerService.class);
        ApplicationAdminService appService = get(ApplicationAdminService.class);
        modelId = getValidModelId(modelId);
        appService.install(compiler.compileYangFiles(modelId, stream));
        appService.activate(appService.getId(modelId));
        return Response.ok().build();
    }

    /**
     * Returns the valid model id by removing the special character with
     * underscore.
     *
     * @param id user given model id
     * @return model id
     * @throws IllegalArgumentException if user defined model id does not
     *                                  contain at least a alphanumeric character
     */
    public static String getValidModelId(String id) throws
            IllegalArgumentException {
        // checking whether modelId contains the alphanumeric character or not.
        if (id.matches(".*[A-Za-z0-9].*")) {
            // replacing special characters with '_'
            id = id.replaceAll("[\\s\\/:*?\"\\[\\]<>|$@!#%&(){}'`;.,-]", "_");
            // remove leading and trailing underscore
            id = id.replaceAll("^_+|_+$", "");
            // replacing the consecutive underscores '_' to single _
            id = id.replaceAll("_+", "_");
            return id;
        } else {
            throw new IllegalArgumentException("Invalid model id " + id);
        }
    }

    /**
     * Returns all models registered with YANG runtime. If the operation is
     * successful, the JSON presentation of the resource plus HTTP status
     * code "200 OK" is returned.Otherwise,
     * HTTP error status code "400 Bad Request" is returned.
     *
     * @onos.rsModel YangModelsGet
     * @return HTTP response
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getModels() {

        modelRegistry = getService(YangModelRegistry.class);
        ObjectNode result = mapper().createObjectNode();
        Set<YangModel> models = modelRegistry.getModels();
        ArrayNode ids = result.putArray("model_ids");
        for (YangModel m : models) {
            ids.add(m.getYangModelId());
        }
        return Response.ok(result.toString()).build();
    }

    /**
     * Returns all modules registered with YANG runtime under given model
     * identifier.If the operation is successful, the JSON presentation of the
     * resource plus HTTP status code "200 OK" is returned. Otherwise,
     * HTTP error status code "400 Bad Request" is returned.
     *
     * @onos.rsModel YangModulesGet
     * @param id for model
     * @return HTTP response
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response getModules(@PathParam("id") String
                                       id) {
        modelRegistry = getService(YangModelRegistry.class);
        ObjectNode result = mapper().createObjectNode();
        YangModel model = modelRegistry.getModel(id);
        if (model == null) {
            return Response.status(NOT_FOUND).build();
        }
        Set<YangModule> modules = model.getYangModules();
        ArrayNode ids = result.putArray(id);
        for (YangModule m : modules) {
            ids.add(m.getYangModuleId().moduleName() + "@" + m
                    .getYangModuleId().revision());
        }
        return Response.ok(result).build();
    }

    /**
     * Returns module registered with YANG runtime with given module
     * identifier.
     * If the operation is successful, the JSON presentation of the resource
     * plus HTTP status code "200 OK" is returned. Otherwise,
     * HTTP error status code "400 Bad Request" is returned.
     *
     * @param n for module name
     * @param r for module revision
     * @return HTTP response
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}@{revision}")
    public String getModule(@PathParam("name") String n,
                            @PathParam("revision") String r) {

        modelRegistry = getService(YangModelRegistry.class);
        YangModule m = modelRegistry.getModule(new DefaultYangModuleId(n, r));
        if (m == null) {
            return Response.status(NOT_FOUND).build().toString();
        }
        String x;
        try {
            x = IOUtils.toString(m.getYangSource(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("ERROR: handleModuleGetRequest", e.getMessage());
            log.debug("Exception in handleModuleGetRequest:", e);
            return e.getMessage();
        }
        return x;
    }
}
