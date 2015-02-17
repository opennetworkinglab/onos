/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.store.consistent.impl;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.hash.Hashing;

/**
 * Partitioner for mapping table entries to individual database partitions.
 * <p>
 * By default a md5 hash of the hash key (key or table name) is used to pick a
 * partition.
 */
public abstract class DatabasePartitioner implements Partitioner<String> {
    // Database partitions sorted by their partition name.
    protected final Database[] sortedPartitions;

    public DatabasePartitioner(Map<String, Database> partitionMap) {
        checkState(partitionMap != null && !partitionMap.isEmpty(), "Partition map cannot be null or empty");
        sortedPartitions = ImmutableSortedMap.<String, Database>copyOf(partitionMap).values().toArray(new Database[]{});
    }

    protected int hash(String key) {
        return Math.abs(Hashing.md5().newHasher().putBytes(key.getBytes(Charsets.UTF_8)).hash().asInt());
    }

}
