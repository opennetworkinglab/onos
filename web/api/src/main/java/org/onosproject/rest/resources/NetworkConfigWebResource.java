/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.SubjectFactory;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.onlab.util.Tools.emptyIsNotFound;
import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Manage network configurations.
 */
@Path("network/configuration")
public class NetworkConfigWebResource extends AbstractWebResource {

    //FIX ME not found Multi status error code 207 in jaxrs Response Status.
    private static final int  MULTI_STATUS_RESPONE = 207;

    private String subjectClassNotFoundErrorString(String subjectClassKey) {
        return "Config for '" + subjectClassKey + "' not found";
    }

    private String subjectNotFoundErrorString(String subjectClassKey,
                                              String subjectKey) {
        return "Config for '"
                + subjectClassKey + "/" + subjectKey
                + "' not found";
    }

    private String configKeyNotFoundErrorString(String subjectClassKey,
                                                String subjectKey,
                                                String configKey) {
        return "Config for '"
                + subjectClassKey + "/" + subjectKey + "/" + configKey
                + "' not found";
    }

    /**
     * Gets entire network configuration base.
     *
     * @return 200 OK with network configuration JSON
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response download() {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = mapper().createObjectNode();
        service.getSubjectClasses().forEach(sc -> {
            SubjectFactory subjectFactory = service.getSubjectFactory(sc);
            produceJson(service, newObject(root, subjectFactory.subjectClassKey()),
                        subjectFactory, sc);
        });
        return ok(root).build();
    }

    /**
     * Gets all network configuration for a subject class.
     *
     * @param subjectClassKey subject class key
     * @return 200 OK with network configuration JSON
     */
    @GET
    @Path("{subjectClassKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response download(@PathParam("subjectClassKey") String subjectClassKey) {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = mapper().createObjectNode();
        SubjectFactory subjectFactory =
                nullIsNotFound(service.getSubjectFactory(subjectClassKey),
                               subjectClassNotFoundErrorString(subjectClassKey));
        produceJson(service, root, subjectFactory, subjectFactory.subjectClass());
        return ok(root).build();
    }

    /**
     * Gets all network configuration for a subjectKey.
     *
     * @param subjectClassKey subjectKey class key
     * @param subjectKey      subjectKey key
     * @return 200 OK with network configuration JSON
     */
    @GET
    @Path("{subjectClassKey}/{subjectKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response download(@PathParam("subjectClassKey") String subjectClassKey,
                             @PathParam("subjectKey") String subjectKey) {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = mapper().createObjectNode();
        SubjectFactory subjectFactory =
                nullIsNotFound(service.getSubjectFactory(subjectClassKey),
                               subjectClassNotFoundErrorString(subjectClassKey));
        produceSubjectJson(service, root, subjectFactory.createSubject(subjectKey),
                           true,
                           subjectNotFoundErrorString(subjectClassKey, subjectKey));
        return ok(root).build();
    }

    /**
     * Gets specific network configuration for a subjectKey.
     *
     * @param subjectClassKey subjectKey class key
     * @param subjectKey      subjectKey key
     * @param configKey       configuration class key
     * @return 200 OK with network configuration JSON
     */
    @GET
    @Path("{subjectClassKey}/{subjectKey}/{configKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response download(@PathParam("subjectClassKey") String subjectClassKey,
                             @PathParam("subjectKey") String subjectKey,
                             @PathParam("configKey") String configKey) {
        NetworkConfigService service = get(NetworkConfigService.class);

        Object subject =
                nullIsNotFound(service.getSubjectFactory(subjectClassKey)
                                       .createSubject(subjectKey),
                                        subjectNotFoundErrorString(subjectClassKey, subjectKey));

        Class configClass =
                nullIsNotFound(service.getConfigClass(subjectClassKey, configKey),
                               configKeyNotFoundErrorString(subjectClassKey, subjectKey, configKey));
        Config config = nullIsNotFound((Config) service.getConfig(subject, configClass),
                               configKeyNotFoundErrorString(subjectClassKey,
                                                            subjectKey,
                                                            configKey));
        return ok(config.node()).build();
    }

