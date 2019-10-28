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

package org.onosproject.net.optical.rest;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onlab.util.Tools.nullIsNotFound;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.graph.ScalarWeight;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Link;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.OchSignal;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.optical.json.OchSignalCodec;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.onosproject.net.optical.util.OpticalIntentUtility.createExplicitOpticalIntent;
import static org.slf4j.LoggerFactory.getLogger;

import static org.onlab.util.Tools.readTreeFromStream;


/**
 * Query, submit and withdraw optical network intents.
 */
@Path("intents")
public class OpticalIntentsWebResource extends AbstractWebResource {

    private static final Logger log = getLogger(OpticalIntentsWebResource.class);

    private static final String JSON_INVALID = "Invalid json input";
    private static final String APP_ID = "appId";
    private static final String INGRESS_POINT = "ingressPoint";
    private static final String EGRESS_POINT = "egressPoint";
    private static final String BIDIRECTIONAL = "bidirectional";
    private static final String SIGNAL = "signal";
    private static final String SUGGESTEDPATH = "suggestedPath";
    private static final String MISSING_MEMBER_MESSAGE = " member is required";
    private static final String E_APP_ID_NOT_FOUND = "Application ID is not found";
    private static final ProviderId PROVIDER_ID = new ProviderId("netconf", "optical-rest");
    private static final int NUM_CRITERIA_OPTICAL_CONNECTIVIY_RULE = 3;

    @Context
    private UriInfo uriInfo;

