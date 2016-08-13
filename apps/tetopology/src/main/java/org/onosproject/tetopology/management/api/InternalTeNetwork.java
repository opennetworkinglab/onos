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
package org.onosproject.tetopology.management.api;


import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Internal TE Network implementation.
 */
public class InternalTeNetwork extends DefaultNetwork {
    private TeTopologyType teTopologyType;

    /**
     * Constructor with all fields.
     *
     * @param teTopologyType TE topology type
     * @param network network object
     */
    public InternalTeNetwork(TeTopologyType teTopologyType, Network network) {
        super(network);
        this.teTopologyType = teTopologyType;
    }

    /**
     * Returns the TE topoology type.
     *
     * @return TE topology type
     */
    public TeTopologyType getTeTopologyType() {
        return this.teTopologyType;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), teTopologyType);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof InternalTeNetwork) {

            if (!super.equals(object)) {
                return false;
            }

            InternalTeNetwork that = (InternalTeNetwork) object;
            return Objects.equal(this.teTopologyType, that.teTopologyType);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("teTopologyType", teTopologyType)
                .add("DefaultNetwork", super.toString())
                .toString();
    }

}
