/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.tapi;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.ImmutableList;
import org.apache.http.HttpStatus;
import org.onosproject.drivers.odtn.impl.DeviceConnection;
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.CONN_REQ_POST_API;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.CONN_REQ_REMOVE_DATA_API;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.END_POINT;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.LAYER_PROTOCOL_NAME;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.LAYER_PROTOCOL_QUALIFIER;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.LOCAL_ID;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.PHOTONIC_MEDIA;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.POINT_TO_POINT_CONNECTIVITY;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.SERVICE_INTERFACE_POINT;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.SERVICE_INTERFACE_POINT_UUID;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.SERVICE_LAYER;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.SERVICE_TYPE;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.TAPI_CONNECTIVITY_CONNECTIVITY_SERVICE;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.TAPI_PHOTONIC_MEDIA_PHOTONIC_LAYER_QUALIFIER_NMC;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.getConnectionCache;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.getUuids;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver Implementation of the DeviceDescrption discovery for ONF Transport-API (TAPI) v2.1 based
 * open line systems (OLS).
 */

public class TapiFlowRuleProgrammable extends AbstractHandlerBehaviour
        implements FlowRuleProgrammable {

    private static final Logger log = getLogger(TapiFlowRuleProgrammable.class);


    @Override
    public Collection<FlowEntry> getFlowEntries() {
        DeviceId deviceId = did();
        //TODO this is a blocking call on ADVA OLS, right now using cache.
        //return getFlowsFromConnectivityServices(deviceId);
        List<FlowEntry> entries = new ArrayList<>();
        Set<FlowRule> rules = getConnectionCache().get(deviceId);
        if (rules != null) {
            rules.forEach(rule -> {
                entries.add(new DefaultFlowEntry(rule, FlowEntry.FlowEntryState.ADDED, 0, 0, 0));
            });
        }
        return entries;
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        DeviceId deviceId = handler().data().deviceId();
        RestSBController controller = checkNotNull(handler().get(RestSBController.class));
        ImmutableList.Builder<FlowRule> added = ImmutableList.builder();
        rules.forEach(flowRule -> {
            String uuid = createUuid();
            ByteArrayOutputStream applyConnectivityRequest = createConnectivityRequest(uuid, flowRule);
            if (applyConnectivityRequest.size() != 0) {
                log.debug("Connectivity request {}", applyConnectivityRequest.toString());
                CompletableFuture<Integer> flowInstallation =
                        CompletableFuture.supplyAsync(() -> controller.post(deviceId, CONN_REQ_POST_API,
                                new ByteArrayInputStream(applyConnectivityRequest.toByteArray()),
                                MediaType.APPLICATION_JSON_TYPE));
                log.debug("Added {} to {}", flowRule, deviceId);
                getConnectionCache().add(deviceId, uuid, flowRule);
                added.add(flowRule);
                flowInstallation.thenApply(result -> {
                    if (result == HttpStatus.SC_CREATED || result == HttpStatus.SC_OK) {
                        log.info("Added {} to deviceId {}", flowRule, deviceId);
                    } else {
                        log.error("Can't add flow {}, result {}", flowRule, result);
                        getConnectionCache().remove(deviceId, flowRule);
                    }
                    return result;
                });
            }
        });
        //TODO workaround for blocking call on ADVA OLS should return added
        return rules;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        DeviceId deviceId = handler().data().deviceId();
        RestSBController controller = checkNotNull(handler().get(RestSBController.class));
        ImmutableList.Builder<FlowRule> removed = ImmutableList.builder();
        rules.forEach(flowRule -> {
            DeviceConnection conn = getConnectionCache().get(deviceId, flowRule.id());
            if (conn == null || conn.getId() == null) {
                log.warn("Can't find associate device connection for flow {} and device {}",
                        flowRule.id(), deviceId);
                return;
            }
            CompletableFuture<Integer> flowRemoval =
                    CompletableFuture.supplyAsync(() -> controller.delete(deviceId,
                            CONN_REQ_REMOVE_DATA_API + conn.getId(),
                            null, MediaType.APPLICATION_JSON_TYPE));
            flowRemoval.thenApply(result -> {
                if (result == HttpStatus.SC_NO_CONTENT) {
                    getConnectionCache().remove(deviceId, flowRule);
                    removed.add(flowRule);
                } else {
                    log.error("Can't remove flow {}, result {}", flowRule, result);
                }
                return result;
            });
        });
        //TODO workaround for blocking call on ADVA OLS shoudl return removed
        return rules;
    }

    /**
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }

    //Currently uused because get is a blocking call on ADVA
    private Collection<FlowEntry> getFlowsFromConnectivityServices(DeviceId deviceId) {
        Set<String> uuids = getUuids(deviceId, handler());
        if (uuids.isEmpty()) {
            return ImmutableList.of();
        }
        DeviceConnectionCache cache = getConnectionCache();
        if (cache.get(deviceId) == null) {
            return ImmutableList.of();
        }
        List<FlowEntry> entries = new ArrayList<>();
        uuids.forEach(uuid -> {
            FlowRule rule = cache.get(deviceId, uuid);
            if (rule != null) {
                entries.add(new DefaultFlowEntry(rule, FlowEntry.FlowEntryState.ADDED, 0, 0, 0));
            } else {
                log.info("Non existing rule for uuid {}", uuid);
            }
        });
        return entries;
    }


    ByteArrayOutputStream createConnectivityRequest(String uuid, FlowRule rule) {
        /*
        {
            "tapi-connectivity:connectivity-service":[
                {
                    "uuid":"ffb006d4-349e-4d2f-817e-0906c88458d0",
                    "service-layer":"PHOTONIC_MEDIA",
                    "service-type":"POINT_TO_POINT_CONNECTIVITY",
                    "end-point":[
                        {
                            "local-id":"1",
                            "layer-protocol-name":"PHOTONIC_MEDIA",
                            "layer-protocol-qualifier":"tapi-photonic-media:PHOTONIC_LAYER_QUALIFIER_NMC",
                            "service-interface-point":{
                                "service-interface-point-uuid":"0923962e-b83f-4702-9b16-a1a0db0dc1f9"
                            }
                        },
                        {
                            "local-id":"2",
                            "layer-protocol-name":"PHOTONIC_MEDIA",
                            "layer-protocol-qualifier":"tapi-photonic-media:PHOTONIC_LAYER_QUALIFIER_NMC",
                            "service-interface-point":{
                                "service-interface-point-uuid":"76be95de-5769-4e5d-b65e-62cb6c39cf6b "
                            }
                        }
                    ]
               }
           ]
        }
        */
        DeviceService deviceService = handler().get(DeviceService.class);
        PortCriterion inputPortCriterion = (PortCriterion) checkNotNull(rule.selector()
                .getCriterion(Criterion.Type.IN_PORT));
        String inputPortUuid = deviceService.getPort(rule.deviceId(),
                inputPortCriterion.port()).annotations().value(TapiDeviceHelper.UUID);

        Instructions.OutputInstruction outInstruction = (Instructions.OutputInstruction) checkNotNull(rule.treatment()
                .allInstructions().stream().filter(instr -> instr.type().equals(Instruction.Type.OUTPUT))
                .findFirst().orElse(null));
        String outputPortUuid = deviceService.getPort(rule.deviceId(),
                outInstruction.port()).annotations().value(TapiDeviceHelper.UUID);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            JsonGenerator generator = getJsonGenerator(stream);
            generator.writeStartObject();
            generator.writeArrayFieldStart(TAPI_CONNECTIVITY_CONNECTIVITY_SERVICE);
            generator.writeStartObject();
            generator.writeStringField(TapiDeviceHelper.UUID, uuid);
            generator.writeStringField(SERVICE_LAYER, PHOTONIC_MEDIA);
            generator.writeStringField(SERVICE_TYPE, POINT_TO_POINT_CONNECTIVITY);
            generator.writeArrayFieldStart(END_POINT);
            //ADVA OLS requires these to be 1,2 for every connection
            addEndPoint(generator, inputPortUuid, 1);
            addEndPoint(generator, outputPortUuid, 2);
            generator.writeEndArray();
            generator.writeEndObject();
            generator.writeEndArray();
            generator.writeEndObject();
            generator.close();
            return stream;
        } catch (IOException e) {
            log.error("Cant' create json", e);
        }
        return stream;
    }

    private JsonGenerator getJsonGenerator(ByteArrayOutputStream stream) throws IOException {
        JsonFactory factory = new JsonFactory();
        return factory.createGenerator(stream, JsonEncoding.UTF8);
    }

    /*
    {
          "local-id":"1",
          "layer-protocol-name":"PHOTONIC_MEDIA",
          "layer-protocol-qualifier":"tapi-photonic-media:PHOTONIC_LAYER_QUALIFIER_NMC",
          "service-interface-point":{
                "service-interface-point-uuid":"0923962e-b83f-4702-9b16-a1a0db0dc1f9"
         }
     }
     */
    private void addEndPoint(JsonGenerator generator, String sipUuid, int localId) throws IOException {
        generator.writeStartObject();
        //ADVA OLS requires Local-id to be integer incremental numbers
        generator.writeStringField(LOCAL_ID, Integer.toString(localId));
        generator.writeStringField(LAYER_PROTOCOL_NAME, PHOTONIC_MEDIA);
        generator.writeStringField(LAYER_PROTOCOL_QUALIFIER,
                TAPI_PHOTONIC_MEDIA_PHOTONIC_LAYER_QUALIFIER_NMC);
        generator.writeObjectFieldStart(SERVICE_INTERFACE_POINT);
        generator.writeStringField(SERVICE_INTERFACE_POINT_UUID, sipUuid);
        generator.writeEndObject();
        generator.writeEndObject();
    }

    private String createUuid() {
        return UUID.randomUUID().toString();
    }
}
