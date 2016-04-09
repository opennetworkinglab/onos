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
package org.onosproject.store.service;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Contains information about a database partition.
 */
public class PartitionInfo {
    private final String name;
    private final long term;
    private final List<String> members;
    private final String leader;

    /**
     * Class constructor.
     *
     * @param name partition name
     * @param term term number
     * @param members partition members
     * @param leader leader name
     */
    public PartitionInfo(String name, long term, List<String> members, String leader) {
        this.name = name;
        this.term = term;
        this.members = ImmutableList.copyOf(members);
        this.leader = leader;
    }

    /**
     * Returns the name of the partition.
     *
     * @return partition name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the term number.
     *
     * @return term number
     */
    public long term() {
        return term;
    }

    /**
     * Returns the list of partition members.
     *
     * @return partition members
     */
    public List<String> members() {
        return members;
    }

    /**
     * Returns the partition leader.
     *
     * @return partition leader
     */
    public String leader() {
        return leader;
    }
}
