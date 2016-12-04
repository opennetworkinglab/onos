/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.node;

import java.util.BitSet;

import org.onosproject.tetopology.management.api.TeStatus;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of common node attributes.
 */
public class CommonNodeData {
    private final String name;
    private final TeStatus adminStatus;
    private final TeStatus opStatus;
    private final BitSet flags;

    /**
     * Creates a common node data instance.
     *
     * @param name        the TE node name
     * @param adminStatus the admin status
     * @param opStatus    the operational status
     * @param flags       the node flags
     */
    public CommonNodeData(String name, TeStatus adminStatus,
                          TeStatus opStatus, BitSet flags) {
        this.name = name;
        this.adminStatus = adminStatus;
        this.opStatus = opStatus;
        this.flags = flags;
    }

    /**
     * Creates a common node data instance based on a given TE node.
     *
     * @param node the given TE node
     */
    public CommonNodeData(TeNode node) {
        this.name = node.name();
        this.adminStatus = node.adminStatus();
        this.opStatus = node.opStatus();
        this.flags = node.flags();
    }

    /**
     * Returns the TE node name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the administrative status.
     *
     * @return the admin status
     */
    public TeStatus adminStatus() {
        return adminStatus;
    }

    /**
     * Returns the operational status.
     *
     * @return the operational status
     */
    public TeStatus opStatus() {
        return opStatus;
    }

    /**
     * Returns the flags in the common node data.
     *
     * @return the flags
     */
    public BitSet flags() {
        return flags;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, adminStatus, opStatus, flags);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof CommonNodeData) {
            CommonNodeData that = (CommonNodeData) object;
            return Objects.equal(name, that.name) &&
                    Objects.equal(adminStatus, that.adminStatus) &&
                    Objects.equal(opStatus, that.opStatus) &&
                    Objects.equal(flags, that.flags);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("adminStatus", adminStatus)
                .add("opStatus", opStatus)
                .add("flags", flags)
                .toString();
    }
}
