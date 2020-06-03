/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.server;

import org.onlab.util.Bandwidth;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.DefaultQueueDescription;
import org.onosproject.net.behaviour.QueueConfigBehaviour;
import org.onosproject.net.behaviour.QueueDescription;
import org.onosproject.net.behaviour.QueueDescription.Type;
import org.onosproject.net.behaviour.QueueId;
import org.onosproject.protocol.rest.RestSBDevice;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList;

import java.io.InputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.ws.rs.ProcessingException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.JSON;
import static org.onosproject.drivers.server.Constants.PARAM_ID;
import static org.onosproject.drivers.server.Constants.PARAM_NICS;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_MAX_RATE;
import static org.onosproject.drivers.server.Constants.PARAM_QUEUES;
import static org.onosproject.drivers.server.Constants.PARAM_TYPE;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_ID_NULL;
import static org.onosproject.drivers.server.Constants.MSG_DEVICE_NULL;
import static org.onosproject.drivers.server.Constants.URL_NIC_QUEUE_ADMIN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of queue config behaviour for server devices.
 */
public class ServerQueueConfig
        extends BasicServerDriver
        implements QueueConfigBehaviour {

    private final Logger log = getLogger(getClass());

    public ServerQueueConfig() {
        super();
        log.debug("Started");
    }

    @Override
    public Collection<QueueDescription> getQueues() {
        // Retrieve the device ID from the handler
        DeviceId deviceId = super.getDeviceId();
        checkNotNull(deviceId, MSG_DEVICE_ID_NULL);

        // Get the device
        RestSBDevice device = super.getDevice(deviceId);
        checkNotNull(device, MSG_DEVICE_NULL);

        // Hit the path that provides queue administration
        InputStream response = null;
        try {
            response = getController().get(deviceId, URL_NIC_QUEUE_ADMIN, JSON);
        } catch (ProcessingException pEx) {
            log.error("Failed to get NIC queues from device: {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        // Load the JSON into object
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonMap = null;
        JsonNode jsonNode = null;
        ObjectNode objNode = null;
        try {
            jsonMap  = mapper.readValue(response, Map.class);
            jsonNode = mapper.convertValue(jsonMap, JsonNode.class);
            objNode = (ObjectNode) jsonNode;
        } catch (IOException ioEx) {
            log.error("Failed to get NIC queues from device: {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        if (objNode == null) {
            log.error("Failed to get NIC queues from device: {}", deviceId);
            return Collections.EMPTY_LIST;
        }

        Collection<QueueDescription> queueDescs = Sets.newHashSet();

        // Fetch NICs' array
        JsonNode nicsNode = objNode.path(PARAM_NICS);

        for (JsonNode nn : nicsNode) {
            ObjectNode nicObjNode = (ObjectNode) nn;
            int nicId = nicObjNode.path(PARAM_ID).asInt();
            JsonNode queuesNode = nicObjNode.path(PARAM_QUEUES);

            // Each NIC has a set of queues
            for (JsonNode qn : queuesNode) {
                ObjectNode queueObjNode = (ObjectNode) qn;

                // Get the attributes of a queue
                int queueIdInt = queueObjNode.path(PARAM_ID).asInt();
                String queueTypeStr = get(qn, PARAM_TYPE);
                long queueRateInt = queueObjNode.path(PARAM_NIC_MAX_RATE).asLong();

                QueueId queueId = QueueId.queueId("nic" + nicId + ":" + queueIdInt);
                EnumSet<Type> queueTypes = getQueueTypesFromString(queueTypeStr);
                Bandwidth queueRate = Bandwidth.mbps(queueRateInt);

                queueDescs.add(
                    DefaultQueueDescription.builder()
                        .queueId(queueId)
                        .type(queueTypes)
                        .maxRate(queueRate)
                        .build());
            }
        }

        log.info("[Device {}] NIC queues: {}", deviceId, queueDescs);

        return ImmutableList.copyOf(queueDescs);
    }

    @Override
    public QueueDescription getQueue(QueueDescription queueDesc) {
        for (QueueDescription qDesc : getQueues()) {
            if (queueDesc.queueId().equals(qDesc.queueId())) {
                return qDesc;
            }
        }

        return null;
    }

    @Override
    public boolean addQueue(QueueDescription queue) {
        throw new UnsupportedOperationException("Add queue operation not supported");
    }

    @Override
    public void deleteQueue(QueueId queueId) {
        throw new UnsupportedOperationException("Delete queue operation not supported");
    }

    /**
     * Convert string-based queue type into enum set.
     *
     * @param queueTypeStr string-based queue type
     * @return queue types enum set
     */
    private EnumSet<Type> getQueueTypesFromString(String queueTypeStr) {
        EnumSet<Type> enumSet = EnumSet.noneOf(Type.class);
        if ((queueTypeStr == null) || (queueTypeStr.isEmpty())) {
            return enumSet;
        }

        if (queueTypeStr.toUpperCase().equals("MIN")) {
            enumSet.add(Type.MAX);
        } else if (queueTypeStr.toUpperCase().equals("MAX")) {
            enumSet.add(Type.MIN);
        } else if (queueTypeStr.toUpperCase().equals("PRIORITY")) {
            enumSet.add(Type.PRIORITY);
        } else {
            enumSet.add(Type.BURST);
        }

        return enumSet;
    }

}
