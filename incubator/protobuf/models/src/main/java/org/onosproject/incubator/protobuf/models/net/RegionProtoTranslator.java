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

import com.google.common.base.Strings;
import org.onosproject.incubator.protobuf.models.net.region.RegionEnumsProtoTranslator;
import org.onosproject.cluster.NodeId;
import org.onosproject.grpc.net.models.RegionProtoOuterClass;
import org.onosproject.net.Annotations;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * gRPC Region message to @link org.onosproject.net.region.Region conversion related utilities for region service.
 */
public final class RegionProtoTranslator {

    /**
     * Translates gRPC RegionProto message to {@link org.onosproject.net.region.Region}.
     *
     * @param region gRPC message
     * @return {@link org.onosproject.net.region.Region}
     */
    public static Region translate(RegionProtoOuterClass.RegionProto region) {
        RegionId id = RegionId.regionId(region.getRegionId());
        Region.Type type = RegionEnumsProtoTranslator.translate(region.getType()).get();
        String name = Strings.nullToEmpty(region.getName());

        List<Set<NodeId>> masters = new ArrayList<>();

        region.getMastersList().forEach(s -> {
            Set<NodeId> nodeIdSet = new HashSet<NodeId>();
            s.getNodeIdList().forEach(n -> {
                nodeIdSet.add(new NodeId(n));
            });
            masters.add(nodeIdSet);
        });

        Annotations annots = AnnotationsTranslator.asAnnotations(region.getAnnotations());

        return new DefaultRegion(id, name, type, annots, masters);
    }

    /**
     * Translates {@link org.onosproject.net.region.Region} to gRPC RegionProto message.
     *
     * @param region {@link org.onosproject.net.region.Region}
     * @return gRPC RegionProto message
     */
    public static RegionProtoOuterClass.RegionProto translate(Region region) {
        return RegionProtoOuterClass.RegionProto.newBuilder()
                .setRegionId(region.id().toString())
                .setType(RegionEnumsProtoTranslator.translate(region.type()))
                .setName(region.name().isEmpty() ? null : region.name())
                .addAllMasters(region.masters()
                                       .stream()
                                       .map(s -> RegionProtoOuterClass.RegionProto.NodeIdSet
                                               .newBuilder()
                                               .addAllNodeId(s.stream().map(id ->
                                                       id.toString()).collect(Collectors.toList()))
                                               .build())
                                       .collect(Collectors.toList()))
                .build();
    }

    // Utility class not intended for instantiation.
    private RegionProtoTranslator() {}

}