    @SuppressWarnings("unchecked")
    private void produceJson(NetworkConfigService service, ObjectNode node,
                             SubjectFactory subjectFactory, Class subjectClass) {
        service.getSubjects(subjectClass).forEach(s ->
            produceSubjectJson(service, newObject(node, subjectFactory.subjectKey(s)), s, false, ""));
    }

    private void produceSubjectJson(NetworkConfigService service, ObjectNode node,
                                    Object subject,
                                    boolean emptyIsError,
                                    String emptyErrorMessage) {
        Set<? extends Config<Object>> configs = service.getConfigs(subject);
        if (emptyIsError) {
            // caller wants an empty set to be a 404
            configs = emptyIsNotFound(configs, emptyErrorMessage);
        }
        configs.forEach(c -> node.set(c.key(), c.node()));
    }


    /**
     * Uploads bulk network configuration.
     *
     * @param request network configuration JSON rooted at the top node
     * @return 200 OK
     * @throws IOException if unable to parse the request
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response upload(InputStream request) throws IOException {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = (ObjectNode) mapper().readTree(request);
        List<String> errorMsgs = new ArrayList<String>();
        root.fieldNames()
                .forEachRemaining(sk ->
                {
                    errorMsgs.addAll(consumeJson(service, (ObjectNode) root.path(sk),
                                                 service.getSubjectFactory(sk)));
                });
        if (errorMsgs.size() > 0) {
            return Response.status(MULTI_STATUS_RESPONE).entity(produceErrorJson(errorMsgs)).build();
        }
        return Response.ok().build();
    }

    /**
     * Upload multiple network configurations for a subject class.
     *
     * @param subjectClassKey subject class key
     * @param request         network configuration JSON rooted at the top node
     * @return 200 OK
     * @throws IOException if unable to parse the request
     */
    @POST
    @Path("{subjectClassKey}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response upload(@PathParam("subjectClassKey") String subjectClassKey,
                           InputStream request) throws IOException {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = (ObjectNode) mapper().readTree(request);
        List<String> errorMsgs = consumeJson(service, root, service.getSubjectFactory(subjectClassKey));
        if (errorMsgs.size() > 0) {
            return Response.status(MULTI_STATUS_RESPONE).entity(produceErrorJson(errorMsgs)).build();
        }
        return Response.ok().build();
    }

    /**
     * Upload mutliple network configurations for a subjectKey.
     *
     * @param subjectClassKey subjectKey class key
     * @param subjectKey      subjectKey key
     * @param request         network configuration JSON rooted at the top node
     * @return 200 OK
     * @throws IOException if unable to parse the request
     */
    @POST
    @Path("{subjectClassKey}/{subjectKey}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response upload(@PathParam("subjectClassKey") String subjectClassKey,
                           @PathParam("subjectKey") String subjectKey,
                           InputStream request) throws IOException {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = (ObjectNode) mapper().readTree(request);
        List<String> errorMsgs = consumeSubjectJson(service, root,
                                 service.getSubjectFactory(subjectClassKey).createSubject(subjectKey),
                                 subjectClassKey);
        if (errorMsgs.size() > 0) {
            return Response.status(MULTI_STATUS_RESPONE).entity(produceErrorJson(errorMsgs)).build();
        }
        return Response.ok().build();
    }

