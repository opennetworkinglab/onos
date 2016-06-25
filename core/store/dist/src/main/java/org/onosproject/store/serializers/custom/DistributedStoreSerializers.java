/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.store.serializers.custom;

import org.onosproject.store.impl.MastershipBasedTimestamp;
import org.onosproject.store.impl.Timestamped;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onlab.util.KryoNamespace;

public final class DistributedStoreSerializers {


    public static final int STORE_CUSTOM_BEGIN = KryoNamespaces.BEGIN_USER_CUSTOM_ID + 10;

    /**
     * KryoNamespace which can serialize ON.lab misc classes.
     */
    public static final KryoNamespace STORE_COMMON = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
            .register(Timestamped.class)
            .register(new MastershipBasedTimestampSerializer(), MastershipBasedTimestamp.class)
            .register(WallClockTimestamp.class)
            .build();

    // avoid instantiation
    private DistributedStoreSerializers() {}
}
