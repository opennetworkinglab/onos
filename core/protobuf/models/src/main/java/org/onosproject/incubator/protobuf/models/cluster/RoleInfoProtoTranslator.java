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
package org.onosproject.incubator.protobuf.models.cluster;

import com.google.common.collect.Lists;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.grpc.cluster.models.RoleInfoProtoOuterClass;

import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC RoleInfoProto message to equivalent ONOS RoleInfo conversion related utilities.
 */
public final class RoleInfoProtoTranslator {

    /**
     * Translates gRPC RoleInfo to {@link RoleInfo}.
     *
     * @param roleInfo gRPC message
     * @return {@link RoleInfo}
     */
    public static RoleInfo translate(RoleInfoProtoOuterClass.RoleInfoProto roleInfo) {
        NodeId master = NodeIdProtoTranslator.translate(roleInfo.getMaster());

        List<NodeId> backups = Lists.newArrayList();
        backups = roleInfo.getBackupsList().stream().map(r ->
                NodeIdProtoTranslator.translate(r)).collect(Collectors.toList());
        return new RoleInfo(master, backups);
    }

    /**
     * Translates {@link RoleInfo} to gRPC RoleInfo message.
     *
     * @param roleInfo {@link RoleInfo}
     * @return gRPC RoleInfo message
     */
    public static RoleInfoProtoOuterClass.RoleInfoProto translate(RoleInfo roleInfo) {

        if (roleInfo != null) {
            RoleInfoProtoOuterClass.RoleInfoProto.Builder builder =
                    RoleInfoProtoOuterClass.RoleInfoProto.newBuilder();
            builder.setMaster(NodeIdProtoTranslator.translate(roleInfo.master()));
            roleInfo.backups().forEach(b -> builder.addBackups(NodeIdProtoTranslator.translate(b)));
            return builder.build();
        }

        return RoleInfoProtoOuterClass.RoleInfoProto.getDefaultInstance();
    }

    // Utility class not intended for instantiation.
    private RoleInfoProtoTranslator() {}
}
