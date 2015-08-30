/*
 * Copyright 2015 Open Networking Laboratory
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

import com.fasterxml.jackson.databind.node.ObjectNode;
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

/**
 * Manage network configurations.
 */
@Path("network/configuration")
public class NetworkConfigWebResource extends AbstractWebResource {

    /**
     * Get entire network configuration base.
     *
     * @return network configuration JSON
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response download() {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = mapper().createObjectNode();
        service.getSubjectClasses().forEach(sc ->
            produceJson(service, newObject(root, service.getSubjectFactory(sc).subjectKey()), sc));
        return ok(root).build();
    }

    /**
     * Get all network configuration for a subject class.
     *
     * @param subjectKey subject class key
     * @return network configuration JSON
     */
    @GET
    @Path("{subjectKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response download(@PathParam("subjectKey") String subjectKey) {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = mapper().createObjectNode();
        produceJson(service, root, service.getSubjectFactory(subjectKey).subjectClass());
        return ok(root).build();
    }

    /**
     * Get all network configuration for a subject.
     *
     * @param subjectKey subject class key
     * @param subject    subject key
     * @return network configuration JSON
     */
    @GET
    @Path("{subjectKey}/{subject}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response download(@PathParam("subjectKey") String subjectKey,
                             @PathParam("subject") String subject) {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = mapper().createObjectNode();
        produceSubjectJson(service, root,
                           service.getSubjectFactory(subjectKey).createSubject(subject));
        return ok(root).build();
    }

    /**
     * Get specific network configuration for a subject.
     *
     * @param subjectKey subject class key
     * @param subject    subject key
     * @param configKey  configuration class key
     * @return network configuration JSON
     */
    @GET
    @Path("{subjectKey}/{subject}/{configKey}")
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response download(@PathParam("subjectKey") String subjectKey,
                             @PathParam("subject") String subject,
                             @PathParam("configKey") String configKey) {
        NetworkConfigService service = get(NetworkConfigService.class);
        return ok(service.getConfig(service.getSubjectFactory(subjectKey).createSubject(subject),
                                    service.getConfigClass(subjectKey, configKey)).node()).build();
    }

    @SuppressWarnings("unchecked")
    private void produceJson(NetworkConfigService service, ObjectNode node,
                             Class subjectClass) {
        service.getSubjects(subjectClass).forEach(s ->
            produceSubjectJson(service, newObject(node, s.toString()), s));
    }

    private void produceSubjectJson(NetworkConfigService service, ObjectNode node,
                                    Object subject) {
        service.getConfigs(subject).forEach(c -> node.set(c.key(), c.node()));
    }


    /**
     * Upload bulk network configuration.
     *
     * @param request network configuration JSON rooted at the top node
     * @throws IOException if unable to parse the request
     * @return empty response
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response upload(InputStream request) throws IOException {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = (ObjectNode) mapper().readTree(request);
        root.fieldNames()
                .forEachRemaining(sk -> consumeJson(service, (ObjectNode) root.path(sk),
                                                    service.getSubjectFactory(sk)));
        return Response.ok().build();
    }

    /**
     * Upload multiple network configurations for a subject class.
     *
     * @param subjectKey subject class key
     * @param request    network configuration JSON rooted at the top node
     * @return empty response
     * @throws IOException if unable to parse the request
     */
    @POST
    @Path("{subjectKey}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response upload(@PathParam("subjectKey") String subjectKey,
                           InputStream request) throws IOException {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = (ObjectNode) mapper().readTree(request);
        consumeJson(service, root, service.getSubjectFactory(subjectKey));
        return Response.ok().build();
    }

    /**
     * Upload mutliple network configurations for a subject.
     *
     * @param subjectKey subject class key
     * @param subject    subject key
     * @param request    network configuration JSON rooted at the top node
     * @return empty response
     * @throws IOException if unable to parse the request
     */
    @POST
    @Path("{subjectKey}/{subject}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response upload(@PathParam("subjectKey") String subjectKey,
                           @PathParam("subject") String subject,
                           InputStream request) throws IOException {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = (ObjectNode) mapper().readTree(request);
        consumeSubjectJson(service, root,
                           service.getSubjectFactory(subjectKey).createSubject(subject),
                           subjectKey);
        return Response.ok().build();
    }

