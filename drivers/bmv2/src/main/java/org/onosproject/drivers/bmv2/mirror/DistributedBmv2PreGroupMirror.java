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
 */

package org.onosproject.drivers.bmv2.mirror;

import org.onlab.util.KryoNamespace;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2EntityType;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreGroup;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreGroupHandle;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreNode;
import org.onosproject.drivers.bmv2.api.runtime.Bmv2PreNodes;
import org.onosproject.store.serializers.KryoNamespaces;
import org.osgi.service.component.annotations.Component;


/**
 * Distributed implementation of a BMv2 PRE group mirror.
 * We need this mirror to preserve BMv2-specific identifiers of group
 * and nodes for further operations on them after creation.
 */
@Component(immediate = true, service = Bmv2PreGroupMirror.class)
public class DistributedBmv2PreGroupMirror
        extends AbstractDistributedBmv2Mirror<Bmv2PreGroupHandle, Bmv2PreGroup>
        implements Bmv2PreGroupMirror {

    private static final String DIST_MAP_NAME = "onos-bmv2-pre-group-mirror";

    @Override
    String mapName() {
        return DIST_MAP_NAME;
    }

    @Override
    KryoNamespace storeSerializer() {
        return KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(Bmv2EntityType.class)
                .register(Bmv2PreNode.class)
                .register(Bmv2PreNodes.class)
                .register(Bmv2PreGroup.class)
                .build();
    }
}
