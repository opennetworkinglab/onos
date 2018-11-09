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

package org.onosproject.incubator.protobuf.models.net;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.grpc.net.models.HostProtoOuterClass.HostProto;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.Host;

import java.util.stream.Collectors;

/**
 * gRPC HostProto message to equivalent ONOS Host conversion related utilities.
 */
public final class HostProtoTranslator {


    /**
     * Translates {@link Host} to gRPC Host message.
     *
     * @param host {@link Host}
     * @return gRPC HostProto message
     */
    public static HostProto translate(Host host) {

        if (host != null) {
            return HostProto.newBuilder()
                    .setHostId(HostIdProtoTranslator.translate(host.id()))
                    .setConfigured(host.configured())
                    .setVlan(host.vlan().toShort())
                    .addAllIpAddresses(host.ipAddresses().stream()
                                               .map(IpAddress::toString)
                                               .collect(Collectors.toList()))
                    .setLocation(HostLocationProtoTranslator.translate(host.location()))
                    .build();
        }

        return HostProto.getDefaultInstance();
    }

    /**
     * Translates gRPC Host message to {@link Host}.
     *
     * @param host gRPC message
     * @return {@link Host}
     */
    public static Host translate(HostProto host) {
        if (host.equals(HostProto.getDefaultInstance())) {
            return null;
        }

        return new DefaultHost(ProviderIdProtoTranslator.translate(host.getProviderId()),
                               HostIdProtoTranslator.translate(host.getHostId()),
                               MacAddress.valueOf(host.getHostId().getMac()),
                               VlanId.vlanId((short) host.getVlan()),
                               HostLocationProtoTranslator.translate(host.getLocation()),
                               host.getIpAddressesList().stream().map(x -> IpAddress.valueOf(x))
                                       .collect(Collectors.toSet()),
                               DefaultAnnotations.builder().putAll(host.getAnnotationsMap()).build());
    }

    // Utility class not intended for instantiation.
    private HostProtoTranslator() {}
}