    /**
     * Submits a new optical intent.
     * Creates and submits optical intents from the JSON request.
     *
     * @param stream input JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel CreateIntent
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIntent(InputStream stream) {
        try {
            IntentService service = get(IntentService.class);
            ObjectNode root = readTreeFromStream(mapper(), stream);
            Intent intent = decode(root);
            service.submit(intent);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("intents")
                    .path(intent.appId().name())
                    .path(Long.toString(intent.id().fingerprint()));
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    /**
     * Get the optical intents on the network.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIntents() {

        DeviceService deviceService = get(DeviceService.class);

        IntentService intentService = get(IntentService.class);
        Iterator intentItr = intentService.getIntents().iterator();

        ArrayNode arrayFlows = mapper().createArrayNode();

        while (intentItr.hasNext()) {

            Intent intent = (Intent) intentItr.next();
            if (intent instanceof OpticalConnectivityIntent) {

                OpticalConnectivityIntent opticalConnectivityIntent = (OpticalConnectivityIntent) intent;

                Device srcDevice = deviceService.getDevice(opticalConnectivityIntent.getSrc().deviceId());
                Device dstDevice = deviceService.getDevice(opticalConnectivityIntent.getDst().deviceId());

                String srcDeviceName = srcDevice.annotations().value(AnnotationKeys.NAME);
                String dstDeviceName = dstDevice.annotations().value(AnnotationKeys.NAME);

                ObjectNode objectNode = mapper().createObjectNode();

                objectNode.put("intent id", opticalConnectivityIntent.id().toString());
                objectNode.put("app id", opticalConnectivityIntent.appId().name());
                objectNode.put("state",
                        intentService.getIntentState(opticalConnectivityIntent.key()).toString());
                objectNode.put("src", opticalConnectivityIntent.getSrc().toString());
                objectNode.put("dst", opticalConnectivityIntent.getDst().toString());
                objectNode.put("srcName", srcDeviceName);
                objectNode.put("dstName", dstDeviceName);

                //Only for INSTALLED intents
                if (intentService.getIntentState(intent.key()) == IntentState.INSTALLED) {

                    //Retrieve associated FlowRuleIntent
                    FlowRuleIntent installableIntent =
                            (FlowRuleIntent) intentService.getInstallableIntents(opticalConnectivityIntent.key())
                            .stream()
                            .filter(FlowRuleIntent.class::isInstance)
                            .findFirst()
                            .orElse(null);

                    //FlowRules computed by the OpticalConnectivityIntentCompiler includes 3 criteria, one of those
                    //is the OchSignal, thus retrieve used ochSignal from the selector of one of the installed rules
                    //TODO store utilized ochSignal in the intent resources
                    if (installableIntent != null) {
                        OchSignal signal = installableIntent.flowRules().stream()
                                .filter(r -> r.selector().criteria().size() == NUM_CRITERIA_OPTICAL_CONNECTIVIY_RULE)
                                .map(r -> ((OchSignalCriterion)
                                        r.selector().getCriterion(Criterion.Type.OCH_SIGID)).lambda())
                                .findFirst()
                                .orElse(null);

                        objectNode.put("ochSignal", signal.toString());
                        objectNode.put("centralFreq", signal.centralFrequency().asTHz() + " THz");
                    }

                    //Retrieve path and print it to REST
                    if (installableIntent != null) {
                        String path = installableIntent.resources().stream()
                                .filter(Link.class::isInstance)
                                .map(Link.class::cast)
                                .map(r -> deviceService.getDevice(r.src().deviceId()))
                                .map(r -> r.annotations().value(AnnotationKeys.NAME))
                                .collect(Collectors.joining(" -> "));

                        List<Link> pathLinks = installableIntent.resources().stream()
                                .filter(Link.class::isInstance)
                                .map(Link.class::cast)
                                .collect(Collectors.toList());

                        DefaultPath defaultPath = new DefaultPath(PROVIDER_ID, pathLinks, new ScalarWeight(1));

                        objectNode.put("path", defaultPath.toString());
                        objectNode.put("pathName", path + " -> " + dstDeviceName);
                    }
                }

                arrayFlows.add(objectNode);
            }
        }

        ObjectNode root = this.mapper().createObjectNode().putPOJO("Intents", arrayFlows);
        return ok(root).build();
    }

    /**
     * Delete the specified optical intent.
     *
     * @param appId application identifier
     * @param keyString   intent key
     * @return 204 NO CONTENT
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{appId}/{key}")
    public Response deleteIntent(@PathParam("appId") String appId,
                                 @PathParam("key") String keyString) {

        final ApplicationId app = get(CoreService.class).getAppId(appId);
        nullIsNotFound(app, "Application Id not found");

        IntentService intentService = get(IntentService.class);
        Intent intent = intentService.getIntent(Key.of(keyString, app));
        if (intent == null) {
            intent = intentService.getIntent(Key.of(Long.decode(keyString), app));
        }
        nullIsNotFound(intent, "Intent Id is not found");

        if ((intent instanceof OpticalConnectivityIntent) || (intent instanceof OpticalCircuitIntent)) {
            intentService.withdraw(intent);
        } else {
            throw new IllegalArgumentException("Specified intent is not of type OpticalConnectivityIntent");
        }

        return Response.noContent().build();
    }

    private Intent decode(ObjectNode json) {
        JsonNode ingressJson = json.get(INGRESS_POINT);
        if (!ingressJson.isObject()) {
            throw new IllegalArgumentException(JSON_INVALID);
        }

        ConnectPoint ingress = codec(ConnectPoint.class).decode((ObjectNode) ingressJson, this);

        JsonNode egressJson = json.get(EGRESS_POINT);
        if (!egressJson.isObject()) {
            throw new IllegalArgumentException(JSON_INVALID);
        }

        ConnectPoint egress = codec(ConnectPoint.class).decode((ObjectNode) egressJson, this);

        JsonNode bidirectionalJson = json.get(BIDIRECTIONAL);
        boolean bidirectional = bidirectionalJson != null ? bidirectionalJson.asBoolean() : false;

        JsonNode signalJson = json.get(SIGNAL);
        OchSignal signal = null;
        if (signalJson != null) {
            if (!signalJson.isObject()) {
                throw new IllegalArgumentException(JSON_INVALID);
            } else {
                signal = OchSignalCodec.decode((ObjectNode) signalJson);
            }
        }

        String appIdString = nullIsIllegal(json.get(APP_ID), APP_ID + MISSING_MEMBER_MESSAGE).asText();
        CoreService service = getService(CoreService.class);
        ApplicationId appId = nullIsNotFound(service.getAppId(appIdString), E_APP_ID_NOT_FOUND);

        Key key = null;
        DeviceService deviceService = get(DeviceService.class);

        JsonNode suggestedPathJson = json.get(SUGGESTEDPATH);
        DefaultPath suggestedPath = null;
        LinkService linkService = get(LinkService.class);

        if (suggestedPathJson != null) {
            if (!suggestedPathJson.isObject()) {
                throw new IllegalArgumentException(JSON_INVALID);
            } else {
                ArrayNode linksJson = nullIsIllegal((ArrayNode) suggestedPathJson.get("links"),
                        "Suggested path specified without links");

                List<Link> listLinks = new ArrayList<>();

                for (JsonNode node : linksJson) {

                    String srcString = node.get("src").asText();
                    String dstString = node.get("dst").asText();

                    ConnectPoint srcConnectPoint = ConnectPoint.fromString(srcString);
                    ConnectPoint dstConnectPoint = ConnectPoint.fromString(dstString);

                    Link link = linkService.getLink(srcConnectPoint, dstConnectPoint);
                    if (link == null) {
                        throw new IllegalArgumentException("Not existing link in the suggested path");
                    }

                    listLinks.add(link);
                }

                if ((!listLinks.get(0).src().deviceId().equals(ingress.deviceId())) ||
                        (!listLinks.get(0).src().port().equals(ingress.port())) ||
                        (!listLinks.get(listLinks.size() - 1).dst().deviceId().equals(egress.deviceId())) ||
                        (!listLinks.get(listLinks.size() - 1).dst().port().equals(egress.port()))) {
                    throw new IllegalArgumentException(
                            "Suggested path not compatible with ingress or egress connect points");
                }

                if (!isPathContiguous(listLinks)) {
                    throw new IllegalArgumentException(
                            "Links specified in the suggested path are not contiguous");
                }

                suggestedPath = new DefaultPath(PROVIDER_ID, listLinks, new ScalarWeight(1));

                log.debug("OpticalIntent along suggestedPath {}", suggestedPath);
            }
        }

        return createExplicitOpticalIntent(
                ingress, egress, deviceService, key, appId, bidirectional, signal, suggestedPath);
    }

    private boolean isPathContiguous(List<Link> path) {
        DeviceId previousDst;
        DeviceId currentSrc;

        for (int i = 1; i < path.size(); i++) {
            previousDst = path.get(i - 1).dst().deviceId();
            currentSrc = path.get(i).src().deviceId();

            if (!previousDst.equals(currentSrc)) {
                log.debug("OpticalIntent links are not contiguous previous {} current {}", previousDst, currentSrc);
                return false;
            }
        }
        return true;
    }
}
