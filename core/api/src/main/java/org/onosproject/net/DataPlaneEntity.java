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

package org.onosproject.net;

import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.group.Group;

import java.util.Objects;

/**
 * Generic abstraction to hold dataplane entities.
 */
public class DataPlaneEntity {
    private FlowEntry flowEntry;
    private Group groupEntry;
    private Type type;

    /**
     * Types of entity.
     */
    public enum Type {
        /**
         * Flow rule entity.
         */
        FLOWRULE,

        /**
         * Group entity.
         */
        GROUP
    }

    /**
     * Creates a dataplane entity from a flow entry.
     *
     * @param flow the inner flow entry
     */
    public DataPlaneEntity(FlowEntry flow) {
        flowEntry = flow;
        type = Type.FLOWRULE;
    }

    /**
     * Creates a dataplane entity from a group entry.
     *
     * @param group the inner group entry
     */
    public DataPlaneEntity(Group group) {
        groupEntry = group;
        type = Type.GROUP;
    }

    /**
     * Returns the flow entry.
     *
     * @return the flow entry
     */
    public FlowEntry getFlowEntry() {
        return flowEntry;
    }

    /**
     * Returns the group entry.
     *
     * @return the group entry
     */
    public Group getGroupEntry() {
        return groupEntry;
    }

    /**
     * Returns the type of the entry.
     *
     * @return the type
     */
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return type == Type.FLOWRULE ? flowEntry.hashCode() : groupEntry.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DataPlaneEntity) {
            DataPlaneEntity that = (DataPlaneEntity) obj;
            if (this.type == that.type) {
                return Objects.equals(flowEntry, that.flowEntry) &&
                        Objects.equals(groupEntry, that.groupEntry);
            }
        }
        return false;
    }

    @Override
    public String toString() {
        Object entity = type == Type.FLOWRULE ? flowEntry : groupEntry;
        return "DataPlaneEntity{" +
                "entity=" + entity +
                '}';
    }
}