    /**
     * Upload specific network configuration for a subjectKey.
     *
     * @param subjectClassKey subjectKey class key
     * @param subjectKey      subjectKey key
     * @param configKey       configuration class key
     * @param request         network configuration JSON rooted at the top node
     * @return 200 OK
     * @throws IOException if unable to parse the request
     */
    @POST
    @Path("{subjectClassKey}/{subjectKey}/{configKey}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response upload(@PathParam("subjectClassKey") String subjectClassKey,
                           @PathParam("subjectKey") String subjectKey,
                           @PathParam("configKey") String configKey,
                           InputStream request) throws IOException {
        NetworkConfigService service = get(NetworkConfigService.class);
        JsonNode root = mapper().readTree(request);
        service.applyConfig(subjectClassKey,
                            service.getSubjectFactory(subjectClassKey).createSubject(subjectKey),
                            configKey, root);
        return Response.ok().build();
    }

    private List<String> consumeJson(NetworkConfigService service, ObjectNode classNode,
                             SubjectFactory subjectFactory) {
        List<String> errorMsgs = new ArrayList<String>();
        classNode.fieldNames().forEachRemaining(s -> {
            List<String> error = consumeSubjectJson(service, (ObjectNode) classNode.path(s),
                                                    subjectFactory.createSubject(s),
                                                    subjectFactory.subjectClassKey());
            errorMsgs.addAll(error);
        });
        return errorMsgs;
    }

    private List<String> consumeSubjectJson(NetworkConfigService service,
                                    ObjectNode subjectNode, Object subject,
                                    String subjectClassKey) {
        List<String> errorMsgs = new ArrayList<String>();
        subjectNode.fieldNames().forEachRemaining(configKey -> {
            try {
                service.applyConfig(subjectClassKey, subject, configKey, subjectNode.path(configKey));
            } catch (IllegalArgumentException e) {
                errorMsgs.add("Error parsing config " + subjectClassKey + "/" + subject + "/" + configKey);
            }
        });
        return errorMsgs;
    }

    private ObjectNode produceErrorJson(List<String> errorMsgs) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode().put("code", 207).putPOJO("message", errorMsgs);
        return result;
    }

    // FIXME: Refactor to allow queued configs to be removed

    /**
     * Clear entire network configuration base.
     *
     * @return 204 NO CONTENT
     */
    @DELETE
    @SuppressWarnings("unchecked")
    public Response delete() {
        NetworkConfigService service = get(NetworkConfigService.class);
        service.removeConfig();
        return Response.noContent().build();
    }

    /**
     * Clear all network configurations for a subject class.
     *
     * @param subjectClassKey subject class key
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{subjectClassKey}")
    @SuppressWarnings("unchecked")
    public Response delete(@PathParam("subjectClassKey") String subjectClassKey) {
        NetworkConfigService service = get(NetworkConfigService.class);
        service.getSubjects(service.getSubjectFactory(subjectClassKey).subjectClass())
                .forEach(subject -> service.removeConfig(subject));
        return Response.noContent().build();
    }

    /**
     * Clear all network configurations for a subjectKey.
     *
     * @param subjectClassKey subjectKey class key
     * @param subjectKey      subjectKey key
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{subjectClassKey}/{subjectKey}")
    @SuppressWarnings("unchecked")
    public Response delete(@PathParam("subjectClassKey") String subjectClassKey,
                           @PathParam("subjectKey") String subjectKey) {
        NetworkConfigService service = get(NetworkConfigService.class);
        service.removeConfig(service.getSubjectFactory(subjectClassKey).createSubject(subjectKey));
        return Response.noContent().build();
    }

    /**
     * Clear specific network configuration for a subjectKey.
     *
     * @param subjectClassKey subjectKey class key
     * @param subjectKey      subjectKey key
     * @param configKey       configuration class key
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{subjectClassKey}/{subjectKey}/{configKey}")
    @SuppressWarnings("unchecked")
    public Response delete(@PathParam("subjectClassKey") String subjectClassKey,
                           @PathParam("subjectKey") String subjectKey,
                           @PathParam("configKey") String configKey) {
        NetworkConfigService service = get(NetworkConfigService.class);
        service.removeConfig(subjectClassKey,
                             service.getSubjectFactory(subjectClassKey).createSubject(subjectKey),
                            configKey);
        return Response.noContent().build();
    }

}
