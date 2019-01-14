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

package org.onosproject.openflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.Config;
import org.onlab.packet.EthType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration for classifiers.
 */
public class OpenFlowClassifierConfig extends Config<DeviceId> {
    private static Logger log = LoggerFactory.getLogger(OpenFlowClassifierConfig.class);

    public static final String TARGET_QUEUE = "target-queue";
    public static final String ETHER_TYPE = "ethernet-type";

    private static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String CLASSF_NULL_ERROR = "Classifier cannot be null";

    private short etherValue(String etherType) throws IllegalArgumentException {
        short etherTypeValue;
        try {
            if (etherType.startsWith("0x")) {
                Integer e = Integer.valueOf(etherType.substring(2), 16);
                if (e < 0 || e > 0xFFFF) {
                    throw new IllegalArgumentException("EtherType value out of range");
                }
                etherTypeValue = e.shortValue();
            } else {
                etherTypeValue = EthType.EtherType.valueOf(etherType).ethType().toShort();
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse ethernet type string");
        }
        return etherTypeValue;
    }

    @Override
    public boolean isValid() {
        for (JsonNode node : array) {
            if (!hasOnlyFields((ObjectNode) node, TARGET_QUEUE, ETHER_TYPE)) {
                return false;
            }

            ObjectNode obj = (ObjectNode) node;

            if (!(isString(obj, ETHER_TYPE, FieldPresence.MANDATORY) &&
                  isIntegralNumber(obj, TARGET_QUEUE, FieldPresence.MANDATORY, 0, 7))) {
                return false;
            }

            try {
                etherValue(node.path(ETHER_TYPE).asText());
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retrieves all classifiers configured on this port.
     *
     * @return set of classifiers
     */
    public Set<OpenFlowClassifier> getClassifiers() {
        Set<OpenFlowClassifier> classifiers = Sets.newHashSet();

        for (JsonNode classfNode : array) {
            DeviceId deviceId = this.subject();
            short ethernetType = etherValue(classfNode.path(ETHER_TYPE).asText());
            int idQueue = Integer.valueOf(classfNode.path(TARGET_QUEUE).asText());

            OpenFlowClassifier classf =
                new OpenFlowClassifier.Builder(deviceId, idQueue).ethernetType(ethernetType).build();
            classifiers.add(classf);
        }

        return classifiers;
    }

    /**
     * Adds a classifier to the config.
     *
     * @param classf classifier to add
     */
    public void addClassifier(OpenFlowClassifier classf) {
        checkNotNull(classf, CLASSF_NULL_ERROR);
        checkArgument(classf.deviceId().equals(this.subject()));

        ObjectNode classfNode = array.addObject();

        EthType.EtherType e = EthType.EtherType.lookup(classf.ethernetType());
        if (e.equals(EthType.EtherType.UNKNOWN)) {
            classfNode.put(ETHER_TYPE, String.format("0x%04x", classf.ethernetType()));
        } else {
            classfNode.put(ETHER_TYPE, e.name());
        }
        classfNode.put(TARGET_QUEUE, classf.idQueue());
    }

    /**
     * Removes a classifier from the config.
     *
     * @param classf classifier to remove
     */
    public void removeClassifier(OpenFlowClassifier classf) {
        checkNotNull(classf, CLASSF_NULL_ERROR);
        checkArgument(classf.deviceId().equals(this.subject()));

        Iterator<JsonNode> it = array.iterator();
        while (it.hasNext()) {
            JsonNode node = it.next();
            if (etherValue(node.path(ETHER_TYPE).asText()) == classf.ethernetType()
                && Integer.valueOf(node.path(TARGET_QUEUE).asText()) == classf.idQueue()) {
                it.remove();
                break;
            }
        }
    }
}
