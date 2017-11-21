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

package org.onosproject.drivers.p4runtime.mirror;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.pi.runtime.PiActionGroup;
import org.onosproject.net.pi.runtime.PiActionGroupHandle;
import org.onosproject.store.serializers.KryoNamespaces;

/**
 * Distributed implementation of a P4Runtime group mirror.
 */
@Component(immediate = true)
@Service
public final class DistributedP4RuntimeGroupMirror
        extends AbstractDistributedP4RuntimeMirror
                        <PiActionGroupHandle, PiActionGroup>
        implements P4RuntimeGroupMirror {

    private static final String DIST_MAP_NAME = "onos-p4runtime-group-mirror";

    @Override
    String mapName() {
        return DIST_MAP_NAME;
    }

    @Override
    KryoNamespace storeSerializer() {
        return KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(TimedEntry.class)
                .build();
    }
}
