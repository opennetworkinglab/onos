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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 *  Represents node's switching limitations.
 */
public class ConnectivityMatrix {
    private final long id;
    private TerminationPointKey from;
    private TerminationPointKey to;
    private boolean isAllowed;

    /**
     * Creates an instance of ConnectivityMatrix.
     *
     * @param id connectivity matrix identifier
     * @param from from termination point key
     * @param to to termination point key
     * @param isAllowed indicate whether this connectivity matrix is useable
     */
    public ConnectivityMatrix(long id, TerminationPointKey from,
                            TerminationPointKey to, boolean isAllowed) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.isAllowed = isAllowed;
    }

    /**
     * Constructor with id only.
     *
     * @param id connectivity matrix id
     */
    public ConnectivityMatrix(long id) {
        this.id = id;
    }

    /**
     * Returns the id.
     *
     * @return connectivity matrix id
     */
    public long id() {
        return id;
    }

    /**
     * Returns the "from" of a connectivity matrix.
     *
     * @return the "from" of a connectivity matrix
     */
    public TerminationPointKey from() {
        return from;
    }

    /**
     * Returns the "to" of a connectivity matrix.
     *
     * @return the "to" of a connectivity matrix
     */
    public TerminationPointKey to() {
        return to;
    }

    /**
     * Returns true if the connectivity matrix is allowed; false otherwise.
     *
     * @return true if the connectivity matrix is allowed; false otherwise
     */
    public boolean isAllowed() {
        return isAllowed;
    }

    /**
     * Sets the from termination point.
     *
     * @param from the from to set
     */
    public void setFrom(TerminationPointKey from) {
        this.from = from;
    }

    /**
     * Sets the to termination point.
     *
     * @param to the to to set
     */
    public void setTo(TerminationPointKey to) {
        this.to = to;
    }

    /**
     * Sets isAllowed.
     *
     * @param isAllowed the isAllowed to set
     */
    public void setIsAllowed(boolean isAllowed) {
        this.isAllowed = isAllowed;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, from, to, isAllowed);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof ConnectivityMatrix) {
            ConnectivityMatrix that = (ConnectivityMatrix) object;
            return Objects.equal(this.id, that.id) &&
                    Objects.equal(this.from, that.from) &&
                    Objects.equal(this.to, that.to) &&
                    Objects.equal(this.isAllowed, that.isAllowed);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("from", from)
                .add("to", to)
                .add("isAllowed", isAllowed)
               .toString();
    }
}
