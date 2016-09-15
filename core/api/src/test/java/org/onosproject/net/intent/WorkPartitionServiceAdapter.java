/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.intent;

import java.util.function.Function;

import org.onosproject.cluster.NodeId;

/**
 * Testing adapter for the WorkPartitionService.
 */
public class WorkPartitionServiceAdapter implements WorkPartitionService {
    @Override
    public <K> boolean isMine(K id, Function<K, Long> hasher) {
        return true;
    }

    @Override
    public <K> NodeId getLeader(K id, Function<K, Long> hasher) {
        return null;
    }

    @Override
    public void addListener(WorkPartitionEventListener listener) {

    }

    @Override
    public void removeListener(WorkPartitionEventListener listener) {

    }
}