    /**
     * Upload specific network configuration for a subject.
     *
     * @param subjectKey subject class key
     * @param subject    subject key
     * @param configKey  configuration class key
     * @param request    network configuration JSON rooted at the top node
     * @return empty response
     * @throws IOException if unable to parse the request
     */
    @POST
    @Path("{subjectKey}/{subject}/{configKey}")
    @Consumes(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response upload(@PathParam("subjectKey") String subjectKey,
                           @PathParam("subject") String subject,
                           @PathParam("configKey") String configKey,
                           InputStream request) throws IOException {
        NetworkConfigService service = get(NetworkConfigService.class);
        ObjectNode root = (ObjectNode) mapper().readTree(request);
        service.applyConfig(service.getSubjectFactory(subjectKey).createSubject(subject),
                            service.getConfigClass(subjectKey, configKey), root);
        return Response.ok().build();
    }

    private void consumeJson(NetworkConfigService service, ObjectNode classNode,
                             SubjectFactory subjectFactory) {
        classNode.fieldNames().forEachRemaining(s ->
                                                        consumeSubjectJson(service, (ObjectNode) classNode.path(s),
                                                                           subjectFactory.createSubject(s),
                                                                           subjectFactory.subjectKey()));
    }

    private void consumeSubjectJson(NetworkConfigService service,
                                    ObjectNode subjectNode, Object subject,
                                    String subjectKey) {
        subjectNode.fieldNames().forEachRemaining(c ->
            service.applyConfig(subject, service.getConfigClass(subjectKey, c),
                                (ObjectNode) subjectNode.path(c)));
    }


    /**
     * Clear entire network configuration base.
     *
     * @return empty response
     */
    @DELETE
    @SuppressWarnings("unchecked")
    public Response delete() {
        NetworkConfigService service = get(NetworkConfigService.class);
        service.getSubjectClasses()
                .forEach(subjectClass -> service.getSubjects(subjectClass)
                        .forEach(subject -> service.getConfigs(subject)
                                .forEach(config -> service
                                        .removeConfig(subject, config.getClass()))));
        return Response.ok().build();
    }

    /**
     * Clear all network configurations for a subject class.
     *
     * @param subjectKey subject class key
     * @return empty response
     */
    @DELETE
    @Path("{subjectKey}")
    @SuppressWarnings("unchecked")
    public Response delete(@PathParam("subjectKey") String subjectKey) {
        NetworkConfigService service = get(NetworkConfigService.class);
        service.getSubjects(service.getSubjectFactory(subjectKey).getClass())
                .forEach(subject -> service.getConfigs(subject)
                        .forEach(config -> service
                                .removeConfig(subject, config.getClass())));
        return Response.ok().build();
    }

    /**
     * Clear all network configurations for a subject.
     *
     * @param subjectKey subject class key
     * @param subject    subject key
     * @return empty response
     */
    @DELETE
    @Path("{subjectKey}/{subject}")
    @SuppressWarnings("unchecked")
    public Response delete(@PathParam("subjectKey") String subjectKey,
                           @PathParam("subject") String subject) {
        NetworkConfigService service = get(NetworkConfigService.class);
        Object s = service.getSubjectFactory(subjectKey).createSubject(subject);
        service.getConfigs(s).forEach(c -> service.removeConfig(s, c.getClass()));
        return Response.ok().build();
    }

    /**
     * Clear specific network configuration for a subject.
     *
     * @param subjectKey subject class key
     * @param subject    subject key
     * @param configKey  configuration class key
     * @return empty response
     */
    @DELETE
    @Path("{subjectKey}/{subject}/{configKey}")
    @SuppressWarnings("unchecked")
    public Response delete(@PathParam("subjectKey") String subjectKey,
                           @PathParam("subject") String subject,
                           @PathParam("configKey") String configKey) {
        NetworkConfigService service = get(NetworkConfigService.class);
        service.removeConfig(service.getSubjectFactory(subjectKey).createSubject(subject),
                             service.getConfigClass(subjectKey, configKey));
        return Response.ok().build();
    }

}
