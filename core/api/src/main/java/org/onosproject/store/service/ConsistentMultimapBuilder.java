/*
 * Copyright 2016 Open Networking Foundation
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

package org.onosproject.store.service;

import org.onosproject.store.primitives.DistributedPrimitiveBuilder;

/**
 * A builder class for {@code AsyncConsistentMultimap}.
 */
public abstract class ConsistentMultimapBuilder<K, V>
    extends ConsistentMultimapOptions<ConsistentMultimapBuilder<K, V>, K, V>
    implements DistributedPrimitiveBuilder<ConsistentMultimap<K, V>> {

    /**
     * Builds the distributed multimap based on the configuration options
     * supplied to this builder.
     *
     * @return new distributed multimap
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    public abstract AsyncConsistentMultimap<K, V> buildMultimap();
}
