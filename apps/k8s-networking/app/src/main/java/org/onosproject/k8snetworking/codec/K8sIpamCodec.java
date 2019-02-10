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
package org.onosproject.k8snetworking.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.k8snetworking.api.DefaultK8sIpam;
import org.onosproject.k8snetworking.api.K8sIpam;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubernetes IPAM codec used for serializing and de-serializing JSON string.
 */
public final class K8sIpamCodec extends JsonCodec<K8sIpam> {

    private final Logger log = getLogger(getClass());

    private static final String IPAM_ID = "ipamId";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String NETWORK_ID = "networkId";

    private static final String MISSING_MESSAGE = " is required in K8sIPAM";

    @Override
    public ObjectNode encode(K8sIpam ipam, CodecContext context) {
        checkNotNull(ipam, "Kubernetes IPAM cannot be null");

        return context.mapper().createObjectNode()
                .put(IPAM_ID, ipam.ipamId())
                .put(IP_ADDRESS, ipam.ipAddress().toString())
                .put(NETWORK_ID, ipam.networkId());
    }

    @Override
    public K8sIpam decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String ipamId = nullIsIllegal(json.get(IPAM_ID).asText(),
                IPAM_ID + MISSING_MESSAGE);
        String ipAddress = nullIsIllegal(json.get(IP_ADDRESS).asText(),
                IP_ADDRESS + MISSING_MESSAGE);
        String networkId = nullIsIllegal(json.get(NETWORK_ID).asText(),
                NETWORK_ID + MISSING_MESSAGE);

        return new DefaultK8sIpam(ipamId, IpAddress.valueOf(ipAddress), networkId);
    }
}
